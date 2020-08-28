package com.bilibili.brouter.example.lib2

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.bilibili.brouter.api.*
import com.bilibili.brouter.example.extensions.base.BaseFragment
import com.bilibili.brouter.example.extensions.launcher.CustomDefaultLauncher
import com.bilibili.brouter.example.lib2.WebFragment.Companion.PROP_SKIP_ME
import javax.inject.Named

@Routes(
    value = ["(http|https)://**"],
    routeType = StandardRouteType.WEB,
    interceptors = [SkipSelfPropInterceptor::class]
)
class WebFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_web, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webview = view.findViewById<WebView>(R.id.web_view)
        webview.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return Uri.parse(url).toBuilder()
                    .props {
                        it.put(PROP_SKIP_ME, "1")
                    }
                    .build()
                    .newCall(CallParams(fragment = this@WebFragment))
                    .execute()
                    .isSuccess
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Toast.makeText(activity, "WebView 加载 $url 中", Toast.LENGTH_SHORT).show()
            }
        }
        webview.settings.javaScriptEnabled = true
        webview.loadUrl(activity?.intent?.data?.toString() ?: error("No data to load."))
    }

    companion object {
        const val PROP_SKIP_ME = "skip.me"
    }
}


@Services(Launcher::class)
@Named(StandardRouteType.WEB)
class WebLauncher : Launcher() {
    override fun createIntent(context: Context, request: RouteRequest, route: RouteInfo): Intent? {
        // 偷个懒
        return CustomDefaultLauncher().createIntent(context, request, route).apply {
            if (data == null) {
                // 如果没有指定 data，就拿请求的 pure uri,即不包含属性的 uri
                data = request.pureUri
            }
        }
    }
}

class SkipSelfPropInterceptor : RouteInterceptor {
    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        return if (chain.request.props.contains(PROP_SKIP_ME)) {
            RouteResponse(RouteResponse.Code.FORBIDDEN, chain.request, "Found prop 'skip.me'.")
        } else {
            chain.proceed(chain.request)
        }
    }
}