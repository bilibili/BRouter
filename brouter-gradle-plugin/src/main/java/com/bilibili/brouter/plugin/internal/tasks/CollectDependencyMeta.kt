package com.bilibili.brouter.plugin.internal.tasks

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.bilibili.brouter.common.compile.META_PATH
import com.bilibili.brouter.common.compile.gson
import com.bilibili.brouter.common.compile.parse
import com.bilibili.brouter.common.meta.LibraryMeta
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File
import java.util.zip.ZipFile
import kotlin.streams.toList

@CacheableTask
open class CollectDependencyMeta : DefaultTask() {

    @get:OutputFile
    lateinit var outputFile: File
        private set

    @get:Classpath
    @get:InputFiles
    lateinit var inputRes: FileCollection
        private set


    @TaskAction
    fun doCollect() {
        val modules = inputRes.files
            .parallelStream()
            .map {
                if (!it.exists()) {
                    null
                } else if (it.name.endsWith(".jar")) {
                    ZipFile(it).use {
                        it.getEntry(META_PATH)?.let { e ->
                            gson.parse<LibraryMeta>(it.getInputStream(e).reader())
                        }
                    }
                } else {
                    val metaFile = File(it, META_PATH.replace('/', File.separatorChar))
                    if (metaFile.exists()) {
                        metaFile.reader().use {
                            gson.parse<LibraryMeta>(it)
                        }
                    } else {
                        null
                    }
                }
            }
            .filter {
                it != null
            }
            .toList() as List<LibraryMeta>

        outputFile
            .bufferedWriter().use {
                gson.toJson(modules, it)
            }
    }

    class ConfigAction(
        private val variant: BaseVariant,
        private val dependencyMetas: ConfigurableFileCollection,
        private val project: Project
    ) : TaskConfigureAction<CollectDependencyMeta> {


        private val outputFile: File
            get() = File(
                project.buildDir,
                "intermediates/brouter/${variant.name}/dependency_meta/output.json"
            )

        override val taskName: String
            get() = "collect${variant.name.capitalize()}BRouterMeta"

        override fun preConfigure(taskName: String) {
            super.preConfigure(taskName)
            dependencyMetas.builtBy(taskName)
                .from(outputFile)
        }

        override fun execute(t: CollectDependencyMeta) {
            t.outputFile = outputFile
            t.inputRes = project.files()
                .fromVariant(variant)
                .apply {
                    if (variant is TestVariant) {
                        fromVariant(variant.testedVariant)
                    }
                }
        }
    }
}


internal fun ConfigurableFileCollection.fromVariant(variant: BaseVariant): ConfigurableFileCollection {
    from(variant.runtimeConfiguration
        .incoming
        .artifactView {
            it.attributes {
                it.attribute(
                    AndroidArtifacts.ARTIFACT_TYPE,
                    AndroidArtifacts.ArtifactType.JAVA_RES.type
                )
            }
        }
        .files)
    return this
}