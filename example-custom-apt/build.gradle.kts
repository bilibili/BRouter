import com.bilibili.brouter.buildsrc.brouterApt

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    compileOnly("com.google.auto.service:auto-service:1.0-rc4")
    kapt("com.google.auto.service:auto-service:1.0-rc4")
    implementation(kotlin("stdlib-jdk7"))

    implementation(brouterApt)
}
kapt {
    includeCompileClasspath = false
}