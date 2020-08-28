package com.bilibili.brouter.example

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bilibili.brouter.api.BRouter
import com.bilibili.brouter.api.toRouteRequest

class EntranceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri == null) {
            Log.e("debug", "no uri")
        } else {
            val response = BRouter.routeTo(uri.toRouteRequest(), this)
            Log.e("debug", response.toString())
        }
        finish()
    }
}