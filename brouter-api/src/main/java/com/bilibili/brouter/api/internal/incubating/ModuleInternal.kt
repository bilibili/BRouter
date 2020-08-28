package com.bilibili.brouter.api.internal.incubating

import com.bilibili.brouter.api.Module
import com.bilibili.brouter.api.internal.AttributeContainerInternal
import com.bilibili.brouter.api.internal.module.ModuleData

interface ModuleInternal : Module {

    val data: ModuleData

    override val tasks: TaskContainerInternal

    fun syncStatus(): ModuleInternal

    override val attributes: AttributeContainerInternal
}