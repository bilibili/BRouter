package com.bilibili.brouter.api.internal.module

import com.bilibili.brouter.api.BootStrapMode
import com.bilibili.brouter.api.ModuleActivator
import com.bilibili.brouter.api.ServiceCentral
import com.bilibili.brouter.api.internal.ModuleWrapper
import com.bilibili.brouter.api.internal.Registry
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.api.task.TaskContainer
import com.bilibili.brouter.api.task.ThreadMode

class ModuleData(
    val name: String,
    val mode: BootStrapMode,
    val onCreate: ModuleTaskOptions?,
    val onPostCreate: ModuleTaskOptions?,
    val attributes: Array<out Pair<String, String>>
) {
    override fun toString(): String {
        return "Module(name='$name', mode=$mode, attributes=${attributes.contentToString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModuleData) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class ModuleTaskOptions(
    val name: String,
    val priority: Int,
    val threadMode: ThreadMode,
    vararg val dependencies: Any
)

abstract class ModuleContainer(override val data: ModuleData) : ModuleWrapper() {

    private lateinit var services: ServiceCentral
    val activator: ModuleActivator by lazy {
        createActivator(services)
    }

    fun attachImpl(base: ModuleInternal, services: ServiceCentral) {
        this.services = services
        attachBaseModule(base)
    }

    open fun createActivator(services: ServiceCentral): ModuleActivator = DefaultModuleActivator

    open fun onRegister(registry: Registry, tasks: TaskContainer) {
    }

    override fun toString(): String {
        return data.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModuleContainer) return false

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }


}

object DefaultModuleActivator : ModuleActivator()