package com.bilibili.brouter.plugin.internal.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.bilibili.brouter.common.analysis.Analyzer
import com.bilibili.brouter.common.compile.META_PATH
import com.bilibili.brouter.common.compile.gson
import com.bilibili.brouter.common.compile.parse
import com.bilibili.brouter.common.meta.LibraryMeta
import com.bilibili.brouter.common.meta.ModuleMeta
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@CacheableTask
open class GenerateModuleRegistry : DefaultTask() {

    @get:OutputDirectory
    lateinit var classesDir: File
        private set

    @get:OutputFile
    lateinit var fullMeta: File
        private set

    @get:InputFile
    lateinit var selfMeta: Provider<File>
        private set

    @get:InputFiles
    lateinit var dependencyMetas: FileCollection
        private set


    @get:Input
    var needCheck = false
        private set

    @TaskAction
    open fun gen() {
        classesDir.deleteRecursively()
        Files.createDirectories(Paths.get(classesDir.toURI()))

        var libs = dependencyMetas.singleFile.reader().use {
            gson.parse<List<LibraryMeta>>(it)
        }

        val selfMetaFile = selfMeta.get()

        if (selfMetaFile.exists()) {
            libs = libs + selfMetaFile.reader().use {
                gson.parse<LibraryMeta>(it).apply {
                    if (needCheck) checkExported(this)
                }
            }
        }

        Analyzer(libs, false).analyze()

        generateRegistry(classesDir, libs.flatMap {
            it.modules
        })

        fullMeta.parentFile.mkdirs()
        fullMeta.writer().use {
            gson.toJson(libs, it)
        }
    }

    private fun checkExported(self: LibraryMeta) {
        self.modules
            .forEach { meta ->
                meta.routes.forEach {
                    require(!it.exported) {
                        "Routes in application can't export to Manifest, ${meta}."
                    }
                }
            }
    }

    class ConfigAction(
        private val variant: BaseVariant,
        private val dependencyMetas: FileCollection,
        private val project: Project
    ) : TaskConfigureAction<GenerateModuleRegistry> {

        private lateinit var postJavac: ConfigurableFileCollection

        private val outputClassDir: File
            get() = File(
                project.buildDir,
                "intermediates/brouter/${variant.name}/registry"
            )

        override val taskName: String
            get() = "generate${variant.name.capitalize()}BRouterModuleRegistry"

        override fun preConfigure(taskName: String) {
            postJavac = project.files(
                outputClassDir
            ).builtBy(taskName)
            variant.registerPostJavacGeneratedBytecode(postJavac)
        }

        override fun execute(t: GenerateModuleRegistry) {
            t.dependencyMetas = dependencyMetas
            t.classesDir = outputClassDir
            t.fullMeta = File(
                project.buildDir,
                "outputs/brouter/${variant.name}/meta.json"
            )

            t.dependsOn(variant.javaCompileProvider)

            // Optimize later.
            t.needCheck = variant is ApplicationVariant
            t.selfMeta = if (project.plugins.hasPlugin("kotlin-kapt"))
                project.provider {
                    File(
                        Kapt3GradleSubplugin.getKaptGeneratedClassesDir(project, variant.name),
                        META_PATH
                    )
                }
            else
                variant.javaCompileProvider.map {
                    File(it.destinationDir, META_PATH)
                }
        }
    }
}


private const val MODULE_CLASS = "com.bilibili.brouter.core.internal.generated.BuiltInModules"

private fun String.toInternalName() = replace('.', '/')

internal fun generateRegistry(classRoot: File, modules: List<ModuleMeta>) {
    val target = File(classRoot, MODULE_CLASS.toInternalName() + ".class")
    target.ensureParentDirsCreated()
    val cw = ClassWriter(0)
    cw.visit(
        Opcodes.V1_7,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_SUPER,
        MODULE_CLASS.toInternalName(),
        null,
        Type.getInternalName(Object::class.java),
        null
    )
    with(cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null)) {
        visitCode()
        visitVarInsn(Opcodes.ALOAD, 0)
        visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            Type.getInternalName(Object::class.java),
            "<init>",
            "()V",
            false
        )
        visitInsn(Opcodes.RETURN)
        visitMaxs(1, 1)
        visitEnd()
    }

    with(
        cw.visitMethod(
            Opcodes.ACC_STATIC or Opcodes.ACC_PUBLIC,
            "modules",
            "()Ljava/util/List;",
            null,
            null
        )
    ) {
        visitCode()
        visitTypeInsn(Opcodes.NEW, "java/util/ArrayList")
        visitInsn(Opcodes.DUP)
        visitLdcInsn(modules.size)
        visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false)
        for (m in modules) {
            visitInsn(Opcodes.DUP)
            visitTypeInsn(Opcodes.NEW, m.entranceClass.toInternalName())
            visitInsn(Opcodes.DUP)
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                m.entranceClass.toInternalName(),
                "<init>",
                "()V",
                false
            )
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/util/ArrayList",
                "add",
                "(Ljava/lang/Object;)Z",
                false
            )
            visitInsn(Opcodes.POP)
        }
        visitInsn(Opcodes.ARETURN)
        visitMaxs(4, 0)
        visitEnd()
    }
    cw.visitEnd()
    target.writeBytes(cw.toByteArray())
}