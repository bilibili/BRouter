import com.android.build.gradle.LibraryExtension
import com.bilibili.brouter.buildsrc.brouterApi

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

configure<LibraryExtension> {
    sourceSets["main"].apply {
        java.exclude("com/bilibili/brouter/core/internal/generated/BuiltInModules.java")
    }
    defaultConfig {
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
}

val KOTLIN_VERSION: String by project

dependencies {
    api(brouterApi)
    api("javax.inject:javax.inject:1@jar")
    implementation(project(":brouter-model"))
    implementation(project(":brouter-runtime-common"))
    implementation(kotlin("stdlib-jdk7", KOTLIN_VERSION))
    testImplementation("junit:junit:4.12")

    androidTestImplementation("com.android.support.test:runner:1.0.2")
}