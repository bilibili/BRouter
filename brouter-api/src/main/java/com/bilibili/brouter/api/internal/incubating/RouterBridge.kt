package com.bilibili.brouter.api.internal.incubating

import com.bilibili.brouter.api.ModuleCentral
import com.bilibili.brouter.api.RouteCall
import com.bilibili.brouter.api.ServiceCentral

interface RouterBridge : RouteCall.Factory {
    val module: ModuleCentral
    val serviceCentral: ServiceCentral
}