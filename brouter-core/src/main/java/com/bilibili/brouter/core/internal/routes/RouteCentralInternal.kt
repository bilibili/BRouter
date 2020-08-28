package com.bilibili.brouter.core.internal.routes

import com.bilibili.brouter.api.RouteRequest
import com.bilibili.brouter.api.RouteResponse
import com.bilibili.brouter.api.internal.IRoutes
import com.bilibili.brouter.core.internal.table.RouteTable


internal interface RouteCentralInternal {
    fun attachTable(
        routeTable: RouteTable,
        defaultScheme: String
    )
    fun findRoute(request: RouteRequest, type: String): RouteResponse
    fun dynamicRegisterRoutes(routes: IRoutes)
}