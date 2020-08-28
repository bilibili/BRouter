package com.bilibili.brouter.core.internal.routes

import com.bilibili.brouter.api.GlobalConfiguration
import com.bilibili.brouter.core.internal.module.ModuleCentralInternal

internal data class RouteContext(
    val config: GlobalConfiguration,
    val call: RealRouteCall,
    val central: ModuleCentralInternal
)