package com.bilibili.brouter.example.pages

import android.os.Bundle
import android.widget.TextView
import com.bilibili.brouter.example.R
import com.bilibili.brouter.example.apt.Route
import com.bilibili.brouter.example.extensions.base.BaseActivity

@Route("coffee://example/ac2")
class Activity2 : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_page)
        findViewById<TextView>(R.id.tv_text)
            .text = "Extras: \n${intent.extras?.apply { get("") }}"
    }
}