ext {
    this["compileVersion"] = 29
    this["minVersion"] = 16
    this["targetVersion"] = 29
    this["buildTools"] = "29.0.1"

    this["tests"] = HashMap<String, Any>().apply {
        this["support-runner"] = "com.android.support.test:runner:1.0.2"
    }

    this["appcompat"] = "com.android.support:appcompat-v7:28.0.0"
    this["androidx"] = "androidx.appcompat:appcompat:1.0.2"
    this["gson"] = "com.google.code.gson:gson:2.8.5"
}

val commonConfigure = project.file(".scripts/common_configure.gradle.kts")
val publishProject = project.file(".scripts/publish.gradle.kts")

val localMode = project.findProperty("brouter.localMode") == "true"


subprojects {
    repositories {
        if (localMode) {
            mavenLocal()
        }
        google()
        jcenter()
    }

    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
    }
    apply(from = commonConfigure)
    if (!name.contains("-stub-") && name.startsWith("brouter-")) {
        apply(from = publishProject)
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

tasks.register("publishAll") {
    val appcompat = System.getProperty("brouter.appcompat") == "true"
    val compats = setOf("brouter-api-appcompat", "brouter-apt-appcompat", "brouter-core-appcompat")
    group = "publishing"
    subprojects {
        if (name.startsWith("brouter-") && !name.contains("stub")) {
            if (localMode) {
                dependsOn(tasks.named("publishBintrayPublicationToMavenLocal"))
            } else {
                if (!appcompat || name in compats) {
                    dependsOn(tasks.named("bintrayUpload"))
                }
                if (!appcompat && name == "brouter-gradle-plugin") {
                    dependsOn(tasks.named("publishPlugins"))
                }
            }
        }
    }
}