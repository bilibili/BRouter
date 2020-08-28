plugins {
    `kotlin-dsl`
}

repositories {
    google()
    jcenter()
}
configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

val KOTLIN_VERSION: String by project
val ANDROID_PLUGIN_VERSION: String by project
val VERSION_NAME: String by project

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:$ANDROID_PLUGIN_VERSION")
    implementation(kotlin("gradle-plugin", KOTLIN_VERSION))
    implementation("com.bilibili.android:brouter-gradle-plugin:$VERSION_NAME")
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.+")
}