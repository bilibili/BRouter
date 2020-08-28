package com.bilibili.brouter.api

import javax.inject.Provider
import kotlin.reflect.KClass


@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Services(
    vararg val value: KClass<*> = [],
    val desc: String = "",
    val dependencies: Array<String> = []
)

const val DEFAULT = "default"

interface ServicesProvider<out T> {

    val all: Map<String, T>

    operator fun get(name: String = DEFAULT): T?

    fun getProvider(name: String = DEFAULT): ModularProvider<out T>?
}

interface ModularProvider<T> : Provider<T>, Modular

interface ServiceInjector<T : Any> {
    fun inject(o: T, services: ServiceCentral)
}

interface ServiceCentral {
    fun <T> getService(clazz: Class<T>, name: String = DEFAULT): T?
    fun <T> getProviderWildcard(clazz: Class<T>, name: String = DEFAULT): ModularProvider<out T>?
    fun <T> getProvider(clazz: Class<T>, name: String = DEFAULT): ModularProvider<T>?
    fun <T> getServices(clazz: Class<T>): ServicesProvider<T>
    fun <T : Any> inject(clazz: Class<T>, o: T)
}