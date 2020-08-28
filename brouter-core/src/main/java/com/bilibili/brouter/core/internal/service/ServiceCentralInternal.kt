package com.bilibili.brouter.core.internal.service

import com.bilibili.brouter.api.ServiceCentral
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.core.internal.table.ServiceTable
import com.bilibili.brouter.core.internal.task.Buildable

internal interface ServiceCentralInternal : ServiceCentral {

    fun <T> fill(clazz: Class<T>, name: String, module: ModuleInternal, t: T)

    fun getDependencies(clazz: Class<*>, name: String): Buildable?

    fun attachTable(serviceTable: ServiceTable)
}