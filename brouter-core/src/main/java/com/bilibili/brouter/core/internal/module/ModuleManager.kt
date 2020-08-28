package com.bilibili.brouter.core.internal.module

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.bilibili.brouter.api.Module
import com.bilibili.brouter.api.ModuleStatus
import com.bilibili.brouter.api.internal.module.ModuleContainer
import com.bilibili.brouter.common.util.matcher.RawSegmentsParser
import com.bilibili.brouter.core.internal.attribute.DefaultAttributeMatcher
import com.bilibili.brouter.core.internal.config.InternalGlobalConfiguration
import com.bilibili.brouter.core.internal.generated.BuiltInModules
import com.bilibili.brouter.core.internal.routes.RouteCentralInternal
import com.bilibili.brouter.core.internal.routes.RouteCapture
import com.bilibili.brouter.core.internal.routes.RouteManager
import com.bilibili.brouter.core.internal.routes.SingleRouteRef
import com.bilibili.brouter.core.internal.service.ServiceCentralInternal
import com.bilibili.brouter.core.internal.service.ServiceManager
import com.bilibili.brouter.core.internal.table.*
import com.bilibili.brouter.core.internal.task.DefaultTaskExecutor
import com.bilibili.brouter.core.internal.task.TaskCentral
import com.bilibili.brouter.core.internal.task.TaskManager
import com.bilibili.brouter.core.internal.util.Initializable
import com.bilibili.brouter.model.StubModuleMeta
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class ModuleManager : Initializable(),
    ModuleCentralInternal {

    private val overrides: MutableList<Class<out ModuleContainer>> = arrayListOf()
    private val stubs: MutableMap<String, StubModuleMeta> = hashMapOf()
    private var globalStatus = ModuleStatus.INITIALIZE

    override lateinit var config: InternalGlobalConfiguration
    override lateinit var serviceCentral: ServiceCentralInternal
    override lateinit var taskCentral: TaskCentral

    override lateinit var routeCentral: RouteCentralInternal

    private val lock = ReentrantReadWriteLock()
    private lateinit var modules: MutableMap<String, ModuleImpl>
    private lateinit var table: Table

    fun init(config: InternalGlobalConfiguration) {

        val serviceCentral = ServiceManager()
        val taskCentral = TaskManager(
            this,
            DefaultTaskExecutor(
                config.taskExecutionListener,
                config.taskComparator,
                config.executor,
                MainExecutor,
                Runtime.getRuntime().availableProcessors()
            )
        )
        init(
            config,
            serviceCentral,
            taskCentral,
            RouteManager()
        )
    }

    internal fun init(
        config: InternalGlobalConfiguration,
        serviceCentral: ServiceCentralInternal,
        taskCentral: TaskCentral,
        routeCentral: RouteCentralInternal
    ) {
        lock.writeLock().withLock {
            requireNonInitialized()
            this.config = config
            this.serviceCentral = serviceCentral
            this.taskCentral = taskCentral
            this.routeCentral = routeCentral

            val m = selectModules()
            modules = m

            // perform register
            config.logger.d {
                "perform Register"
            }

            val set = Collections.synchronizedSet(hashSetOf<Table>())
            val matcher = DefaultAttributeMatcher<SingleRouteRef>(config.attributeSchema.asSelector)
            val parser = RawSegmentsParser(config.defaultScheme)
            RouteCapture.parser = parser
            val local = LocalTable(this, set, matcher, parser)

            val executor = config.executor

            val futures = arrayListOf<Future<*>>()

            for ((name, stub) in stubs) {
                if (!m.containsKey(name)) {
                    futures.add(executor.submit {
                        stub.register(local.get()!!)
                    })
                }
            }
            stubs.clear()
            overrides.clear()

            m.forEach {
                futures.add(executor.submit {
                    it.value.performRegister(local.get()!!)
                })
            }

            futures.forEach {
                if (!it.isDone) {
                    it.get()
                }
            }

            // merge table
            config.logger.d {
                "merge table"
            }


            table =
                if (set.isEmpty()) {
                    Table(
                        ServiceTable(this),
                        RouteTable(matcher, parser)
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && executor is ForkJoinPool) {
                    executor.invoke(ForkJoinMergeTable(set.toList()))
                } else {
                    executor.submit(DivideAndConquerMergeTable(executor, set.toList())).get()
                }

            // attach table
            config.logger.d {
                "attach table"
            }
            table.routeTable.markInitialized()
            table.serviceTable.markInitialized()
            serviceCentral.attachTable(table.serviceTable)
            routeCentral.attachTable(table.routeTable, config.defaultScheme)

            // resolve create in lifecycle
            moveActiveModulesTo(ModuleStatus.CREATED)

            markInitialized()
        }
    }

    private fun moveActiveModulesTo(targetStatus: ModuleStatus) {
        lock.writeLock().withLock {
            config.logger.d {
                "Move active modules to $targetStatus"
            }
            this.globalStatus = targetStatus
            modules.values.forEach {
                if (it.isActive) {
                    it.moveStatusTo(targetStatus)
                }
            }
        }
    }

    private fun selectModules(): MutableMap<String, ModuleImpl> {

        val moduleList = BuiltInModules.modules()
        val modules = HashMap<String, ModuleContainer>(moduleList.size)

        for (module in moduleList) {
            if (modules.put(module.data.name, module) != null) {
                error("Duplicated module ${module.data.name}")
            }
        }
        overrides.forEach {
            it.createInstance()
                .also {
                    modules[it.data.name] = it
                }
        }
        return modules.mapValuesTo(hashMapOf()) {
            ModuleImpl().apply {
                bindOutModuleAndCentral(it.value, this@ModuleManager)
            }
        }
    }


    override fun install(vararg entrance: Class<out ModuleContainer>) {
        lock.writeLock().withLock {
            if (!initialized) {
                overrides += entrance
            } else {
                val targetStatus = globalStatus
                entrance.map {
                    it.createInstance()
                }.filter {
                    !modules.containsKey(it.data.name)
                }.map {
                    ModuleImpl().apply {
                        bindOutModuleAndCentral(it, this@ModuleManager)
                        modules[it.data.name] = this
                        performRegister(table)
                    }
                }.forEach {
                    if (it.isActive) {
                        it.moveStatusTo(targetStatus)
                    }
                }
            }
        }
    }

    override fun syncStatus(module: ModuleImpl): ModuleStatus {
        return globalStatus.also {
            module.moveStatusTo(it)
        }
    }

    @Synchronized
    fun installStub(stub: StubModuleMeta) {
        lock.writeLock().withLock {
            requireNonInitialized()
            stubs[stub.name] = stub
        }
    }

    private fun Class<out ModuleContainer>.createInstance(): ModuleContainer = try {
        getDeclaredConstructor().let {
            it.isAccessible = true
            it.newInstance()
        }
    } catch (e: Exception) {
        throw IllegalStateException(
            "No public empty constructor for Module $this",
            e
        )
    }

    override fun getModuleImpl(name: String): ModuleImpl? {
        return lock.readLock().withLock {
            modules[name]
        }
    }

    override fun module(name: String): Module {
        return lock.readLock().withLock {
            getModuleImpl(name)?.outerModule ?: error("Module $name not exists.")
        }
    }

    internal fun dispatchPostCreate() {
        moveActiveModulesTo(ModuleStatus.POST_CREATED)
    }
}


internal class LocalTable(
    private val central: ModuleCentralInternal,
    private val tables: MutableSet<Table>,
    private val matcher: DefaultAttributeMatcher<SingleRouteRef>,
    private val parser: RawSegmentsParser
) :
    ThreadLocal<Table>() {
    override fun initialValue(): Table = Table(
        ServiceTable(central),
        RouteTable(matcher, parser)
    ).apply {
        tables.add(this)
    }
}

internal object MainExecutor : Executor {
    private val handler = Handler(Looper.getMainLooper())
    override fun execute(command: Runnable) {
        handler.post(command)
    }
}