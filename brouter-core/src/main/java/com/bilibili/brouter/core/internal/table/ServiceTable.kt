package com.bilibili.brouter.core.internal.table

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.ServiceRegistry
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.api.task.TaskDependency
import com.bilibili.brouter.core.internal.module.ModuleCentralInternal
import com.bilibili.brouter.core.internal.module.NoModule
import com.bilibili.brouter.core.internal.task.Buildable
import com.bilibili.brouter.core.internal.task.DefaultTaskDependency
import com.bilibili.brouter.core.internal.task.NoDependency
import com.bilibili.brouter.core.internal.task.TaskCentral
import com.bilibili.brouter.core.internal.util.Initializable
import com.bilibili.brouter.stub.ArrayMap
import javax.inject.Provider
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Suppress("UNCHECKED_CAST")
internal class ServiceTable(private val central: ModuleCentralInternal) : Initializable(),
    ServiceRegistry,
    Merger<ServiceTable> {

    private val servicesMap: MutableMap<Class<*>, ServicesProviderImpl<*>> = hashMapOf()
    /**
     * Optimize for name 'default'.
     */
    private val defaultServicesMap: MutableMap<Class<*>, ModularProviderInternal<*>> = hashMapOf()

    /**
     * No lock for register.
     */
    override fun <T> registerProviderService(
        clazz: Class<T>, name: String,
        provider: Provider<out T>,
        module: ModuleInternal,
        dependencies: Array<out Any>
    ) {
        registerService(
            clazz, name, DefaultModularProvider(
                provider,
                module,
                central.taskCentral,
                dependencies
            )
        )
    }

    override fun <T> registerTaskOutputService(
        clazz: Class<T>,
        name: String,
        module: ModuleInternal,
        taskName: String
    ) {
        registerService(
            clazz, name, TaskOutputModularProvider(
                module,
                central.taskCentral,
                taskName
            )
        )
    }

    internal fun <T> registerValueService(
        clazz: Class<T>,
        name: String,
        module: ModuleInternal,
        t: T
    ) {
        registerService(clazz, name, ValueModularProvider(t, module))
    }

    private fun <T> registerService(
        clazz: Class<T>,
        name: String,
        modularProvider: ModularProviderInternal<out T>
    ) {
        if (name != DEFAULT) {
            getServices(clazz).addStaticModularProvider(name, modularProvider)
        } else {
            synchronized(defaultServicesMap) {
                defaultServicesMap.put(clazz, modularProvider)
            }?.let {
                error("Found Duplicated service ${clazz.name} named 'default'")
            }
        }
    }

    internal fun <T> getServices(clazz: Class<T>): InternalServiceProvider<T> {
        return synchronized(servicesMap) {
            servicesMap.getOrPut(clazz) {
                ServicesProviderImpl(clazz)
            }
        } as InternalServiceProvider<T>
    }

    override fun deferred(): ServiceCentral = central.serviceCentral

    override fun merge(other: ServiceTable) {
        other.servicesMap.forEach {
            val c = servicesMap[it.key]
            if (c == null) {
                servicesMap[it.key] = it.value
            } else {
                c.merge(it.value)
            }
        }
        this.defaultServicesMap.putAll(other.defaultServicesMap)
    }

    fun <T> getServiceProvider(clazz: Class<T>, name: String): ModularProviderInternal<out T>? {
        return if (name == DEFAULT) {
            getDefaultServiceProvider(clazz)
        } else {
            getServices(clazz).getProvider(name)
        }
    }

    internal fun <T> getDefaultServiceProvider(clazz: Class<T>): ModularProviderInternal<out T>? =
        synchronized(defaultServicesMap) {
            defaultServicesMap[clazz] as ModularProviderInternal<out T>?
        }

    internal inner class ServicesProviderImpl<T>(
        val clazz: Class<T>,
        // Use array map for each services
        val map: MutableMap<String, ModularProviderInternal<out T>> = ArrayMap()
    ) :
        InternalServiceProvider<T>, Merger<ServicesProviderImpl<*>> {

        override fun addStaticModularProvider(
            name: String,
            provider: ModularProviderInternal<out T>
        ) {
            synchronized(map) {
                map.put(name, provider)
            }?.let {
                error("Found Duplicated service ${clazz.name} named '$name'")
            }
        }

        override fun get(name: String): T? {
            return getProvider(name)?.get()
        }

        override fun getProvider(name: String): ModularProviderInternal<out T>? {
            return if (name == DEFAULT) {
                getDefaultServiceProvider(clazz)
            } else {
                synchronized(map) {
                    map[name]
                }
            }
        }

        override val all: Map<String, T>
            get() {
                val ret = hashMapOf<String, T>()
                getDefaultServiceProvider(clazz)?.let {
                    ret[DEFAULT] = it.get()
                }
                synchronized(map) {
                    map.forEach { (name, provider) ->
                        ret[name] = provider.get()
                    }
                }
                return ret
            }

        override fun merge(other: ServicesProviderImpl<*>) {
            map.putAll(other.map as Map<out String, ModularProviderInternal<out T>>)
        }
    }
}

internal interface InternalServiceProvider<T> : ServicesProvider<T> {
    fun addStaticModularProvider(name: String, provider: ModularProviderInternal<out T>)

    override fun getProvider(name: String): ModularProviderInternal<out T>?
}

internal interface ModularProviderInternal<T> : ModularProvider<T>, Buildable

internal class NoModuleProvider<T>(private val provider: Provider<T>) : ModularProviderInternal<T> {
    override fun get(): T = provider.get()
    override val module: Module get() = NoModule
    override val dependencies: TaskDependency get() = NoDependency
}

internal abstract class AbstractModularProvider<T>(
    override val module: ModuleInternal,
    private val central: TaskCentral
) : ModularProviderInternal<T> {

    abstract override var dependencies: TaskDependency

    override fun get(): T {
        if (dependencies !is NoDependency) {
            synchronized(this) {
                if (dependencies !is NoDependency) {
                    central.stealing(dependencies)
                    dependencies = NoDependency
                }
            }
        }
        return doGet()
    }

    abstract fun doGet(): T
}

private class ValueModularProvider<T>(
    private val t: T,
    override val module: ModuleInternal
) : ModularProviderInternal<T> {
    override fun get(): T = t

    override val dependencies: TaskDependency
        get() = NoDependency
}

private class DefaultModularProvider<T>(
    private val provider: Provider<T>,
    module: ModuleInternal,
    central: TaskCentral,
    dependencies: Array<out Any>
) : AbstractModularProvider<T>(module, central) {

    override var dependencies: TaskDependency =
        if (dependencies.isNotEmpty()) {
            DefaultTaskDependency(module, central, dependencies.asList())
        } else {
            NoDependency
        }

    override fun doGet(): T = provider.get()
}

internal class TaskOutputModularProvider<T>(
    module: ModuleInternal,
    central: TaskCentral,
    taskName: String
) : AbstractModularProvider<T>(module, central) {

    override var dependencies: TaskDependency =
        DefaultTaskDependency(module, central, listOf(taskName))

    private var t: T? = null

    fun setResult(t: T) {
        check(this.t == null) {
            "Set result multiple times."
        }
        this.t = t
    }

    override fun doGet(): T {
        return t
            ?: throw IllegalStateException("${module.name} declare output but don't create it.")
    }
}