package com.bilibili.brouter.example.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bilibili.brouter.api.Routes
import com.bilibili.brouter.example.R
import com.bilibili.brouter.example.extensions.base.BaseFragment

@Routes("coffee://example/frag1")
class Fragment1 : BaseFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.simple_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_text)
            .text = "Activity Extras: \n${activity?.intent?.extras?.apply { get("") }}\n" +
                "Arguments: ${arguments?.apply { get("") }}"
    }
}