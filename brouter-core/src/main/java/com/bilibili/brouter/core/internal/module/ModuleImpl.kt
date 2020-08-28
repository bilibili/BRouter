package com.bilibili.brouter.core.internal.module

import android.app.Application
import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.AttributeContainerInternal
import com.bilibili.brouter.api.internal.ModuleWrapper
import com.bilibili.brouter.api.internal.Registry
import com.bilibili.brouter.api.internal.TaskLike
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.api.internal.incubating.TaskContainerInternal
import com.bilibili.brouter.api.internal.module.ModuleContainer
import com.bilibili.brouter.api.internal.module.ModuleData
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.core.internal.config.InternalGlobalConfiguration
import com.bilibili.brouter.core.internal.task.DefaultTaskContainer
import com.bilibili.brouter.core.internal.task.TaskInternal
import com.bilibili.brouter.core.internal.task.TaskStub
import com.bilibili.brouter.core.internal.util.Locked
import java.util.*

internal class ModuleImpl : ModuleInternal, ModuleContext {

    override val name: String
        get() = outerModule.name

    override val mode: BootStrapMode
        get() = outerModule.mode

    override val attributes: AttributeContainerInternal by lazy {
        attributesOf(outerModule.data.attributes.asList()) as AttributeContainerInternal
    }

    override var moduleInterceptors: List<RouteInterceptor> = emptyList()
        private set

    private lateinit var central: ModuleCentralInternal
    internal lateinit var outerModule: ModuleContainer
    private val lockedStatus = Locked(ModuleStatus::class.java, ModuleStatus.INITIALIZE)

    override val status get() = lockedStatus.value

    private val createTask: TaskLike?
        get() =
            outerModule.data.onCreate?.let {
                tasks[it.name]
            }
    private val postCreateTask: TaskLike?
        get() =
            outerModule.data.onPostCreate?.let {
                tasks[it.name]
            }
    internal var isActive = false

    override fun awaitAtLeast(status: ModuleStatus) {
        when (status) {
            ModuleStatus.CREATED -> stealingOrAwait(createTask, status)
            ModuleStatus.POST_CREATED -> stealingOrAwait(postCreateTask, status)
        }
    }

    private fun stealingOrAwait(task: TaskLike?, status: ModuleStatus) {
        if (task is Task) {
            task.awaitComplete()
        } else if (task !is TaskStub) {
            lockedStatus.awaitAtLeast(status)
        }
    }

    override fun whenStatusAtLeast(status: ModuleStatus, action: (Module) -> Unit) {
        lockedStatus.whenAtLeast(status) {
            action(outerModule)
        }
    }

    fun bindOutModuleAndCentral(wrapper: ModuleContainer, central: ModuleCentralInternal) {
        this.central = central
        this.outerModule = wrapper
        wrapper.attachImpl(this, central.serviceCentral)
        isActive = wrapper.data.mode == BootStrapMode.ON_INIT
    }

    fun performRegister(registry: Registry) {
        outerModule.onRegister(registry, tasks)
        registerCreate()
        registerPostCreate()
    }

    override fun moveStatusTo(status: ModuleStatus) {
        isActive = true
        lockedStatus.ifLessThanTarget(status) {
            when (status) {
                ModuleStatus.CREATED -> submitOrMove(createTask, status)
                ModuleStatus.POST_CREATED -> submitOrMove(postCreateTask, status)
            }
        }
    }

    private fun submitOrMove(task: TaskLike?, status: ModuleStatus) {
        if (task is Task) {
            task.submit()
        } else {
            lockedStatus.tryIncreaseTo(status)
        }
    }

    private fun registerCreate() {
        outerModule.data.onCreate?.let { onCreateOptions ->
            tasks.register(onCreateOptions.name) {
                it.priority(onCreateOptions.priority)
                    .threadMode(onCreateOptions.threadMode)
                    .dependsOn(onCreateOptions.dependencies)
                    .doLast {
                        val modifier = ModifierImpl(central.config)
                        outerModule.activator.onCreate(this, modifier)
                        central.config.mutablePreMatchInterceptors.addAll(modifier.preInterceptors)
                        central.config.mutablePostMatchInterceptors.addAll(modifier.postInterceptors)
                        this@ModuleImpl.moduleInterceptors =
                            Collections.unmodifiableList(modifier.moduleInterceptors.toTypedArray().asList())
                        lockedStatus.tryIncreaseTo(ModuleStatus.CREATED)
                    }
            }
        }
    }

