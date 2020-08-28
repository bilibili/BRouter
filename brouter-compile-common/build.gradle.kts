
plugins {
    kotlin("jvm")
}

val gson: String by project
dependencies {
    implementation(kotlin("stdlib-jdk7"))
    implementation(project(":brouter-model"))
    api(project(":brouter-runtime-common"))
    implementation(gson)
}