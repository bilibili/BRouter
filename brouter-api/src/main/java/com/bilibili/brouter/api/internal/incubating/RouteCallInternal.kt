package com.bilibili.brouter.api.internal.incubating

import com.bilibili.brouter.api.RouteCall
import com.bilibili.brouter.api.RouteListener


interface RouteCallInternal : RouteCall {

    val listener: RouteListener

    override fun clone(): RouteCallInternal
}