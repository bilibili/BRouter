package com.bilibili.brouter.core.defaults

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.bilibili.brouter.api.*
import com.bilibili.brouter.stub.Fragment

/**
 * @author dieyi
 * Created at 2020/5/10.
 */
open class DefaultGlobalLauncher : GlobalLauncher {
    override fun launch(
        context: Context,
        fragment: Fragment?,
        request: RouteRequest,
        intents: Array<out Intent>
    ): RouteResponse {
        return if (intents.isEmpty())
            RouteResponse(RouteResponse.Code.ERROR, request, "No intent to launch.")
        else try {

            if (fragment != null) {
                if (request.requestCode >= 0) {
                    fragment.startActivityForResult(
                        intents.last(),
                        request.requestCode,
                        request.options
                    )
                } else {
                    fragment.startActivity(intents.last(), request.options)
                }
            } else if (context is Activity) {
                when {
                    request.requestCode >= 0 -> context.startActivityForResult(
                        intents.last(),
                        request.requestCode,
                        request.options
                    )
                    intents.size == 1 -> context.startActivity(intents[0], request.options)
                    else -> context.startActivities(
                        intents,
                        request.options
                    )
                }
            } else {
                intents.first().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intents.size == 1) {
                    context.startActivity(intents[0], request.options)
                } else {
                    context.startActivities(intents, request.options)
                }
            }

            if (context is Activity && (request.animIn != -1 || request.animOut != -1)) {
                context.overridePendingTransition(request.animIn, request.animOut)
            }

            RouteResponse(RouteResponse.Code.OK, request)
        } catch (e: ActivityNotFoundException) {
            request.buildResponse(RouteResponse.Code.ERROR)
                .message(e.toString())
                .obj(e)
                .build()
        }
    }

    override fun onInterceptIntent(
        context: Context,
        request: RouteRequest,
        route: RouteInfo,
        intent: Intent
    ): Intent {
        return intent
    }

    override fun createIntent(context: Context, request: RouteRequest, route: RouteInfo): Intent? {
        if (!Activity::class.java.isAssignableFrom(route.target)) {
            return null
        }
        return Intent().apply {
            setClass(context, route.target)
            putExtras(createExtrasWithParams(request, route).also {
                appendProps(it, request)
            })
            data = request.data
            flags = request.flags
        }
    }

    companion object {
        fun createExtrasWithParams(request: RouteRequest, route: RouteInfo): Bundle {
            val extras = request.extras?.let {
                Bundle(it)
            } ?: Bundle()
            route.captures.forEach { (key, value) ->
                extras.putString(key, value)
            }

            request.params.let {
                for (key in it.keySet) {
                    extras.putString(key, it.getLast(key))
                }
            }
            return extras
        }

        fun appendProps(bundle: Bundle, request: RouteRequest): Bundle {
            bundle.putParcelable(CROUTER_PROPS, request.props)
            request.forward?.let {
                bundle.putParcelable(CROUTER_FORWARD, it)
            }
            return bundle
        }
    }
}