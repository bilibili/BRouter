package com.bilibili.brouter.common.compile

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.Reader

const val META_PATH = "META-INF/brouter/library_meta.json"
const val GENERATED_PACKAGE = "com.bilibili.brouter.core.internal.generated"

val gson = GsonBuilder()
    .disableHtmlEscaping().setPrettyPrinting().create()

inline fun <reified T> Gson.parse(reader: Reader): T {
    return fromJson(reader, object : TypeToken<T>() {}.type)
}