    private fun registerPostCreate() {
        val data = outerModule.data
        data.onPostCreate?.let { onPostCreateOptions ->
            tasks.register(onPostCreateOptions.name) {
                it.priority(onPostCreateOptions.priority)
                    .threadMode(onPostCreateOptions.threadMode)
                    .dependsOn(onPostCreateOptions.dependencies)
                    .doLast {
                        outerModule.activator.onPostCreate(this)
                        lockedStatus.tryIncreaseTo(ModuleStatus.POST_CREATED)
                    }
            }
        }
    }

    override val data: ModuleData
        get() = outerModule.data

    override fun syncStatus(): ModuleInternal {
        if (status < ModuleStatus.CREATED) {
            val targetStatus = central.syncStatus(this)
            awaitAtLeast(targetStatus)
        }
        return this
    }

    override val self: ModuleInternal
        get() = this@ModuleImpl.outerModule
    override val app: Application
        get() = central.config.app
    override val tasks = LazyTaskContainer()

    override fun findByName(name: String): Module? {
        return central.getModuleImpl(name)?.outerModule
    }


    internal inner class LazyTaskContainer : TaskContainerInternal {

        private val task = lazy(LazyThreadSafetyMode.PUBLICATION) {
            DefaultTaskContainer(outerModule).apply {
                central = this@ModuleImpl.central.taskCentral
            }
        }
        val initialized: Boolean get() = task.isInitialized()

        override fun register(name: String) {
            task.value.register(name)
        }

        override fun register(name: String, configure: (Task.Builder) -> Unit) {
            task.value.register(name, configure)
        }

        override fun create(name: String): TaskInternal {
            return task.value.create(name)
        }

        override fun create(name: String, configure: (Task.Builder) -> Unit): TaskInternal {
            return task.value.create(name, configure)
        }

        override fun get(name: String): TaskLike {
            return task.value[name]
        }

        override fun find(name: String): TaskLike? {
            return task.value.find(name)
        }

        override fun markTombstone(name: String) {
            task.value.markTombstone(name)
        }

        override fun toString(): String {
            return task.value.toString()
        }
    }
}

internal object NoModule : ModuleWrapper() {

    override val status: ModuleStatus
        get() = ModuleStatus.POST_CREATED
    override val moduleInterceptors: List<RouteInterceptor>
        get() = emptyList()

    override fun syncStatus(): ModuleInternal {
        return this
    }

    override val data: ModuleData
        get() = ModuleData("Noop", BootStrapMode.ON_INIT, null, null, emptyArray())
    override val tasks: TaskContainerInternal
        get() = error("Don't support task on NoModule.")

    override val name: String
        get() = "Noop"
    override val mode: BootStrapMode
        get() = BootStrapMode.ON_INIT
    override val attributes: AttributeContainerInternal
        get() = attributesOf() as AttributeContainerInternal

    override fun moveStatusTo(status: ModuleStatus) {
    }

    override fun awaitAtLeast(status: ModuleStatus) {
    }

    override fun whenStatusAtLeast(status: ModuleStatus, action: (Module) -> Unit) {
        action(this)
    }
}


private class ModifierImpl(private val config: InternalGlobalConfiguration) :
    ModuleConfigurationModifier {
    override val moduleInterceptors: MutableList<RouteInterceptor> = arrayListOf()
    override val attributeSchema: AttributeSchema get() = config.attributeSchema

    internal val preInterceptors = arrayListOf<RouteInterceptor>()
    internal val postInterceptors = arrayListOf<RouteInterceptor>()

    override fun addModuleInterceptors(interceptor: RouteInterceptor): ModuleConfigurationModifier =
        this.apply {
            moduleInterceptors.add(interceptor)
        }

    override fun addGlobalPreMatchInterceptor(interceptor: RouteInterceptor): ModuleConfigurationModifier =
        this.apply {
            preInterceptors.add(interceptor)
        }

    override fun addGlobalPostMatchInterceptor(interceptor: RouteInterceptor): ModuleConfigurationModifier =
        this.apply {
            postInterceptors.add(interceptor)
        }

    override fun attributeSchema(action: (AttributeSchema) -> Unit): ModuleConfigurationModifier =
        this.apply {
            action(attributeSchema)
        }
}