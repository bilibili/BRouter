
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    api("javax.inject:javax.inject:1@jar")
    implementation(project(":brouter-model"))
    implementation(project(":brouter-runtime-common"))
    compileOnly("com.google.android:android:4.1.1.4")
}