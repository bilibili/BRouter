package com.bilibili.brouter.api

import android.os.Parcelable
import com.bilibili.brouter.api.internal.HasInternalProtocol

@HasInternalProtocol
interface MultiMap : Parcelable {
    val size: Int
    val isEmpty: Boolean
    val keySet: Set<String>
    operator fun get(key: String): String?
    fun getLast(key: String): String?
    fun contains(key: String): Boolean
    fun getAll(key: String): List<String>?
}

@HasInternalProtocol
interface MutableMultiMap : MultiMap {

    override val keySet: MutableSet<String>

    fun clear()
    fun append(key: String, value: String): MutableMultiMap
    fun appendAll(key: String, values: Collection<String>): MutableMultiMap
    fun put(key: String, value: String): MutableList<String>?
    fun putAll(key: String, values: Collection<String>): MutableList<String>?
    fun remove(key: String): MutableList<String>?
}

operator fun MutableMultiMap.set(key: String, value: String) {
    put(key, value)
}