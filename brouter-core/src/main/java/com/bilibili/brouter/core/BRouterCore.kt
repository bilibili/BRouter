package com.bilibili.brouter.core

import android.app.Application
import com.bilibili.brouter.api.*
import com.bilibili.brouter.core.internal.config.ConfigurationImpl
import com.bilibili.brouter.api.ModuleCentral
import com.bilibili.brouter.api.internal.incubating.RouterBridge
import com.bilibili.brouter.api.internal.module.ModuleContainer
import com.bilibili.brouter.core.internal.module.ModuleManager
import com.bilibili.brouter.core.internal.routes.RealRouteCall

object BRouterCore {

    // internal API
    private val manager = ModuleManager()

    @JvmStatic
    @JvmOverloads
    fun setUp(app: Application, build: (GlobalConfiguration.Builder) -> Unit = { }) {
        BRouter.setBridge(DefaultRouterBridge)
        val conf = ConfigurationImpl.Builder(app).let {
            build(it)
            it.build()
        }
        manager.init(conf)
    }

    @JvmStatic
    fun installModules(vararg clazz: Class<out ModuleContainer>) {
        manager.install(*clazz)
    }

    @JvmStatic
    fun postCreateModules() {
        manager.dispatchPostCreate()
    }

    private object DefaultRouterBridge : RouterBridge {
        override val module: ModuleCentral
            get() = manager
        override val serviceCentral: ServiceCentral
            get() = manager.serviceCentral

        override fun newCall(request: RouteRequest, params: CallParams): RouteCall {
            require(params.fragment == null || params.context == null) {
                "Can't contain both fragment and context."
            }
            return RealRouteCall.newCall(request, params, manager)
        }
    }
}