package com.bilibili.brouter.core.internal.interceptors

import android.content.Intent
import com.bilibili.brouter.api.RequestMode
import com.bilibili.brouter.api.RouteInterceptor
import com.bilibili.brouter.api.RouteRequest
import com.bilibili.brouter.api.RouteResponse
import com.bilibili.brouter.api.internal.incubating.ChainInternal
import com.bilibili.brouter.api.internal.incubating.RouteCallInternal

object MultiRequestInterceptor : RouteInterceptor {

    @Suppress("UNCHECKED_CAST")
    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        chain as ChainInternal
        val call = chain.call
        val request = chain.request
        val lastResponse = chain.nextWithEvent(call, request)

        return if (lastResponse.isSuccess) {
            val mode = chain.mode
            if (mode == RequestMode.OPEN) {
                val wantedIntent = lastResponse.obj
                if (wantedIntent !is Intent) { // open directly by launcher or interceptor, ignore prev
                    lastResponse
                } else {
                    val intents = mutableListOf<Intent>()
                    val finalResponse = chain.withMode(RequestMode.INTENT)
                        .linkPrev(call, request.prev, lastResponse, intents)

                    if (!finalResponse.isSuccess) {
                        return finalResponse
                    }

                    val launcher = chain.config.globalLauncher

                    call.listener.onLaunchStart(call, true)
                    val response = launcher.launch(
                        chain.context,
                        chain.fragment,
                        chain.request,
                        intents.toTypedArray()
                    )
                    (if (response.isSuccess) {
                        // If success, use last response
                        finalResponse
                    } else {
                        response.newBuilder()
                            .prevRequestResponse(finalResponse)
                            .build()
                    }).apply {
                        call.listener.onLaunchEnd(call, this)
                    }
                }
            } else {
                chain.linkPrev(call, request.prev, lastResponse, null)
            }
        } else {
            lastResponse
        }
    }
}

private fun RouteInterceptor.Chain.linkPrev(
    call: RouteCallInternal,
    request: RouteRequest?,
    nextResponse: RouteResponse,
    intents: MutableList<Intent>?
): RouteResponse {
    return if (request != null && nextResponse.request.forward == null) {
        val currentResponseWithoutPre = nextWithEvent(call, request)
        if (!currentResponseWithoutPre.isSuccess) {
            return currentResponseWithoutPre
        }
        val finalCurrentOrPrevFailureResponse =
            linkPrev(call, request.prev, currentResponseWithoutPre, intents)
        if (!finalCurrentOrPrevFailureResponse.isSuccess) {
            return finalCurrentOrPrevFailureResponse
        }
        intents?.add(nextResponse.obj as Intent)
        nextResponse.newBuilder()
            .prevRequestResponse(finalCurrentOrPrevFailureResponse)
            .build()
    } else {
        intents?.add(nextResponse.obj as Intent)
        nextResponse
    }
}

private fun RouteInterceptor.Chain.nextWithEvent(
    call: RouteCallInternal,
    request: RouteRequest
): RouteResponse {
    call.listener.onSubRequestStart(call, request)
    return proceed(request).apply {
        call.listener.onSubRequestEnd(call, this)
    }
}