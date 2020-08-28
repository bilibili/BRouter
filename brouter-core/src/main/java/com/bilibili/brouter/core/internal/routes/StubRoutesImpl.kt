package com.bilibili.brouter.core.internal.routes

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.IRoutes
import com.bilibili.brouter.core.internal.module.NoModule
import com.bilibili.brouter.model.AttributeBean
import com.bilibili.brouter.model.StubRoutes

class StubRoutesImpl(
    override val routeName: String,
    private val routeArray: Array<String>,
    override val routeType: String,
    private val attributesList: List<AttributeBean>,
    val moduleName: String
) : IRoutes {
    override val routeRules: Iterator<String>
        get() = routeArray.iterator()
    override val interceptors: Array<Class<out RouteInterceptor>>
        get() = emptyArray()
    override val launcher: Class<out Launcher>
        get() = Launcher::class.java
    override val clazz: Class<*>
        get() = StubRoutes::class.java
    override val module: Module
        get() = NoModule


    override val attributes: AttributeContainer by lazy {
        attributesOf(attributesList.map {
            it.name to it.value
        })
    }

    override fun toString(): String {
        return "StubRoutesImpl(name='$routeName', routes=${routeArray.contentToString()}, routeType='$routeType', attributesList=$attributesList, moduleName='$moduleName')"
    }
}