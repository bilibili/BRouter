package com.bilibili.brouter.core.internal.routes

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.incubating.RouteCallInternal
import com.bilibili.brouter.core.internal.interceptors.BridgeInterceptor
import com.bilibili.brouter.core.internal.interceptors.FinalInterceptor
import com.bilibili.brouter.core.internal.interceptors.MultiRequestInterceptor
import com.bilibili.brouter.core.internal.interceptors.RetryAndFollowUpInterceptor
import com.bilibili.brouter.core.internal.module.ModuleCentralInternal

internal class RealRouteCall private constructor(
    override val request: RouteRequest,
    override val params: CallParams,
    private val central: ModuleCentralInternal
) :
    RouteCallInternal {

    override var executed = false
        private set

    override val listener: RouteListener = central.config.routerListenerFactory(this)

    override fun execute(): RouteResponse {
        synchronized(this) {
            if (executed) {
                error("Executed!")
            }
            executed = true
        }

        listener.onCallStart(this)

        val config = central.config

        val ctx = params.fragment?.activity ?: params.context ?: config.app
        val interceptors =
            ArrayList<RouteInterceptor>(config.preMatchInterceptors.size + config.postMatchInterceptors.size + 5)
                .apply {
                    this += config.preMatchInterceptors
                    this += MultiRequestInterceptor
                    this += RetryAndFollowUpInterceptor()
                    this += config.postMatchInterceptors
                    this += BridgeInterceptor
                    this += FinalInterceptor
                }
        return RealChain(
            interceptors,
            0,
            request,
            RouteContext(config, this, central),
            params.mode,
            ctx,
            params.fragment
        ).proceed(request).also {
            listener.onCallEnd(this, it)
        }
    }

    override fun clone(): RouteCallInternal =
        RealRouteCall(request, params, central)

    internal companion object {
        fun newCall(
            routeRequest: RouteRequest,
            params: CallParams,
            central: ModuleCentralInternal
        ): RouteCallInternal {
            return RealRouteCall(
                routeRequest,
                params,
                central
            )
        }
    }
}