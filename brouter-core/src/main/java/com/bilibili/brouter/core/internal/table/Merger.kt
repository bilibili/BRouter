package com.bilibili.brouter.core.internal.table

interface Merger<T> {

    fun merge(other: T)
}