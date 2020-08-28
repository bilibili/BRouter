package com.bilibili.brouter.plugin.internal.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.bilibili.brouter.common.compile.gson
import com.bilibili.brouter.common.compile.parse
import com.bilibili.brouter.common.meta.LibraryMeta
import com.bilibili.brouter.common.util.matcher.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
open class GenerateManifestForExportedActivity : DefaultTask() {

    @get:InputFiles
    lateinit var dependencyMetas: FileCollection
        private set

    @get:OutputFile
    lateinit var outputFile: File
        private set

    @get:Input
    lateinit var className: String
        private set


    @TaskAction
    fun generate() {
        val modules = dependencyMetas.singleFile.reader().use {
            gson.parse<List<LibraryMeta>>(it)
        }

        outputFile.bufferedWriter().use { writer ->
            writer.write(
                """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" 
    package="com.bilibili.brouter.generated">

    <application>
        <activity
            android:name="$className"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
"""
            )

            modules.parallelStream()
                .flatMap {
                    it.modules.stream()
                }
                .flatMap {
                    it.routes.stream()
                }
                .filter {
                    it.exported
                }
                .forEach { route ->
                    val parser = RawSegmentsParser.forceScheme()
                    val sb = StringBuilder(128)

                    route.routeRules.forEach { rule ->
                        parser.parse(rule).flatten().forEach { segments ->
                            val scheme = segments[0].let {
                                require(
                                    it is ExactSegment
                                ) {
                                    "Don't support export wildcard scheme '$rule'."
                                }
                                it.segment
                            }

                            val host = segments[1].let {
                                if (it is PrefixSegment) {
                                    ""
                                } else if (it is WildCardSegment) {
                                    require(it.prefix.isEmpty()) {
                                        "Only support host with leading '*' or '{any}', but is '$rule'."
                                    }
                                    "*${it.suffix}"
                                } else {
                                    it.segment
                                }
                            }
                            require(segments.size > 2 || segments[1] is PrefixSegment) {
                                "Exported rule without path '$rule'"
                            }
                            val path = if (segments.size == 2) {
                                require(segments[1] is PrefixSegment) {
                                    "Exported rule without path '$rule'"
                                }
                                ""
                            } else {
                                segments.subList(2, segments.size).joinToString(separator = "") {
                                    "/" + when (it) {
                                        is ExactSegment -> {
                                            it.segment
                                        }
                                        is WildCardSegment -> {
                                            it.prefix + ".*" + it.suffix
                                        }
                                        is PrefixSegment -> {
                                            ".*"
                                        }
                                        else -> throw AssertionError()
                                    }
                                }
                            }

                            sb.append(
                                "                <data" +
                                        "\n                    android:scheme=\"$scheme\""
                            )
                            if (host.isNotEmpty()) {
                                sb.append("\n                    android:host=\"$host\"")
                                if (path.isNotEmpty()) {
                                    val i = path.indexOf(".*")
                                    when {
                                        i < 0 -> {
                                            sb.append("\n                   android:path=\"$path\"")
                                        }
                                        i == path.length - 2 -> { // endsWith(".*")
                                            // /ss.* ->  /ss
                                            sb.append(
                                                "\n                   android:pathPrefix=\"${path.substring(
                                                    0,
                                                    i
                                                )}\""
                                            )
                                        }
                                        else -> {
                                            sb.append("\n                   android:pathPattern=\"$path\"")
                                        }
                                    }
                                }
                            }
                            sb.append(" />\n")
                        }
                    }
                    writer.write(sb.toString())
                }
            writer.write(
                "            </intent-filter>\n" +
                        "        </activity>\n" +
                        "    </application>\n" +
                        "</manifest>\n"
            )
        }
    }

    class ConfigAction(
        private val variant: ApplicationVariant,
        private val className: String,
        private val dependencyMetas: FileCollection,
        private val project: Project
    ) :
        TaskConfigureAction<GenerateManifestForExportedActivity> {

        private val outputFile: File
            get() = File(
                project.buildDir,
                "intermediates/brouter/${variant.name}/exported/AndroidManifest.xml"
            )

        override val taskName: String get() = "generateExported${variant.name.capitalize()}AndroidManifest"

        override fun preConfigure(taskName: String) {
            val outputManifest = project.files(outputFile)
            //  .builtBy(taskName)
            // 不能 builtBy(taskName) ，否则会形成循环依赖，我们直接在外面让处理 Manifest 任务依赖我们的任务

            // exported to runtime configuration
            variant.runtimeConfiguration
                .dependencies.add(
                project.dependencies.create(outputManifest)
            )
        }

        override fun execute(t: GenerateManifestForExportedActivity) {
            t.className = className
            t.outputFile = outputFile
            // 这里不能直接用 Meta 产物，因为 Javac 会依赖 Process Resource，所以无法支持 App 下导出 Manifest
            t.dependencyMetas = dependencyMetas
        }
    }
}