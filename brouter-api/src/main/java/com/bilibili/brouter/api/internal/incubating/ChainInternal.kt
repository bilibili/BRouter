package com.bilibili.brouter.api.internal.incubating

import android.content.Context
import com.bilibili.brouter.api.*
import com.bilibili.brouter.stub.Fragment

interface ChainInternal : RouteInterceptor.Chain {

    val call: RouteCallInternal

    val serviceCentral: ServiceCentral

    val config: GlobalConfiguration

    override val route: RouteInfoInternal?

    fun proceed(
        request: RouteRequest = this.request,
        context: Context = this.context,
        fragment: Fragment? = this.fragment,
        mode: RequestMode = this.mode,
        route: RouteInfoInternal? = this.route,
        call: RouteCallInternal = this.call
    ): RouteResponse
}