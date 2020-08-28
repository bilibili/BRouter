package com.bilibili.brouter.example.apt

/**
 * Like ARouter
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(
    val path: String,
    val group: String = "",
    val name: String = "",
    val extras: Int = 0
)