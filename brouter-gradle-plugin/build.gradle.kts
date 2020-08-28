
plugins {
    kotlin("jvm")
    id("com.gradle.plugin-publish") version "0.12.0"
    `java-gradle-plugin`
}


val ANDROID_PLUGIN_VERSION: String by project
val KOTLIN_VERSION: String by project

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    compileOnly("com.android.tools.build:gradle:$ANDROID_PLUGIN_VERSION")
    compileOnly(kotlin("gradle-plugin"))
    implementation(project(":brouter-compile-common"))
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("BRouterPlugin") {
            id = "com.bilibili.brouter"
            displayName = "BRouter Plugin"
            description = "BRouter gradle plugin for Android"
            implementationClass = "com.bilibili.brouter.plugin.BRouterPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/bilibili/BRouter"
    vcsUrl = "https://github.com/bilibili/BRouter.git"
    tags = listOf("router", "android", "module", "inject", "ioc")
}
