package com.bilibili.brouter.plugin.internal

import com.bilibili.brouter.plugin.BRouterAppExtension

open class DefaultBRouterAppExtension : BRouterAppExtension {
    var exportedClassName: String? = null

    override fun exportedActivityClass(className: String) {
        this.exportedClassName = className
    }
}