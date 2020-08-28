package com.bilibili.brouter.plugin

import com.bilibili.brouter.plugin.internal.app.ApplicationConfigure
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.bilibili.brouter.plugin.internal.DefaultBRouterAppExtension
import com.bilibili.brouter.plugin.internal.lib.LibraryConfigure
import org.gradle.api.Plugin
import org.gradle.api.Project

class BRouterPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.withType(BasePlugin::class.java) {
            with(it.extension) {
                if (this is LibraryExtension) {
                    LibraryConfigure(target).configure(this)
                } else if (this is AppExtension) {
                    val ext = target.extensions.create(BRouterAppExtension::class.java, "brouter", DefaultBRouterAppExtension::class.java) as DefaultBRouterAppExtension
                    ApplicationConfigure(target, ext).configure(this)
                }
            }
        }
    }
}

