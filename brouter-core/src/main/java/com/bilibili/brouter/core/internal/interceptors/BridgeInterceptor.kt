package com.bilibili.brouter.core.internal.interceptors

import com.bilibili.brouter.api.RouteInterceptor
import com.bilibili.brouter.api.RouteResponse
import com.bilibili.brouter.api.internal.incubating.ChainInternal
import com.bilibili.brouter.core.internal.routes.RealChain

object BridgeInterceptor : RouteInterceptor {

    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        val route = chain.route!!
        chain as RealChain

        val call = chain.call
        val request = chain.request

        val moduleInterceptors = route.module.moduleInterceptors
        val customInterceptors = route.interceptors
        if (moduleInterceptors.isEmpty() && customInterceptors.isEmpty()) {
            return chain.proceed(request)
        }

        call.listener.onLocalInterceptorStart(call, route)

        val interceptors =
            ArrayList<RouteInterceptor>(customInterceptors.size + moduleInterceptors.size + 1).apply {
                this += moduleInterceptors
                this += customInterceptors.map { clazz ->
                    clazz.fromServicesOrFactory(chain.config, chain.serviceCentral)
                }
                this += ContinueInterceptor(chain)
            }
        return RealChain(
            interceptors,
            chain
        ).proceed(chain.request).apply {
            call.listener.onLocalInterceptorEnd(call)
        }
    }

    private class ContinueInterceptor(val continueChain: ChainInternal) :
        RouteInterceptor {
        override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
            chain as ChainInternal
            return continueChain.proceed(
                chain.request,
                chain.context,
                chain.fragment,
                chain.mode,
                chain.route
                    ?: throw IllegalArgumentException("Custom interceptor returns null route!"),
                chain.call
            )
        }
    }
}