package com.bilibili.brouter.example.extensions.base

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.bilibili.brouter.api.*


open class BaseActivity : AppCompatActivity() {

    val props: MultiMap? get() = intent.getParcelableExtra(CROUTER_PROPS)

    fun performForward() {
        intent.getParcelableExtra<RouteRequest>(CROUTER_FORWARD)
            ?.newCall(CallParams(context = this))?.execute()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = intent.getStringExtra("brouter.rule") ?: javaClass.simpleName
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}