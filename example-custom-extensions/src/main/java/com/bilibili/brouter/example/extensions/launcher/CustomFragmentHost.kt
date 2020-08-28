package com.bilibili.brouter.example.extensions.launcher

import kotlin.reflect.KClass

/**
 * @author dieyi
 * Created at 2020/6/3.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomFragmentHost(
    val value: KClass<out SingleFragmentHost>
)

/**
 * 没啥用，就是标记一下，防止 CustomFragmentHost 里填的容器类不对
 */
interface SingleFragmentHost