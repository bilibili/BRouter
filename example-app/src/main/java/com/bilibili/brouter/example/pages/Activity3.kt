package com.bilibili.brouter.example.pages

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import com.bilibili.brouter.api.RouteInterceptor
import com.bilibili.brouter.api.RouteResponse
import com.bilibili.brouter.api.Routes
import com.bilibili.brouter.api.redirectTo
import com.bilibili.brouter.example.R
import com.bilibili.brouter.example.extensions.base.BaseActivity

@Routes(
    value = ["coffee://example/ac3"],
    interceptors = [MayRedirectInterceptor::class]
)
class Activity3 : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_page)
        findViewById<TextView>(R.id.tv_text)
            .text = "Extras: \n${intent.extras?.apply { get("") }}"
    }
}

class MayRedirectInterceptor : RouteInterceptor {

    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        return if (Math.random() >= 0.5) {
            chain.request.redirectTo(
                // 用 newBuilder 保留其他参数
                chain.request.newBuilder()
                    .targetUri(Uri.parse("coffee://example/ac1"))
                    .build()
            )
        } else {
            chain.proceed(chain.request)
        }
    }
}
