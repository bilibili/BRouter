package com.bilibili.lib.blrouter.plugin.internal.lib

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.bilibili.brouter.plugin.internal.tasks.TaskConfigureAction
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

class ConfigCompileAction(
    private val variant: BaseVariant,
    private val extension: LibraryExtension,
    private val sources: List<File>,
    private val classpath: Configuration,
    private val compileOut: ConfigurableFileCollection,
    project: Project
) : TaskConfigureAction<JavaCompile> {

    private val compileDir: File = File(project.buildDir, "intermediates/brouter/${variant.name}/api_javac")

    override val taskName: String get() = "compile${variant.name.capitalize()}BRouterApiWithJavac"

    override fun preConfigure(taskName: String) {
        compileOut.builtBy(taskName)
            .from(compileDir)
    }

    override fun execute(t: JavaCompile) {


        val compileOptions = extension.compileOptions
        t.classpath = classpath

        t.include("**/*.java")
        t.source(t.project.files(sources.map {
            t.project.fileTree(it)
        }).asFileTree)
        t.destinationDir = compileDir
        t.sourceCompatibility = compileOptions.sourceCompatibility.toString()
        t.targetCompatibility = compileOptions.targetCompatibility.toString()
        t.options.encoding = compileOptions.encoding
        t.options.isIncremental = compileOptions.incremental ?: true
    }
}


class ConfigJarAction(
    private val variant: BaseVariant,
    project: Project,
    private val source: FileCollection,
    private val jarOut: ConfigurableFileCollection
) : TaskConfigureAction<Jar> {

    private val jarFile =
        File(project.buildDir, "intermediates/brouter/${variant.name}/api_jar/$JAR_NAME")

    override val taskName: String
        get() = "bundle${variant.name.capitalize()}BRouterApiJar"

    override fun preConfigure(taskName: String) {
        jarOut.from(jarFile)
            .builtBy(taskName)
    }

    override fun execute(t: Jar) {
        t.from(source)
        t.destinationDirectory.set(jarFile.parentFile)
        t.archiveFileName.set(jarFile.name)
    }
}


internal fun String.serviceOnly(): String {
    return if (this == SourceSet.MAIN_SOURCE_SET_NAME) {
        "serviceOnly"
    } else {
        this + "ServiceOnly"
    }
}

internal fun String.serviceApi(): String {
    return if (this == SourceSet.MAIN_SOURCE_SET_NAME) {
        "serviceApi"
    } else {
        this + "ServiceApi"
    }
}

internal fun String.api(): String {
    return if (this == SourceSet.MAIN_SOURCE_SET_NAME) {
        "api"
    } else {
        this + "Api"
    }
}

internal val ATTR_API = Attribute.of("brouter.api", String::class.javaObjectType)


internal fun Configuration.attributeCopy(configuration: Configuration) {
    val myAttributes = this.attributes
    val otherAttributes = configuration.attributes
    for (key in otherAttributes.keySet()) {
        key as Attribute<Any>
        myAttributes.attribute(key, otherAttributes.getAttribute(key)!!)
    }
}

internal const val API = "api"
internal const val ALL = "all"
internal const val JAR_NAME = "router-api.jar"