package com.bilibili.brouter.example.extensions.base

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    fun performForward() {
        (activity as BaseActivity).performForward()
    }
}