package com.bilibili.brouter.example

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskExecutionListener
import com.bilibili.brouter.core.BRouterCore
import com.bilibili.brouter.example.extensions.launcher.CustomDefaultLauncher
import javax.inject.Named

class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        AppProvider.app = this
    }

    override fun onCreate() {
        super.onCreate()
        BRouterCore.setUp(this) {
            it.logger(object : SimpleLogger {
                override fun d(msg: () -> Any?) {
                    Log.d("BRouter", msg().toString())
                }

                override fun e(t: Throwable?, msg: () -> Any?) {
                    Log.e("BRouter", msg().toString(), t)
                }
            })
                .defaultScheme("coffee")
                .authenticator(
                    object : RouteAuthenticator {
                        override fun authenticate(
                            route: RouteInfo,
                            response: RouteResponse
                        ): RouteRequest? {
                            return "coffee://login".toRouteRequest()
                        }
                    }
                )
                .emptyRouteTypeHandler(object : EmptyTypeHandler {
                    override fun invoke(p1: RouteRequest): List<String> {
                        return listOf(StandardRouteType.NATIVE, StandardRouteType.WEB)
                    }
                })
                .routeListenerFactory(RouteListener.factory(object : RouteListener() {

                    override fun onCallEnd(call: RouteCall, response: RouteResponse) {
                        Log.i("BRouter", "Call end: ${response.toPrettyString()}")
                    }
                }))
                .attributeSchema {
                    it.attribute("page1") {
                        it.addAttributeCompatibilityRule {
                            if (it.requestValue == "lib3" && it.producerValue == "lib1") {
                                it.compatible()
                            }
                        }
                    }
                }
                .taskExecutionListener(object : TaskExecutionListener {
                    override fun beforeExecute(task: Task) {
                        Log.i("BRouter", "task ${task.module.name}.${task.name} start")
                    }

                    override fun afterExecute(task: Task) {
                        Log.i("BRouter", "task ${task.module.name}.${task.name} end")
                    }
                })
                .addPreMatchInterceptor(object : RouteInterceptor {
                    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
                        return if (chain.request.targetUri.scheme == "black") {
                            Toast.makeText(chain.context, "black scheme 被拦截了", Toast.LENGTH_SHORT)
                                .show()
                            chain.request.buildResponse(RouteResponse.Code.FORBIDDEN)
                                .build()
                        } else {
                            chain.proceed(chain.request)
                        }
                    }
                })
                .globalLauncher(CustomDefaultLauncher())
        }
    }
}

object AppProvider {
    @JvmStatic
    @get:Named("app")
    @get:Services(Application::class, Context::class)
    lateinit var app: Application
}