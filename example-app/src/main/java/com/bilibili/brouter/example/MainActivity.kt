package com.bilibili.brouter.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.BRouter
import com.bilibili.brouter.example.extensions.container.PagerContainerActivity
import com.bilibili.brouter.example.lib1.LoginService
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var loginService: LoginService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BRouter.inject(this)

        findViewById<TextView>(R.id.tv_click1).setOnClickListener {
            BRouter.routeTo(Uri.parse("coffee://example/ac1?param1=1&param1=2")
                .toBuilder()
                .params {
                    it.put("param2", "3")
                }
                .overridePendingTransition(R.anim.bottom_to_top_in,R.anim.bottom_to_top_out)
                .requestCode(1)
                .build(), this)
        }
        findViewById<TextView>(R.id.tv_click2).setOnClickListener {
            // 默认 scheme 为 coffee
            Uri.parse("example/ac2")
                .toBuilder()
                .extras {
                    // 慎用， extra 作为 Bundle 本身是可变的，不能保证在路由过程中的一致性
                    it.putFloat("其他参数", 0.2f)
                }
                .prev("coffee://example/ac1?param=ppp".toRouteRequest())
                .build()
                .newCall(CallParams(context = this))
                .execute()
        }

        findViewById<TextView>(R.id.tv_click20).setOnClickListener {
            BRouter.routeTo("coffee://example/ac3?param=3".toRouteRequest(), this)
        }

        findViewById<TextView>(R.id.tv_click21).setOnClickListener {
            BRouter.routeTo("coffee://example/ac4?param=4".toRouteRequest(), this)
        }

        findViewById<TextView>(R.id.tv_click22).setOnClickListener {
            BRouter.routeTo("https://github.com/CoffeePartner/BRouter".toRouteRequest(), this)
        }

        findViewById<TextView>(R.id.tv_click22).setOnClickListener {
            BRouter.routeTo("https://github.com/CoffeePartner/BRouter".toRouteRequest(), this)
        }

        findViewById<TextView>(R.id.tv_click23).setOnClickListener {
            BRouter.routeTo("coffee://example/ac7".toRouteRequest(), this)
        }

        findViewById<TextView>(R.id.tv_click24).setOnClickListener {
            BRouter.routeTo("coffee://example/ac8?-Aabtest=b".toRouteRequest(), this)
        }

        findViewById<TextView>(R.id.tv_click3).setOnClickListener {
            "coffee://example/frag1?q=1&-Ptoolbar.title=自定义title".toRouteRequest()
                .newCall(CallParams(context = this))
                .execute()
        }
        findViewById<TextView>(R.id.tv_click4).setOnClickListener {
            "coffee://example/frag2?q=2&-Ptoolbar.hide=true&-Pup.prev=${Uri.encode("coffee://example/frag1?q=3")}".toRouteRequest()
                .newCall(CallParams(context = this))
                .execute()
        }
        findViewById<TextView>(R.id.tv_click5).setOnClickListener {
            "coffee://teenager"
                .toRouteRequest()
                .newCall(CallParams(context = this))
                .execute()
        }
        findViewById<TextView>(R.id.tv_click6).setOnClickListener {
            BRouter.routeTo("coffee://lib1/test1".toRouteRequest(), this)
        }
        findViewById<TextView>(R.id.tv_click7).setOnClickListener {
            BRouter.routeTo("https://dieyidezui.com".toRouteRequest(), this)
        }
        findViewById<TextView>(R.id.tv_click8).setOnClickListener {
            BRouter.routeTo(
                "https://dieyidezui.com?-Pup.types=web".toRouteRequest(), this
            )
        }
        findViewById<TextView>(R.id.tv_click9).setOnClickListener {
            BRouter.routeTo("black://xxx".toRouteRequest(), this)
        }
        findViewById<TextView>(R.id.tv_click30).setOnClickListener {
            BRouter.routeTo(
                "zhihu://search?-Pup.prev=${Uri.encode("coffee://example/ac1")}".toRouteRequest(),
                this
            )
        }

        findViewById<TextView>(R.id.tv_click31).setOnClickListener {
            BRouter.routeTo(
                "bilibili://video/BV1tC4y1H7yz?-Pup.prev=${Uri.encode("coffee://example/ac3")}".toRouteRequest(),
                this
            )
        }

        findViewById<TextView>(R.id.tv_click32).setOnClickListener {
            BRouter.routeTo(
                "coffee://example/show_dialog1?title=omg&message=ohhahahh".toRouteRequest(),
                this
            )
        }

        findViewById<TextView>(R.id.tv_click33).setOnClickListener {
            BRouter.routeTo(
                "coffee://container/pager".toRouteRequest()
                    .newBuilder()
                    .props {
                        it.append(PagerContainerActivity.PROP_PAGER_ROUTE,
                            Uri.parse("coffee://example/frag1?q=1")
                                .toBuilder()
                                .props {
                                    it[PagerContainerActivity.PROP_PAGER_TITLE] =
                                        "自定义Titie Fragment1"
                                }
                                .build()
                                .uniformUri
                                .toString()
                        ).append(
                            PagerContainerActivity.PROP_PAGER_ROUTE,
                            "coffee://login?-P${PagerContainerActivity.PROP_PAGER_TITLE}=login page omg"
                        )
                    }
                    .build(),
                this
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == 2) {
            Log.i("BRouter", "从 Activity1 回来了")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            loginService.logout()
        }
        return super.onOptionsItemSelected(item)
    }
}