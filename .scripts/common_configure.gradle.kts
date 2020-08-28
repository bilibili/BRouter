import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.AndroidBasePlugin

val appcompat = System.getProperty("brouter.appcompat") == "true"
val stub = project.dependencies.create(
    if (!appcompat) {
        project(":brouter-stub-androidx")
    } else {
        project(":brouter-stub-appcompat")
    }
)

val actual = project.dependencies.create(
    if (!appcompat) {
        val androidx: String by project
        androidx
    } else {
        val appcompat: String by project
        appcompat
    }
)
val isPublishLibrary = project.name.startsWith("brouter-")
val isStubLibrary = project.name.startsWith("brouter-stub-")

if (!isStubLibrary && isPublishLibrary) {
    project.configurations.configureEach {
        if (name == "compileOnly" || name == "provided") {
            dependencies.add(stub)
        }
        if (name == "androidTestImplementation") {
            dependencies.add(actual)
        }
    }
}

val VERSION_NAME: String by project
val GROUP: String by project
project.version = VERSION_NAME
project.group = GROUP

project.plugins.withType(AndroidBasePlugin::class.java) {
    afterEvaluate {
        configure<BaseExtension> {
            val compileVersion: Int by project
            val buildTools: String by project

            compileSdkVersion(compileVersion)
            buildToolsVersion(buildTools)

            defaultConfig {
                val minVersion: Int by project
                val targetVersion: Int by project
                minSdkVersion(minVersion)
                targetSdkVersion(targetVersion)
                versionName = VERSION_NAME
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            if (isPublishLibrary && this is LibraryExtension) {
                defaultConfig.consumerProguardFile("proguard-rules.pro")
            }
        }

        if (!isPublishLibrary) {
            dependencies.add("implementation", actual)
        }
    }
}

project.plugins.withType(JavaPlugin::class.java) {


    configure<JavaPluginConvention> {
        this.targetCompatibility = JavaVersion.VERSION_1_8
        this.sourceCompatibility = JavaVersion.VERSION_1_8
    }
}