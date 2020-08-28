package com.bilibili.brouter.core.internal.routes

import android.content.Context
import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.incubating.ChainInternal
import com.bilibili.brouter.api.internal.incubating.RouteCallInternal
import com.bilibili.brouter.api.internal.incubating.RouteInfoInternal
import com.bilibili.brouter.core.internal.module.ModuleCentralInternal
import com.bilibili.brouter.stub.Fragment

internal open class RealChain(
    private val interceptors: List<RouteInterceptor>,
    private val index: Int,
    override val request: RouteRequest,
    private val routeContext: RouteContext,
    override val mode: RequestMode,
    override val context: Context,
    override val fragment: Fragment? = null,
    override val route: RouteInfoInternal? = null
) : ChainInternal {

    internal constructor(interceptors: List<RouteInterceptor>, chain: RealChain) :
            this(
                interceptors,
                0,
                chain.request,
                chain.routeContext,
                chain.mode,
                chain.context,
                chain.fragment,
                chain.route
            )

    val moduleCentral: ModuleCentralInternal get() = routeContext.central

    val routeCentral: RouteCentralInternal get() = routeContext.central.routeCentral

    override val call: RouteCallInternal
        get() = routeContext.call
    override val serviceCentral: ServiceCentral
        get() = routeContext.central.serviceCentral
    override val config: GlobalConfiguration
        get() = routeContext.config

    private val currentInterceptor: RouteInterceptor get() = interceptors[index]

    override fun proceed(request: RouteRequest): RouteResponse =
        proceed(request, context, fragment, mode, route, call)

    override fun proceed(
        request: RouteRequest,
        context: Context,
        fragment: Fragment?,
        mode: RequestMode,
        route: RouteInfoInternal?,
        call: RouteCallInternal
    ): RouteResponse {
        if (index >= interceptors.size) {
            throw AssertionError()
        }
        val next = RealChain(
            interceptors,
            index + 1,
            request,
            routeContext,
            mode,
            context,
            fragment,
            route
        )
        return currentInterceptor.intercept(next)
    }

    override fun withRoute(newRoute: RouteInfo): RouteInterceptor.Chain =
        RealChain(
            interceptors,
            index,
            request,
            routeContext,
            mode,
            context,
            fragment,
            if (newRoute is RouteInfoInternal) newRoute else error("Don't use custom routeInfo")
        )

    override fun withMode(mode: RequestMode): RouteInterceptor.Chain =
        RealChain(
            interceptors,
            index,
            request,
            routeContext,
            mode,
            context,
            fragment,
            route
        )
}

