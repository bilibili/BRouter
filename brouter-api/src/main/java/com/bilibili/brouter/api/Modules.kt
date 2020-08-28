package com.bilibili.brouter.api

import android.app.Application
import com.bilibili.brouter.api.internal.HasInternalProtocol
import com.bilibili.brouter.api.task.TaskContainer


@HasInternalProtocol
interface Module : HasAttributes {
    val name: String
    val mode: BootStrapMode
    val status: ModuleStatus
    val moduleInterceptors: List<RouteInterceptor>
    val tasks: TaskContainer

    fun moveStatusTo(status: ModuleStatus)
    /**
     * Await until module reach target status.
     */
    @Throws(InterruptedException::class)
    fun awaitAtLeast(status: ModuleStatus)

    fun whenStatusAtLeast(status: ModuleStatus, action: (Module) -> Unit)
}

interface Modular {
    val module: Module
}

enum class ModuleStatus {
    INITIALIZE,
    CREATED,
    POST_CREATED
}

annotation class ModuleOptions(
    val name: String,
    val mode: BootStrapMode = BootStrapMode.ON_INIT,
    val desc: String = "",
    /**
     * If true, routes / services / tasks without @BelongsTo belongs to this module.
     */
    val defaultModule: Boolean = true
)

/**
 * Indicates a module to which a service or route belongs.
 */
annotation class BelongsTo(val value: String)

enum class BootStrapMode {
    ON_DEMAND,
    ON_INIT
}

/**
 * 模块入口
 */
abstract class ModuleActivator {

    open fun onCreate(context: ModuleContext, modifier: ModuleConfigurationModifier) {
    }

    open fun onPostCreate(context: ModuleContext) {
    }
}

interface ModuleContext {
    val self: Module
    val app: Application
    fun findByName(name: String): Module?
}

interface ModuleConfigurationModifier {

    val moduleInterceptors: MutableList<RouteInterceptor>

    val attributeSchema: AttributeSchema

    fun addModuleInterceptors(interceptor: RouteInterceptor): ModuleConfigurationModifier

    fun addGlobalPreMatchInterceptor(interceptor: RouteInterceptor): ModuleConfigurationModifier

    fun addGlobalPostMatchInterceptor(interceptor: RouteInterceptor): ModuleConfigurationModifier

    fun attributeSchema(action: (AttributeSchema) -> Unit): ModuleConfigurationModifier
}

interface ModuleCentral {
    fun module(name: String): Module
}