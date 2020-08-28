package com.bilibili.brouter.core.internal.service

import android.util.LruCache
import com.bilibili.brouter.api.ModularProvider
import com.bilibili.brouter.api.ServiceCentral
import com.bilibili.brouter.api.ServiceInjector
import com.bilibili.brouter.api.ServicesProvider
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.core.internal.table.ServiceTable
import com.bilibili.brouter.core.internal.table.TaskOutputModularProvider
import com.bilibili.brouter.core.internal.task.Buildable

@Suppress("UNCHECKED_CAST")
internal class ServiceManager :
    ServiceCentralInternal {

    private lateinit var serviceTable: ServiceTable

    private val injectors = object : LruCache<String, ServiceInjector<Any>>(128) {
        override fun create(key: String): ServiceInjector<Any>? {
            return try {
                Class.forName("$key\$\$BRInjector")
                    .asSubclass(ServiceInjector::class.java)
                    .newInstance() as ServiceInjector<Any>
            } catch (e: ClassNotFoundException) {
                null
            }
        }
    }

    override fun attachTable(serviceTable: ServiceTable) {
        this.serviceTable = serviceTable
    }

    override fun <T> getServices(clazz: Class<T>): ServicesProvider<T> {
        return serviceTable.getServices(clazz)
    }

    override fun <T : Any> inject(clazz: Class<T>, o: T) {
        var targetClass: Class<in T> = clazz
        while (targetClass !== Any::class.java) {
            injectors.get(targetClass.name)?.let {
                if (targetClass !== clazz) {
                    // put injector for subclass
                    injectors.put(clazz.name, it)
                }
                it.inject(o, this)
                return
            }
            targetClass = targetClass.superclass ?: break
        }
        // no injector, put empty injector
        injectors.put(clazz.name, ObjectInjector)
    }

    override fun getDependencies(clazz: Class<*>, name: String): Buildable? {
        return serviceTable.getServiceProvider(clazz, name)
    }

    override fun <T> fill(clazz: Class<T>, name: String, module: ModuleInternal, t: T) {
        val modularProvider = serviceTable.getServiceProvider(clazz, name)
        if (modularProvider != null) {
            require(modularProvider.module.name == module.name) {
                "Illegal module $module that provides service $clazz named '$name'."
            }
            if (modularProvider is TaskOutputModularProvider<*>) {
                modularProvider as TaskOutputModularProvider<T>
                modularProvider.setResult(t)
            } else {
                error("Service $clazz named '${name}' is not producible by Task.")
            }
        } else {
            serviceTable.registerValueService(clazz, name, module, t)
        }
    }

    override fun <T> getService(clazz: Class<T>, name: String): T? {
        return getProviderWildcard(clazz, name)?.get()
    }

    override fun <T> getProvider(clazz: Class<T>, name: String): ModularProvider<T>? {
        return getProviderWildcard(clazz, name) as ModularProvider<T>?
    }

    override fun <T> getProviderWildcard(clazz: Class<T>, name: String): ModularProvider<out T>? {
        return serviceTable.getServiceProvider(clazz, name)
    }

    companion object ObjectInjector : ServiceInjector<Any> {
        override fun inject(o: Any, services: ServiceCentral) {
        }
    }
}