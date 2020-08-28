package com.bilibili.brouter.api

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import com.bilibili.brouter.api.internal.DefaultRouteRequest

interface RouteRequest : Parcelable, HasAttributes {
    /**
     * 不包含 -A -B 的 Uri
     */
    val pureUri: Uri

    /**
     * 转成的统一路由协议的 Uri
     */
    val uniformUri: Uri

    /**
     * 不包含 Query 的 Uri
     */
    val targetUri: Uri

    val flags: Int
    val requestCode: Int
    val data: Uri?
    val routeTypes: List<String>

    /**
     * 非路由协议中的 Query
     */
    val params: MultiMap

    /**
     * -B 非保留的 Query
     */
    val props: MultiMap

    /**
     * 前置路由请求
     */
    val prev: RouteRequest?

    /**
     * 转发路由请求
     */
    val forward: RouteRequest?

    /**
     * 以下 4 个字段均不包含在路由协议内
     *
     * 慎用，唯一不是 Immutable 的。
     * 启动页面时的额外参数
     */
    val extras: Bundle?

    /**
     * startActivity 中的 options 参数
     */
    val options: Bundle?

    /**
     * Activity.overridePendingTransition
     */
    val animIn: Int
    val animOut: Int

    fun newBuilder(): Builder

    fun toPrettyString(): String

    interface Builder : HasConfigurableAttributes<Builder> {

        fun targetUri(uri: Uri): Builder
        fun requestCode(requestCode: Int): Builder
        fun addFlag(flags: Int): Builder
        fun flags(flags: Int): Builder
        fun data(data: Uri?): Builder
        fun routeTypes(types: List<String>): Builder
        fun routeTypes(vararg type: String): Builder
        val params: MutableMultiMap
        fun params(configure: (MutableMultiMap) -> Unit): Builder
        val props: MutableMultiMap
        fun props(configure: (MutableMultiMap) -> Unit): Builder
        fun prev(prev: RouteRequest?): Builder
        fun forward(forward: RouteRequest?): Builder

        fun extras(configure: (Bundle) -> Unit): Builder
        fun extras(extras: Bundle?): Builder
        fun options(configure: (Bundle) -> Unit): Builder
        fun options(options: Bundle?): Builder
        fun overridePendingTransition(animIn: Int = 0, animOut: Int = 0): Builder

        fun build(): RouteRequest
    }
}

fun String.toRouteRequest() = Uri.parse(this).toRouteRequest()
fun String.toBuilder(): RouteRequest.Builder = DefaultRouteRequest.Builder(Uri.parse(this))
fun Uri.toRouteRequest(): RouteRequest = DefaultRouteRequest(this)
fun Uri.toBuilder(): RouteRequest.Builder = DefaultRouteRequest.Builder(this)

@JvmOverloads
fun RouteRequest.newCall(context: CallParams = CallParams()): RouteCall =
    BRouter.newCall(this, context)
