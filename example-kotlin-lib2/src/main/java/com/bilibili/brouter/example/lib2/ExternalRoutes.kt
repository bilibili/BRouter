package com.bilibili.brouter.example.lib2

import android.content.Context
import android.content.Intent
import com.bilibili.brouter.api.IntentCreator
import com.bilibili.brouter.api.RouteInfo
import com.bilibili.brouter.api.RouteRequest
import com.bilibili.brouter.api.Routes


@Routes("zhihu://**")
class ZhihuSearch : IntentCreator {
    override fun createIntent(context: Context, request: RouteRequest, route: RouteInfo): Intent? {
        return Intent(Intent.ACTION_VIEW, request.targetUri)
    }
}

@Routes("bilibili://video/{id}", "(http|https)://(m|www).bilibili.com/{id}")
class BilibiliVideo : IntentCreator {
    override fun createIntent(context: Context, request: RouteRequest, route: RouteInfo): Intent? {
        return Intent(Intent.ACTION_VIEW, request.targetUri)
    }
}

