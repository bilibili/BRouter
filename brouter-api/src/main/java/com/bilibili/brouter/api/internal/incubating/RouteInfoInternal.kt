package com.bilibili.brouter.api.internal.incubating

import com.bilibili.brouter.api.RouteInfo
import com.bilibili.brouter.api.internal.IRoutes

interface RouteInfoInternal : RouteInfo {
    val routes: IRoutes
}