package com.bilibili.brouter.example.extensions.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.bilibili.brouter.api.RouteInfo
import com.bilibili.brouter.api.RouteRequest
import com.bilibili.brouter.core.defaults.DefaultGlobalLauncher
import com.bilibili.brouter.example.extensions.container.SingleFragmentContainerActivity
import com.bilibili.brouter.example.extensions.container.SingleFragmentContainerActivity.Companion.createIntentForFragment

/**
 * 自定义全局 Launcher 示例。如果是 Fragment, 则交给 Fragment 专属的容器
 * @author dieyi
 * Created at 2020/6/2.
 */
class CustomDefaultLauncher : DefaultGlobalLauncher() {

    override fun createIntent(context: Context, request: RouteRequest, route: RouteInfo): Intent {
        val targetClass = route.target
        return when {
            Fragment::class.java.isAssignableFrom(targetClass) -> {
                val arguments = createExtrasWithParams(request, route)
                val host = targetClass.getAnnotation(CustomFragmentHost::class.java)?.value?.java
                    ?: SingleFragmentContainerActivity::class.java
                val extras = appendProps(Bundle(), request)
                val intent =
                    createIntentForFragment(
                        context,
                        targetClass.asSubclass(Fragment::class.java),
                        host,
                        arguments,
                        extras
                    )

                intent.flags = request.flags
                intent.data = request.data
                return intent
            }
            Activity::class.java.isAssignableFrom(targetClass) -> {
                // 偷个懒，直接用默认的了
                super.createIntent(context, request, route)!!
            }
            else -> error("Unsupported route $targetClass.")
        }
    }

    override fun onInterceptIntent(
        context: Context,
        request: RouteRequest,
        route: RouteInfo,
        intent: Intent
    ): Intent {
        intent.putExtra("brouter.rule", route.routeRule)
        return intent
    }
}