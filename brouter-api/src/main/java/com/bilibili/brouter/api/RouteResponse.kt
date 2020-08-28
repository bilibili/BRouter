package com.bilibili.brouter.api

import com.bilibili.brouter.api.internal.DefaultRouteResponse

/**
 * @author dieyi
 * Created at 2020/3/11.
 */
interface RouteResponse {
    /**
     * 响应码
     */
    val code: Code

    /**
     * 对应的路由请求
     */
    val request: RouteRequest

    /**
     * 响应消息
     */
    val message: String

    /**
     * 路由信息
     */
    val routeInfo: RouteInfo?

    val obj: Any?

    /**
     * 重定向路由
     */
    val redirect: RouteRequest?

    /**
     * 可能为 FLAG_SKIP_REST_TYPES 或者 0
     */
    val flags: Int

    /**
     * 上一个失败的路由响应
     */
    val priorFailureResponse: RouteResponse?
    /**
     * 上一个路由类型的失败的路由响应
     */
    val priorRouteTypeResponse: RouteResponse?
    /**
     * 上一个路由请求的失败的路由响应
     */
    val prevRequestResponse: RouteResponse?

    /**
     * code == OK
     */
    val isSuccess: Boolean

    fun toPrettyString(): String

    fun newBuilder(): Builder

    interface Builder {
        fun code(code: Code): Builder
        fun request(request: RouteRequest): Builder
        fun message(message: String): Builder
        fun routeInfo(routeInfo: RouteInfo?): Builder
        fun obj(obj: Any?): Builder
        fun redirect(redirect: RouteRequest?): Builder
        fun flags(flags: Int): Builder
        fun addFlag(flags: Int): Builder
        fun prevRequestResponse(prevRequestResponse: RouteResponse?): Builder
        fun priorFailureResponse(priorFailureResponse: RouteResponse?): Builder
        fun priorRouteTypeResponse(priorRouteTypeResponse: RouteResponse?): Builder
        fun build(): RouteResponse
    }

    enum class Code {
        OK,
        REDIRECT,
        BAD_REQUEST,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        ERROR,
        FOUND_STUB,
        UNSUPPORTED,
    }

    companion object {
        /**
         * 跳过剩余的路由类型
         */
        const val FLAG_SKIP_REST_TYPES = 0x2

        operator fun invoke(
            code: Code,
            request: RouteRequest,
            message: String = code.name,
            routeInfo: RouteInfo? = null
        ): RouteResponse {
            return DefaultRouteResponse(
                code,
                request,
                message,
                routeInfo,
                null,
                null,
                0,
                null,
                null,
                null
            )
        }
    }
}

fun RouteRequest.buildResponse(code: RouteResponse.Code): RouteResponse.Builder =
    DefaultRouteResponse.Builder(code, this)

fun RouteRequest.redirectTo(redirect: RouteRequest): RouteResponse =
    DefaultRouteResponse.Builder(RouteResponse.Code.REDIRECT, this)
        .redirect(redirect)
        .build()