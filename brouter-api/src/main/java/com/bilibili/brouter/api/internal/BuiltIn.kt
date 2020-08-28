package com.bilibili.brouter.api.internal

import com.bilibili.brouter.api.AttributeContainer
import com.bilibili.brouter.api.Launcher
import com.bilibili.brouter.api.RouteInterceptor
import javax.inject.Provider

private val arrayProvider: Provider<Array<Class<*>>> = Provider {
    emptyArray<Class<*>>()
}

private val emptyAnyArray = emptyArray<Any>()

fun <T> emptyArrayProvider(): Provider<Array<Class<out T>>> {
    return arrayProvider as Provider<Array<Class<out T>>>
}

fun emptyAnyArray(): Array<Any> = emptyAnyArray

private val emptyAttributesArray = emptyArray<Pair<String, String>>()

fun emptyAttributesArray(): Array<out Pair<String, String>> {
    return emptyAttributesArray
}

private val stubProvider: Provider<Class<out Launcher>> = Provider {
    Launcher::class.java
}

fun stubLauncherProvider(): Provider<Class<out Launcher>> = stubProvider

fun <T> singletonProvider(provider: Provider<T>): Provider<T> = SingletonProvider(provider)

private class SingletonProvider<T>(private val provider: Provider<T>) : Provider<T> {
    private val lazySingleton = lazy {
        provider.get()
    }

    override fun get(): T = lazySingleton.value
}

fun <T> requireNonNull(t: T?, msg: String): T {
    if (t == null) {
        throw NullPointerException(msg)
    }
    return t
}

fun routesBean(
    name: String,
    routeArray: Array<String>,
    routeType: String,
    attributesArray: Array<out Pair<String, String>>,
    interceptorsProvider: Provider<Array<Class<out RouteInterceptor>>>,
    launcherProvider: Provider<Class<out Launcher>>,
    clazzProvider: Provider<Class<*>>,
    module: ModuleWrapper
): IRoutes {
    return RoutesBean(
        name,
        routeArray,
        routeType,
        attributesArray,
        interceptorsProvider,
        launcherProvider,
        clazzProvider,
        module
    )
}

private class RoutesBean(
    override val routeName: String,
    private val routeArray: Array<String>,
    override val routeType: String,
    private val attributesArray: Array<out Pair<String, String>>,
    val interceptorsProvider: Provider<Array<Class<out RouteInterceptor>>>,
    val launcherProvider: Provider<Class<out Launcher>>,
    val clazzProvider: Provider<Class<*>>,
    override val module: ModuleWrapper
) : IRoutes {

    override val routeRules: Iterator<String> = routeArray.iterator()
    override val interceptors: Array<Class<out RouteInterceptor>>
        get() = interceptorsProvider.get()
    override val launcher: Class<out Launcher>
        get() = launcherProvider.get()
    override val clazz: Class<*>
        get() = clazzProvider.get()

    override fun toString(): String {
        return "IRoutes(name=$routeName, routes=${routeArray.contentToString()}, routeType=$routeType, attributes=$attributes, interceptors=${interceptors.contentToString()}, launcher=$launcher, clazz=$clazz)"
    }

    override val attributes: AttributeContainer by lazy {
        if (attributesArray.isEmpty()) {
            module.attributes
        } else {
            module.attributes.asMutable.let {
                attributesArray.forEach { pair ->
                    it.attribute(pair.first, pair.second)
                }
                it.asImmutable(false)
            }
        }
    }
}