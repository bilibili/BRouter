package com.bilibili.brouter.core.internal.routes

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.IRoutes
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.api.internal.incubating.RouteInfoInternal
import com.bilibili.brouter.core.internal.attribute.HasAttributesContainer
import com.bilibili.brouter.core.internal.table.RouteTable
import java.util.*
import kotlin.collections.ArrayList

internal class RouteManager() :
    RouteCentralInternal {

    private lateinit var routeTable: RouteTable
    private lateinit var defaultScheme: String

    override fun attachTable(
        routeTable: RouteTable,
        defaultScheme: String
    ) {
        this.routeTable = routeTable
        this.defaultScheme = defaultScheme
        // will override the routes that allow overrides
        routeTable.defaultFlag = HasAttributesContainer.FLAG_OVERRIDE_EXISTS
    }

    override fun findRoute(request: RouteRequest, type: String): RouteResponse {
        if (request.targetUri.isOpaque) {
            return RouteResponse(RouteResponse.Code.UNSUPPORTED, request)
        }
        val segments = request.targetUri.let { targetUri ->
            val scheme = targetUri.scheme ?: defaultScheme
            val host = targetUri.host
            val paths = targetUri.pathSegments
            val segments = ArrayList<String>(paths.size + if (host != null) 2 else 1)

            segments += scheme
            if (host != null) {
                segments += host
            }
            segments += paths
            segments
        }
        return routeTable.findRoute(segments, type, request.attributes)?.let { results ->
            when {
                results.size == 1 -> {
                    results[0].let {
                        (it.routes.module as ModuleInternal).syncStatus()
                        RouteResponse(
                            RouteResponse.Code.OK,
                            request,
                            routeInfo = RealRouteInfo(
                                it.routeRule,
                                it.routes,
                                Collections.unmodifiableMap(it.capture(segments)),
                                null
                            )
                        )
                    }
                }
                results.isNotEmpty() -> {
                    RouteResponse(
                        RouteResponse.Code.ERROR,
                        request,
                        "For ${request.attributes},\n" +
                                "cannot choose between the following routes: \n${results.joinToString(
                                    separator = "\n"
                                )}"
                    )
                }
                else -> {
                    null
                }
            }
        } ?: RouteResponse(
            RouteResponse.Code.NOT_FOUND,
            request,
            "Can't found routes for type '${type}'."
        )
    }

    override fun dynamicRegisterRoutes(routes: IRoutes) {
        routeTable.registerRoutes(routes)
    }
}

private data class RealRouteInfo(
    override val routeRule: String,
    override val routes: IRoutes,
    override val captures: Map<String, String>,
    val replacedClass: Class<*>? = null
) : RouteInfoInternal {

    override val routeName: String
        get() = routes.routeName
    override val routeType: String
        get() = routes.routeType
    override val target: Class<*>
        get() = replacedClass ?: routes.clazz
    override val interceptors: Array<out Class<out RouteInterceptor>>
        get() = routes.interceptors
    override val module: Module
        get() = routes.module
    override val launcher: Class<out Launcher>
        get() = routes.launcher

    override fun withTarget(clazz: Class<*>): RouteInfo =
        RealRouteInfo(
            routeRule,
            routes,
            captures,
            clazz
        )

    override val attributes: AttributeContainer
        get() = routes.attributes

    override fun toString(): String {
        return "RouteInfo(routes=$routes, captures=$captures, replacedClass=$replacedClass)"
    }
}