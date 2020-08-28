package com.bilibili.brouter.api.internal

import com.bilibili.brouter.api.RouteInfo
import com.bilibili.brouter.api.RouteRequest
import com.bilibili.brouter.api.RouteResponse

internal class DefaultRouteResponse(
    override val code: RouteResponse.Code,
    override val request: RouteRequest,
    override val message: String,
    override val routeInfo: RouteInfo?,
    override val obj: Any?,
    override val redirect: RouteRequest?,
    override val flags: Int,
    override val priorFailureResponse: RouteResponse?,
    override val priorRouteTypeResponse: RouteResponse?,
    override val prevRequestResponse: RouteResponse?
) : RouteResponse {

    internal constructor(builder: Builder) : this(
        builder.code,
        builder.request,
        builder.message,
        builder.routeInfo,
        builder.obj,
        builder.redirect,
        builder.flags,
        builder.priorFailureResponse,
        builder.priorRouteTypeResponse,
        builder.prevRequestResponse
    )

    override val isSuccess get() = code == RouteResponse.Code.OK

    override fun toPrettyString(): String {
        return StringBuilder(128)
            .apply {
                appendToWithPrefix(this, "RouteResponse", 0)
            }
            .toString()
    }

    override fun newBuilder() = Builder(this)
    override fun toString(): String {
        return "RouteResponse(code=$code, request=$request, message='$message', routeInfo=$routeInfo, obj=$obj, redirect=$redirect, flags=$flags, priorFailureResponse=$priorFailureResponse, priorRouteTypeResponse=$priorRouteTypeResponse, prevRequestResponse=$prevRequestResponse)"
    }


    internal class Builder : RouteResponse.Builder {
        internal var code: RouteResponse.Code
        internal var request: RouteRequest
        internal var message: String
        internal var routeInfo: RouteInfo?
        internal var obj: Any?
        internal var redirect: RouteRequest?
        internal var flags: Int
        internal var priorFailureResponse: RouteResponse?
        internal var priorRouteTypeResponse: RouteResponse?
        internal var prevRequestResponse: RouteResponse?


        internal constructor(code: RouteResponse.Code, request: RouteRequest) {
            this.code = code
            this.request = request
            this.message = code.name
            this.routeInfo = null
            this.obj = null
            this.redirect = null
            this.flags = 0
            this.priorFailureResponse = null
            this.priorRouteTypeResponse = null
            this.prevRequestResponse = null
        }

        internal constructor(response: RouteResponse) {
            this.code = response.code
            this.request = response.request
            this.message = response.message
            this.routeInfo = response.routeInfo
            this.obj = response.obj
            this.redirect = response.redirect
            this.flags = response.flags
            this.priorFailureResponse = response.priorFailureResponse
            this.priorRouteTypeResponse = response.priorRouteTypeResponse
            this.prevRequestResponse = response.prevRequestResponse
        }

        override fun code(code: RouteResponse.Code): RouteResponse.Builder = this.apply {
            this.code = code
        }

        override fun request(request: RouteRequest): RouteResponse.Builder = this.apply {
            this.request = request
        }

        override fun message(message: String): RouteResponse.Builder = this.apply {
            this.message = message
        }

        override fun routeInfo(routeInfo: RouteInfo?): RouteResponse.Builder = this.apply {
            this.routeInfo = routeInfo
        }

        override fun obj(obj: Any?): RouteResponse.Builder = this.apply {
            this.obj = obj
        }

        override fun redirect(redirect: RouteRequest?): RouteResponse.Builder = this.apply {
            this.redirect = redirect
        }

        override fun flags(flags: Int): RouteResponse.Builder = this.apply {
            this.flags = flags
        }

        override fun addFlag(flags: Int): RouteResponse.Builder = this.apply {
            this.flags = this.flags or flags
        }

        override fun priorFailureResponse(priorFailureResponse: RouteResponse?): RouteResponse.Builder =
            this.apply {
                this.priorFailureResponse = priorFailureResponse
            }

        override fun priorRouteTypeResponse(priorRouteTypeResponse: RouteResponse?): RouteResponse.Builder =
            this.apply {
                this.priorRouteTypeResponse = priorRouteTypeResponse
            }

        override fun prevRequestResponse(prevRequestResponse: RouteResponse?): RouteResponse.Builder =
            this.apply {
                this.prevRequestResponse = prevRequestResponse
            }

        override fun build(): RouteResponse = DefaultRouteResponse(this)

        override fun toString(): String {
            return "RouteResponse.Builder(code=$code, request=$request, message='$message', routeInfo=$routeInfo, obj=$obj, redirect=$redirect, flags=$flags, priorFailureResponse=$priorFailureResponse, priorRouteTypeResponse=$priorRouteTypeResponse, prevRequestResponse=$prevRequestResponse)"
        }
    }
}

fun RouteResponse.appendToWithPrefix(builder: StringBuilder, name: String, i: Int) {
    builder
        .appendPrefix(i)
        .append(name)
        .append(" Code: ")
        .append(code)
        .append('\n')

        .appendPrefix(i)
        .append(" Flags: 0x")
        .append(java.lang.Integer.toHexString(flags))
        .append('\n')

        .appendPrefix(i)
        .append(" Message: ")
        .append(message)
        .append('\n')

        .appendPrefix(i)
        .append(" RouteInfo: ")
        .append(routeInfo)
        .append('\n')

        .appendPrefix(i)
        .append(" Obj: ")
        .append(obj)
        .append('\n')

    request.appendToWithPrefix(builder, "Request", i, false)
    redirect?.appendToWithPrefix(builder, "RedirectRequest", i + 1, false)
    priorFailureResponse?.appendToWithPrefix(builder, "PriorFailureResponse", i + 1)
    priorRouteTypeResponse?.appendToWithPrefix(builder, "PriorRouteTypeResponse", i + 1)

    prevRequestResponse?.appendToWithPrefix(builder, "PrevRequestResponse", i)
}