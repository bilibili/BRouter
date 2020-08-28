plugins {
    kotlin("jvm")
}


val gson: String by project

dependencies {
    compileOnly(kotlin("stdlib-jdk7"))
    compile(gson)
}