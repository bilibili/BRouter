package com.bilibili.brouter.plugin.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.bilibili.brouter.plugin.internal.tasks.CollectDependencyMeta
import com.bilibili.brouter.plugin.internal.tasks.GenerateModuleRegistry
import com.bilibili.brouter.plugin.internal.tasks.register
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

internal open class BaseConfigure<T : BaseExtension>(val target: Project) {

    open fun configure(ext: T) {
        with(ext) {
            this as TestedExtension
            unitTestVariants.configureEach {
                generateRegistryFor(it)
            }
            testVariants.configureEach {
                generateRegistryFor(it)
            }
        }
    }

    protected fun generateRegistryFor(variant: BaseVariant): FileCollection {
        val files = target.files()
        target.tasks.register(CollectDependencyMeta.ConfigAction(variant, files, target))
        target.tasks.register(GenerateModuleRegistry.ConfigAction(variant, files, target))
        return files
    }
}