package com.bilibili.brouter.buildsrc

import org.gradle.api.Project

private object Stub {
    val appcompat: Boolean = System.getProperty("brouter.appcompat") == "true"
    val brouterApi = ":brouter-api"
    val brouterApt = ":brouter-apt"
    val brouterCore = ":brouter-core"
}

val appcompat = Stub.appcompat

private fun String.fixAppcompat(): String {
    return if (!appcompat) {
        this
    } else {
        "$this-appcompat"
    }
}

val Project.brouterApi get() = project(Stub.brouterApi.fixAppcompat())
val Project.brouterApt get() = project(Stub.brouterApt.fixAppcompat())
val Project.brouterCore get() = project(Stub.brouterCore.fixAppcompat())
