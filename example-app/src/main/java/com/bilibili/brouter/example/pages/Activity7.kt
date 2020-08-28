package com.bilibili.brouter.example.pages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bilibili.brouter.api.*
import com.bilibili.brouter.example.R
import com.bilibili.brouter.example.extensions.base.BaseActivity

@Routes(
    "coffee://example/ac7",
    launcher = CustomLauncher::class
)
class Activity7 : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_page)
        findViewById<TextView>(R.id.tv_text)
            .text = "Extras: \n${intent.extras?.apply { get("") }}"
    }
}

class CustomLauncher : Launcher() {
    override fun launch(
        context: Context,
        fragment: Fragment?,
        request: RouteRequest,
        route: RouteInfo
    ): RouteResponse {
        // very simple, no args, no props
        context.startActivity(Intent(context, route.target))
        return request.buildResponse(RouteResponse.Code.OK)
            .routeInfo(route)
            .build()
    }
}