package com.bilibili.brouter.api

import android.content.Context
import android.content.Intent
import com.bilibili.brouter.api.internal.HasInternalProtocol
import com.bilibili.brouter.stub.Fragment
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Routes(

    /**
     * One or more rule definition, like (http|https)://(m|www).bilibili.com/BV{id}".
     */
    vararg val value: String,

    /**
     * Whether to export the above rules to AndroidManifest.
     */
    val exported: Boolean = false,

    /**
     * Page name, for aggregation report.
     * If empty, the first rule will be used.
     */
    val routeName: String = "",

    /**
     * The route descriptor.
     */
    val desc: String = "",

    /**
     * The correspond runtime.
     */
    val routeType: String = StandardRouteType.NATIVE,

    /**
     * Route Interceptor.
     */
    val interceptors: Array<KClass<out RouteInterceptor>> = [],

    /**
     * The correspond launcher, if not changed, The default launcher will start this route.
     */
    val launcher: KClass<out Launcher> = Launcher::class
)

object StandardRouteType {
    const val NATIVE = "native"
    const val FLUTTER = "flutter"
    const val APPLETS = "applets"
    const val WEB = "web"
}

@HasInternalProtocol
interface RouteCall {

    /**
     * 请求额外信息
     */
    val params: CallParams

    /**
     * 路由请求
     */
    val request: RouteRequest

    val executed: Boolean

    fun execute(): RouteResponse

    fun clone(): RouteCall

    interface Factory {
        fun newCall(request: RouteRequest, params: CallParams): RouteCall
    }
}

data class CallParams @JvmOverloads constructor(
    val mode: RequestMode = RequestMode.OPEN,
    val context: Context? = null,
    val fragment: Fragment? = null
)

@HasInternalProtocol
interface RouteInfo : HasAttributes {

    val routeName: String
    /**
     * The rule.
     */
    val routeRule: String
    /**
     * Type of the route.
     */
    val routeType: String

    /**
     * Captures.
     */
    val captures: Map<String, String>

    /**
     * Matched class.
     */
    val target: Class<*>

    /**
     * Interceptors of target route.
     */
    val interceptors: Array<out Class<out RouteInterceptor>>

    /**
     * The module to which the route belongs
     */
    val module: Module

    /**
     * launcher of target route.
     */
    val launcher: Class<out Launcher>

    /**
     * Attributes of the route, include module's attributes.
     */
    override val attributes: AttributeContainer

    /**
     * Return a new RouteInfo with new a target class.
     */
    fun withTarget(clazz: Class<*>): RouteInfo
}

abstract class Launcher : IntentCreator {

    open fun launch(
        context: Context,
        fragment: Fragment?,
        request: RouteRequest,
        route: RouteInfo
    ): RouteResponse = throw UnsupportedOperationException()

    /**
     * If support create intent, launch will not called.
     */
    override fun createIntent(context: Context, request: RouteRequest, route: RouteInfo): Intent? =
        null
}

interface GlobalLauncher : IntentCreator {
    fun launch(
        context: Context,
        fragment: Fragment?,
        request: RouteRequest,
        intents: Array<out Intent>
    ): RouteResponse

    fun onInterceptIntent(
        context: Context,
        request: RouteRequest,
        route: RouteInfo,
        intent: Intent
    ): Intent
}

enum class RequestMode {
    /**
     * 打开目标
     */
    OPEN,
    /**
     * 获取目标的Intent
     */
    INTENT,

    /**
     * 获取目标的路由信息
     */
    ROUTE
}

interface IntentCreator {
    /**
     * Return's null for unsupported routes.
     */
    fun createIntent(context: Context, request: RouteRequest, route: RouteInfo): Intent?
}

const val CROUTER_PROPS = "brouter.props"
const val CROUTER_FORWARD = "brouter.forward"