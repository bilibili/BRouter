package com.bilibili.brouter.core.internal.module

import com.bilibili.brouter.api.ModuleStatus
import com.bilibili.brouter.api.internal.module.ModuleContainer
import com.bilibili.brouter.core.internal.config.InternalGlobalConfiguration
import com.bilibili.brouter.api.ModuleCentral
import com.bilibili.brouter.core.internal.routes.RouteCentralInternal
import com.bilibili.brouter.core.internal.service.ServiceCentralInternal
import com.bilibili.brouter.core.internal.task.TaskCentral

internal interface ModuleCentralInternal :
    ModuleCentral {

    fun getModuleImpl(name: String): ModuleImpl?

    fun syncStatus(module: ModuleImpl): ModuleStatus

    val config: InternalGlobalConfiguration

    val serviceCentral: ServiceCentralInternal

    val routeCentral: RouteCentralInternal

    val taskCentral: TaskCentral

    fun install(vararg entrance: Class<out ModuleContainer>)
}