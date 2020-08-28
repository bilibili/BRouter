package com.bilibili.brouter.plugin.internal.app

import com.bilibili.brouter.common.compile.META_PATH
import com.bilibili.brouter.plugin.internal.BaseConfigure
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.bilibili.brouter.plugin.internal.DefaultBRouterAppExtension
import com.bilibili.brouter.plugin.internal.dependency.XmlToManifestTransform
import com.bilibili.brouter.plugin.internal.tasks.GenerateManifestForExportedActivity
import com.bilibili.brouter.plugin.internal.tasks.register
import org.gradle.api.Project


internal class ApplicationConfigure(target: Project,
                                    private val routerExt: DefaultBRouterAppExtension
) : BaseConfigure<AppExtension>(target) {

    override fun configure(ext: AppExtension) {
        super.configure(ext)

        ext.packagingOptions {
            it.exclude(META_PATH)
        }

        target.afterEvaluate {
            val className = routerExt.exportedClassName

            ext.applicationVariants.configureEach {
                val dependencyMetas = generateRegistryFor(it)
                if (className != null) {
                    val taskProvider = target.tasks.register(
                        GenerateManifestForExportedActivity.ConfigAction(
                            it,
                            className,
                            dependencyMetas,
                            target
                        )
                    )
                    it.outputs.configureEach {
                        it.processManifestProvider.configure {
                            it.dependsOn(taskProvider)
                        }
                    }
                }
            }

            // convert xml -> manifest
            if (className != null) {
                require(!className.startsWith(".")) {
                    "Illegal class name should not start with '.', but is $className"
                }

                target.dependencies.registerTransform(XmlToManifestTransform::class.java) {
                    it.from.attribute(AndroidArtifacts.ARTIFACT_TYPE, "xml")
                    it.to.attribute(
                        AndroidArtifacts.ARTIFACT_TYPE,
                        AndroidArtifacts.ArtifactType.MANIFEST.type
                    )
                }
            }
        }
    }
}