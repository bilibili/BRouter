package com.bilibili.brouter.core.internal.interceptors

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.incubating.ChainInternal
import com.bilibili.brouter.api.internal.incubating.RouteInfoInternal

object FinalInterceptor : RouteInterceptor {

    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        val request = chain.request

        if (chain.mode == RequestMode.ROUTE) {
            return RouteResponse(RouteResponse.Code.OK, request, routeInfo = chain.route)
        }

        chain as ChainInternal
        val route = chain.route!!
        val clazz = route.target

        val creator = if (IntentCreator::class.java.isAssignableFrom(clazz)) {
            clazz.fromServicesOrFactory(chain.config, chain.serviceCentral) as IntentCreator
        } else {
            chain.serviceCentral.findLauncherOrGlobalLauncher(route, chain.config)
        }
        val intent = creator.createIntent(chain.context, request, route)
        return if (intent == null) {
            if (chain.mode == RequestMode.OPEN && creator is Launcher) {
                val call = chain.call
                call.listener.onLaunchStart(call, false)
                creator.launch(chain.context, chain.fragment, request, route).apply {
                    call.listener.onLaunchEnd(call, this)
                }
            } else {
                RouteResponse(
                    RouteResponse.Code.UNSUPPORTED, request,
                    "$creator don't support create intent for ${request}."
                )
            }
        } else {
            request.buildResponse(RouteResponse.Code.OK)
                .routeInfo(chain.route)
                .obj(
                    chain.config.globalLauncher.onInterceptIntent(
                        chain.context,
                        request,
                        route,
                        intent
                    )
                )
                .build()
        }
    }
}

internal fun ServiceCentral.findLauncherOrGlobalLauncher(
    route: RouteInfoInternal,
    config: GlobalConfiguration
): IntentCreator {
    val launcherClass = route.launcher
    return if (launcherClass === Launcher::class.java) {
        this.getService(launcherClass, route.routeType)
            ?: config.globalLauncher
    } else {
        launcherClass.fromServicesOrFactory(config, this)
    }
}

internal fun <T : Any> Class<out T>.fromServicesOrFactory(
    config: GlobalConfiguration,
    central: ServiceCentral
): T {
    return central.getService(this, DEFAULT)
        ?: requireNotNull(config.servicesMissFactory.create(this)) {
            "MissFactory returns null for class ${this.canonicalName}"
        }
}