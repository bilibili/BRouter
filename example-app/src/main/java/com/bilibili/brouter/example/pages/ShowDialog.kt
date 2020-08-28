package com.bilibili.brouter.example.pages

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bilibili.brouter.api.*

/**
 * 这是一个示例，实践中推荐使用 Service。
 * @author dieyi
 * Created at 2020/6/10.
 */
@Routes(
    "coffee://example/show_dialog1"
)
class ShowDialog1 : Launcher() {
    override fun launch(
        context: Context,
        fragment: Fragment?,
        request: RouteRequest,
        route: RouteInfo
    ): RouteResponse {
        AlertDialog.Builder(context)
            .setTitle(request.params.getLast("title") ?: "默认标题")
            .setMessage(request.params.getLast("message") ?: "默认消息")
            .setNegativeButton("关闭") { d, i ->
                Log.i("BRouter", "关闭了Dialog1")
            }
            .create().show()
        return RouteResponse(RouteResponse.Code.OK, request, routeInfo = route)
    }
}

