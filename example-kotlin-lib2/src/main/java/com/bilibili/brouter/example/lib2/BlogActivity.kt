package com.bilibili.brouter.example.lib2

import android.os.Bundle
import android.widget.TextView
import com.bilibili.brouter.api.Routes
import com.bilibili.brouter.example.extensions.base.BaseActivity

@Routes(
    "(?<scheme>http|https)://dieyidezui.com",
    "(?<scheme>http|https)://dieyidezui.com/**"
)
class BlogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_page)
        findViewById<TextView>(R.id.tv_text)
            .text = "Hi, 这是 dieyi 的博客\n" +
                "Extras: \n${intent.extras?.apply { get("") }}"

    }
}