package com.bilibili.brouter.api

import android.content.Context
import com.bilibili.brouter.api.internal.incubating.RouterBridge
import com.bilibili.brouter.stub.Fragment


object BRouter {

    private lateinit var bridge: RouterBridge
    fun setBridge(bridge: RouterBridge) {
        this.bridge = bridge
    }

    @JvmStatic
    val module: ModuleCentral
        get() = bridge.module

    @JvmStatic
    val serviceCentral: ServiceCentral
        get() = bridge.serviceCentral

    @JvmStatic
    fun newCall(request: RouteRequest, params: CallParams): RouteCall {
        return bridge.newCall(request, params)
    }

    @JvmStatic
    fun routeTo(request: RouteRequest, fragment: Fragment): RouteResponse =
        newCall(request, CallParams(fragment = fragment)).execute()

    @JvmOverloads
    @JvmStatic
    fun routeTo(request: RouteRequest, context: Context? = null): RouteResponse =
        newCall(request, CallParams(context = context)).execute()

    @JvmStatic
    fun inject(any: Any) {
        serviceCentral.inject(any.javaClass, any)
    }

    @JvmStatic
    fun module(name: String): Module = module.module(name)
}