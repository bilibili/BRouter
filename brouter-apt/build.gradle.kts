import com.bilibili.brouter.buildsrc.brouterApi

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    compileOnly("com.google.auto.service:auto-service:1.0-rc4")
    kapt("com.google.auto.service:auto-service:1.0-rc4")

    implementation(kotlin("stdlib-jdk7"))
    implementation("com.squareup.okio:okio:2.2.1")
    implementation("com.squareup:javapoet:1.9.0")
    implementation(brouterApi)
    implementation(project(":brouter-model"))
    implementation(project(":brouter-compile-common"))
}

kapt {
    arguments {
        arg("debug", true)
    }
}