package com.bilibili.brouter.api

import android.app.Application
import android.util.Log
import com.bilibili.brouter.api.internal.HasInternalProtocol
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskExecutionListener
import java.util.concurrent.ExecutorService


@HasInternalProtocol
interface GlobalConfiguration {
    val preMatchInterceptors: List<RouteInterceptor>
    val postMatchInterceptors: List<RouteInterceptor>
    val logger: SimpleLogger
    val emptyRouteTypeHandler: EmptyTypeHandler
    val authenticator: RouteAuthenticator
    val executor: ExecutorService
    val servicesMissFactory: OnServicesMissFactory
    val routerListenerFactory: RouteListener.Factory
    val moduleMissingReactor: ModuleMissingReactor
    val defaultScheme: String
    val globalLauncher: GlobalLauncher
    val taskExecutionListener: TaskExecutionListener
    val taskComparator: Comparator<Task>
    val app: Application
    fun newBuilder(): Builder

    interface Builder {
        /**
         * 返回一个可修改的全局匹配前路由拦截器列表
         */
        val preMatchInterceptors: MutableList<RouteInterceptor>
        /**
         * 返回一个可修改的全局匹配后路由拦截器列表
         */
        val postMatchInterceptors: MutableList<RouteInterceptor>
        /**
         * 路由特性匹配规则
         */
        val attributeSchema: AttributeSchema

        /**
         * 默认 scheme，如果路由规则和路由请求中不带 scheme 时则为该 scheme
         */
        fun defaultScheme(defaultScheme: String): Builder

        /**
         * 简单的 logger，路由内部使用
         */
        fun logger(logger: SimpleLogger): Builder

        /**
         * 当 RouteRequest 未指定路由类型时，则由它决定
         */
        fun emptyRouteTypeHandler(handler: EmptyTypeHandler): Builder

        /**
         * Use to create a instance of Interceptor or Custom Launcher if no correspond Service.
         * 需要创建拦截器或者自定义启动器对象时，如果没有对应的服务，则由它来创建
         */
        fun servicesMissFactory(factory: OnServicesMissFactory): Builder

        /**
         * 如果 RouteResponse.Code 是 UNAUTHORIZED, 则尝试重定向到一个认证的路由请求
         */
        fun authenticator(authenticator: RouteAuthenticator): Builder

        /**
         * 追加一个全局匹配前路由拦截器，此时 chain.route == null
         */
        fun addPreMatchInterceptor(interceptor: RouteInterceptor): Builder

        /**
         * 追加一个全局匹配后路由拦截器，chain.route != null
         * 对于单个 RouteRequest 可能会被执行多次，如 Redirect / Authorize / Multi RouteType
         */
        fun addPostMatchInterceptor(interceptor: RouteInterceptor): Builder

        /**
         * 配置一个路由请求的完整生命周期的事件监听。
         */
        fun routeListener(listener: RouteListener): Builder

        /**
         * 同上
         */
        fun routeListenerFactory(factory: RouteListener.Factory): Builder

        /**
         * 提供一个线程池供路由调度任务
         */
        fun executor(executor: ExecutorService): Builder

        /**
         * 配置路由特性匹配规则
         */
        fun attributeSchema(action: (AttributeSchema) -> Unit): Builder

        /**
         * 全局启动器
         * 启动器查找顺序：
         * 1. 目标类本身
         * 2. 路由注解指定
         * 3. 对应的路由类型是否存在对应名字的 Launcher 服务
         * 4. 全局启动器
         */
        fun globalLauncher(globalLauncher: GlobalLauncher): Builder

        /**
         * 任务执行前后监听
         */
        fun taskExecutionListener(taskExecutionListener: TaskExecutionListener): Builder

        /**
         * 决定并发任务的顺序
         */
        fun taskComparator(comparator: Comparator<Task>): Builder
    }
}

interface SimpleLogger {
    fun d(msg: () -> Any?)
    fun e(t: Throwable? = null, msg: () -> Any?)

    companion object ANDROID : SimpleLogger {
        override fun d(msg: () -> Any?) {
            Log.d("BRouter", msg().toString())
        }

        override fun e(t: Throwable?, msg: () -> Any?) {
            Log.e("BRouter", msg().toString(), t)
        }
    }
}

interface ModuleMissingReactor {
    fun onModuleMissing(moduleName: String, route: RouteInfo, request: RouteRequest): RouteRequest?

    companion object EMPTY : ModuleMissingReactor {
        override fun onModuleMissing(
            moduleName: String,
            route: RouteInfo,
            request: RouteRequest
        ): RouteRequest? = null
    }
}

interface EmptyTypeHandler : (RouteRequest) -> List<String> {

    companion object DEFAULT : EmptyTypeHandler {
        override fun invoke(p1: RouteRequest): List<String> =
            listOf(
                StandardRouteType.NATIVE,
                StandardRouteType.FLUTTER,
                StandardRouteType.APPLETS,
                StandardRouteType.WEB
            )
    }
}

interface RouteAuthenticator {
    fun authenticate(route: RouteInfo, response: RouteResponse): RouteRequest?

    companion object NONE : RouteAuthenticator {
        override fun authenticate(route: RouteInfo, response: RouteResponse): RouteRequest? = null
    }
}


open class RouteListener {

    open fun onCallStart(call: RouteCall) {
    }

    open fun onSubRequestStart(call: RouteCall, request: RouteRequest) {
    }

    open fun onLocalInterceptorStart(call: RouteCall, route: RouteInfo) {
    }

    open fun onLocalInterceptorEnd(call: RouteCall) {
    }

    open fun onSubRequestEnd(call: RouteCall, response: RouteResponse) {
    }

    open fun onLaunchStart(call: RouteCall, byGlobalLauncher: Boolean) {
    }

    open fun onLaunchEnd(call: RouteCall, response: RouteResponse) {
    }

    open fun onCallEnd(call: RouteCall, response: RouteResponse) {
    }

    interface Factory : Function1<RouteCall, RouteListener>

    companion object {

        fun factory(listener: RouteListener): Factory {
            return object : Factory {
                override fun invoke(call: RouteCall): RouteListener = listener
            }
        }
    }
}


interface OnServicesMissFactory {

    fun <T> create(clazz: Class<T>): T

    companion object REFLECTION : OnServicesMissFactory {
        override fun <T> create(clazz: Class<T>): T {
            return clazz.getDeclaredConstructor().run {
                isAccessible = true
                newInstance()
            }
        }
    }
}