package com.bilibili.brouter.example.pages

import android.os.Bundle
import android.widget.TextView
import com.bilibili.brouter.api.RouteInterceptor
import com.bilibili.brouter.api.RouteResponse
import com.bilibili.brouter.api.Routes
import com.bilibili.brouter.example.R
import com.bilibili.brouter.example.extensions.base.BaseActivity

@Routes(value = ["coffee://example/ac4"], interceptors = [ReplaceTargetInterceptor::class])
class Activity4 : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_page)
        findViewById<TextView>(R.id.tv_text)
            .text = "${this.javaClass.name}  Extras: \n${intent.extras?.apply { get("") }}"

    }
}

class ReplaceTargetInterceptor : RouteInterceptor {

    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        return (if (Math.random() >= 0.5) {
            chain.withRoute(chain.route!!.withTarget(Activity5::class.java))
        } else {
            chain
        }).proceed(chain.request)
    }
}

class Activity5 : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_page)
        findViewById<TextView>(R.id.tv_text)
            .text = "${this.javaClass.name} Extras: \n${intent.extras?.apply { get("") }}"

    }
}