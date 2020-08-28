package com.bilibili.brouter.example.extensions.container

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.bilibili.brouter.example.extensions.base.BaseActivity
import com.bilibili.brouter.example.extensions.launcher.SingleFragmentHost

/**
 * @author dieyi
 * Created at 2020/6/2.
 */
class SingleFragmentContainerActivity : BaseActivity(), SingleFragmentHost {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val className =
                intent.getStringExtra(FRAGMENT_CLASS) ?: error("No fragment class.")
            val arguments = intent.getBundleExtra(FRAGMENT_ARGUMENTS)


            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, Fragment.instantiate(this, className, arguments))
                .commit()

            val props = props
            val actionBar = supportActionBar!!
            if (props?.get(PROP_TOOLBAR_HIDE) == "true") {
                actionBar.hide()
            } else {
                props?.get(PROP_TOOLBAR_TITLE)?.let {
                    actionBar.title = it
                }
            }
        }
    }

    companion object {
        const val FRAGMENT_CLASS = "fragment_class"
        const val FRAGMENT_ARGUMENTS = "fragment_arguments"
        const val PROP_TOOLBAR_HIDE = "toolbar.hide"
        const val PROP_TOOLBAR_TITLE = "toolbar.title"

        fun createIntentForFragment(
            context: Context,
            clazz: Class<out Fragment>,
            host: Class<out SingleFragmentHost>,
            arguments: Bundle,
            extras: Bundle
        ): Intent {
            extras.putString(FRAGMENT_CLASS, clazz.name)
            extras.putBundle(FRAGMENT_ARGUMENTS, arguments)
            return Intent(context, host).apply {
                putExtras(extras)
            }
        }
    }
}