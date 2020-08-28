package com.bilibili.brouter.core.internal.interceptors

import android.content.Intent
import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.internal.incubating.RouteInfoInternal
import com.bilibili.brouter.core.internal.routes.RealChain
import com.bilibili.brouter.core.internal.routes.StubRoutesImpl

internal class RetryAndFollowUpInterceptor() :
    RouteInterceptor {

    private var followUpCount = 0

    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        chain as RealChain

        followUpCount = 0

        return chain.processRequest(chain.request)
    }

    private fun RealChain.processRequest(
        request: RouteRequest,
        priorResponse: RouteResponse? = null
    ): RouteResponse {
        var priorTypeResponse: RouteResponse? = null

        val pendingTypes = request.routeTypes.let {
            if (it.isEmpty()) {
                config.emptyRouteTypeHandler(request)
            } else {
                it
            }
        }

        for (type in pendingTypes) {
            val queryTableResponse = routeCentral.findRoute(request, type)

            val failureResponse = if (queryTableResponse.isSuccess) {
                val route = queryTableResponse.routeInfo as RouteInfoInternal
                val routeResponse = processSingleType(route, request)
                if (routeResponse.isSuccess || (routeResponse.flags and RouteResponse.FLAG_SKIP_REST_TYPES) != 0) {
                    if (routeResponse.isSuccess && mode == RequestMode.INTENT) {
                        val obj = routeResponse.obj
                        require(obj != null && Intent::class.java.isAssignableFrom(obj.javaClass)) {
                            "$routeResponse is success, expect Intent but is $obj, " +
                                    "please check post-match global interceptors and ${route.interceptors.contentToString()}."
                        }
                    }
                    return routeResponse
                        .newBuilder()
                        .priorFailureResponse(priorResponse)
                        .priorRouteTypeResponse(priorTypeResponse)
                        .build()
                }
                routeResponse
            } else {
                queryTableResponse
            }
            priorTypeResponse = if (priorTypeResponse == null) {
                failureResponse
            } else {
                failureResponse
                    .newBuilder()
                    .priorRouteTypeResponse(priorTypeResponse)
                    .build()
            }
        }

        return (priorTypeResponse?.newBuilder()
            ?: request.buildResponse(RouteResponse.Code.NOT_FOUND))
            .priorFailureResponse(priorResponse)
            .build()
    }

    private fun RealChain.processSingleType(
        _route: RouteInfoInternal,
        request: RouteRequest
    ): RouteResponse {
        var routeInfo = _route
        routeInfo.routes.let {
            if (it is StubRoutesImpl) {
                val m = moduleCentral.getModuleImpl(it.moduleName)
                if (m == null) {
                    val response = RouteResponse(
                        RouteResponse.Code.FOUND_STUB,
                        request,
                        "Stub module: ${it.moduleName}"
                    )
                    return config.moduleMissingReactor.onModuleMissing(
                        it.moduleName,
                        routeInfo, request
                    )?.let {
                        if (++followUpCount > MAX_FOLLOW_TIMES) {
                            response.toManyFollowUp()
                        } else {
                            processRequest(
                                it.followUpRequest(request),
                                response
                            )
                        }
                    } ?: response
                } else {
                    // 再查一次表，防止查找路由时正好在安装
                    routeCentral.findRoute(request, routeInfo.routeType).let { r ->
                        if (!r.isSuccess) {
                            return RouteResponse(
                                RouteResponse.Code.ERROR,
                                request,
                                "First query result is StubModule ${it.moduleName}, second is failed"
                            )
                        } else {
                            val secondRoutes = r.obj as RouteInfoInternal
                            if (secondRoutes.routes is StubRoutesImpl) {
                                val routes = secondRoutes.routes as StubRoutesImpl
                                return RouteResponse(
                                    RouteResponse.Code.ERROR,
                                    request,
                                    if (routes.moduleName == it.moduleName) "StubModule '${routes.moduleName}' installed but no actual route found"
                                    else "First query result is StubModule '${it.moduleName}', second is StubModule '${routes.moduleName}'"
                                )
                            } else {
                                // 更新为新的有效路由
                                routeInfo = secondRoutes
                            }
                        }
                    }
                }
            }

            var response = proceed(request, route = routeInfo)

            if (response.isSuccess && response.routeInfo == null) {
                error("Success but routeInfo is null: $response.")
            }

            val followUp = when (response.code) {
                RouteResponse.Code.REDIRECT -> {
                    response.redirect ?: error(
                        "Redirect but no redirect request found: $response."
                    )
                }
                RouteResponse.Code.UNAUTHORIZED -> {
                    config.authenticator.authenticate(routeInfo, response)?.followUpRequest(request)
                }
                else -> null
            }

            followUp?.let {
                response = if (++followUpCount > MAX_FOLLOW_TIMES) {
                    response.toManyFollowUp()
                } else {
                    processRequest(it, response)
                }
            }
            return response
        }
    }
}

private fun RouteResponse.toManyFollowUp(): RouteResponse {
    return newBuilder()
        .code(RouteResponse.Code.ERROR)
        .priorFailureResponse(this)
        .message("Too many follow-up requests: $MAX_FOLLOW_TIMES")
        .build()
}

private const val MAX_FOLLOW_TIMES = 20

private fun RouteRequest.followUpRequest(originalRequest: RouteRequest): RouteRequest {
    return newBuilder()
        .requestCode(originalRequest.requestCode)
        .prev(null)
        .forward(
            originalRequest
                .newBuilder()
                .requestCode(-1)
                .addFlag(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                .build()
        )
        .build()
}