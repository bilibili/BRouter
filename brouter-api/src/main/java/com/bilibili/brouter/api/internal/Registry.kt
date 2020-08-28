package com.bilibili.brouter.api.internal

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.api.internal.incubating.TaskContainerInternal
import javax.inject.Provider


interface Registry : RouteRegistry, ServiceRegistry

interface RouteRegistry {
    fun registerRoutes(routes: IRoutes)
}

interface ServiceRegistry {
    fun <T> registerProviderService(
        clazz: Class<T>, name: String,
        provider: Provider<out T>,
        module: ModuleInternal,
        dependencies: Array<out Any>
    )

    fun <T> registerTaskOutputService(
        clazz: Class<T>, name: String,
        module: ModuleInternal,
        taskName: String
    )

    fun deferred(): ServiceCentral
}

interface IRoutes : Modular, HasAttributes {
    val routeName: String
    val routeRules: Iterator<String>
    val routeType: String
    val interceptors: Array<out Class<out RouteInterceptor>>
    val launcher: Class<out Launcher>
    val clazz: Class<*>
}

abstract class ModuleWrapper() : ModuleInternal {
    open lateinit var base: ModuleInternal

    val innerModule: ModuleInternal
        get() {
            var m = base
            while (m is ModuleWrapper) {
                m = m.base
            }
            return m
        }

    override val name: String
        get() = data.name

    override val mode: BootStrapMode
        get() = data.mode

    override val attributes: AttributeContainerInternal
        get() = base.attributes

    override val status: ModuleStatus
        get() = base.status

    override val moduleInterceptors: List<RouteInterceptor>
        get() = base.moduleInterceptors

    override fun moveStatusTo(status: ModuleStatus) {
        base.moveStatusTo(status)
    }

    override fun awaitAtLeast(status: ModuleStatus) {
        base.awaitAtLeast(status)
    }

    override fun whenStatusAtLeast(status: ModuleStatus, action: (Module) -> Unit) {
        base.awaitAtLeast(status)
    }

    fun attachBaseModule(module: ModuleInternal) {
        this.base = module
    }

    override val tasks: TaskContainerInternal
        get() = base.tasks

    override fun syncStatus(): ModuleInternal {
        this.base.syncStatus()
        return this
    }
}