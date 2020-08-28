package com.bilibili.brouter.example.extensions.container

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bilibili.brouter.api.*
import com.bilibili.brouter.core.defaults.DefaultGlobalLauncher
import com.bilibili.brouter.example.extensions.R
import com.bilibili.brouter.example.extensions.base.BaseActivity

@Routes(
    routeName = "container.pager",
    desc = "简单的 Pager 容器示例",
    value = ["coffee://container/pager"]
)
class PagerContainerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val props = props

        val pages = props?.getAll(PROP_PAGER_ROUTE).orEmpty().mapNotNull {
            val request = it.toRouteRequest()
            val response = request.newCall(CallParams(RequestMode.ROUTE, this)).execute()
            response.routeInfo?.let {
                it to request
            }
        }

        setContentView(R.layout.pager_container)
        findViewById<ViewPager>(R.id.view_pager).adapter =
            object : FragmentStatePagerAdapter(supportFragmentManager) {
                override fun getItem(position: Int): Fragment {
                    val (route, request) = pages[position]
                    return Fragment.instantiate(
                        this@PagerContainerActivity, route.target.name,
                        DefaultGlobalLauncher.createExtrasWithParams(request, route)
                    )
                }

                override fun getPageTitle(position: Int): CharSequence? {
                    val (route, request) = pages[position]
                    return request.props[PROP_PAGER_TITLE] ?: route.routeName
                }

                override fun getCount(): Int = pages.size
            }
    }

    companion object {
        val PROP_PAGER_ROUTE = "pager.route"
        val PROP_PAGER_TITLE = "pager.title"
    }
}