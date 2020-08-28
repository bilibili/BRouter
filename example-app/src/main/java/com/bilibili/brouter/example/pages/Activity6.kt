package com.bilibili.brouter.example.pages

import android.os.Bundle
import android.widget.TextView
import com.bilibili.brouter.api.Routes
import com.bilibili.brouter.example.R
import com.bilibili.brouter.example.extensions.base.BaseActivity

/**
 * @author dieyi
 * Created at 2020/6/9.
 */
@Routes(
    "(?<scheme>https|http)://github.com/{org_name}/{project_name}"
)
class Activity6 : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_page)
        findViewById<TextView>(R.id.tv_text)
            .text = "Extras: \n${intent.extras?.apply { get("") }}"
    }
}