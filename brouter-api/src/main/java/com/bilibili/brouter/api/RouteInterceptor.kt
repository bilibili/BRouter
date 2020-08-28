package com.bilibili.brouter.api

import android.content.Context
import com.bilibili.brouter.api.internal.HasInternalProtocol
import com.bilibili.brouter.stub.Fragment

interface RouteInterceptor {

    fun intercept(chain: Chain): RouteResponse

    @HasInternalProtocol
    interface Chain {

        /**
         * The request mode.
         */
        val mode: RequestMode

        /**
         * The request.
         */
        val request: RouteRequest

        /**
         * The context from.
         */
        val context: Context

        /**
         * The fragment from.
         */
        val fragment: Fragment?

        /**
         * The matched route for current request.
         * Always null in pre-match Interceptor, never null for others.
         */
        val route: RouteInfo?

        /**
         * return a new Chain with the new route.
         */
        fun withRoute(newRoute: RouteInfo): Chain

        /**
         * Change the request mode for subsequent interceptors,
         * but the current interceptor should handle the original mode and return's correspond data when code is OK.
         * OPEN: Intent(for open later) or any type(already open)
         * ROUTE: RouteInfo
         * INTENT: Intent
         */
        fun withMode(mode: RequestMode): Chain

        /**
         * Continue the chain.
         */
        fun proceed(request: RouteRequest): RouteResponse
    }
}