package com.bilibili.brouter.example.lib2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.bilibili.brouter.api.BRouter.inject
import com.bilibili.brouter.example.apt.Route
import com.bilibili.brouter.example.extensions.base.BaseFragment
import javax.inject.Inject

@Route("coffee://teenager")
class TeenagerFragment : BaseFragment() {
    @Inject
    lateinit var service: TeenagerService

    private lateinit var tv: TextView
    private lateinit var btn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_teenager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inject(this)
        tv = view.findViewById(R.id.btn_teenager_status)
        btn = view.findViewById(R.id.btn_teenager_enable)
        btn.setOnClickListener {
            if (service.isEnabled) {
                service.disable()
            } else {
                service.enable()
            }
            refresh()
        }
        refresh()
    }

    private fun refresh() {
        tv.text = if (service.isEnabled) "青少年模式已开启" else "青少年模式未开启"
        btn.text = if (service.isEnabled) "点击关闭青少年模式" else "点击打开青少年模式"
    }
}