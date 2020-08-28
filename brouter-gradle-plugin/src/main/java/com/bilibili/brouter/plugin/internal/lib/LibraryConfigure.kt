package com.bilibili.brouter.plugin.internal.lib

import com.android.build.gradle.LibraryExtension
import com.bilibili.brouter.plugin.internal.BaseConfigure
import com.bilibili.brouter.plugin.internal.tasks.register
import com.bilibili.lib.blrouter.plugin.internal.lib.*
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency


internal class LibraryConfigure(target: Project) : BaseConfigure<LibraryExtension>(target) {

    override fun configure(ext: LibraryExtension) {
        super.configure(ext)

        with(target.dependencies) {
            attributesSchema {
                it.attribute(ATTR_API).disambiguationRules.add(AllDefault::class.java)
            }
            registerTransform(ApiTransform::class.java) {
                it.from.attribute(ATTR_API, ALL)
                it.to.attribute(ATTR_API, API)
            }
        }

        ext.sourceSets.configureEach { sourceSet ->
            target.configurations.create(sourceSet.name.serviceOnly()) {
                it.isCanBeConsumed = false
                it.isCanBeResolved = false
                it.isVisible = false
                it.description =
                    "CompileOnly BRouter Service for SourceSet '${sourceSet.name}'."
                it.dependencies.configureEach {
                    if (it is ModuleDependency) {
                        it.attributes {
                            it.attribute(
                                ATTR_API,
                                API
                            )
                        }
                    }
                }
            }

            target.configurations.create(sourceSet.name.serviceApi()) {
                it.isCanBeConsumed = false
                it.isCanBeResolved = false
                it.isVisible = false
                it.description =
                    "API to compile BRouter Service for SourceSet '${sourceSet.name}'."
            }
        }


        ext.libraryVariants.configureEach { variant ->
            val compileClasspath = variant.compileConfiguration
            val apiElements = target.configurations.getByName("${variant.name}ApiElements")
            val routerApiElements =
                target.configurations.create("${variant.name}BRouterApiElements") {
                    it.isVisible = true
                    it.isCanBeResolved = false
                    it.isCanBeConsumed = true
                    it.attributeCopy(apiElements)
                    it.attributes
                        .attribute(ATTR_API, API)
                    apiElements.attributes
                        .attribute(ATTR_API, ALL)
                }

            val routerApiClasspath =
                target.configurations.create("${variant.name}BRouterApiClasspath") {
                    it.isVisible = true
                    it.isCanBeResolved = true
                    it.isCanBeConsumed = false
                    it.attributeCopy(apiElements)
                    it.attributes
                        .attribute(ATTR_API, API)
                }


            val apis = variant.sourceSets.map {

                compileClasspath.extendsFrom(target.configurations.getByName(it.name.serviceOnly()))

                val serviceApi = target.configurations.getByName(it.name.serviceApi())
                // every serviceApi should expose to routerApiClasspath for compile api
                routerApiClasspath.extendsFrom(serviceApi)

                // every serviceApi should expose to api
                target.configurations.getByName(it.name.api())
                    .extendsFrom(serviceApi)


                target.file("src/${it.name}/api").apply {
                    // 借用 Android 的 API，会作为 generated 类型被展示
                    variant.addJavaSourceFoldersToModel(this)
                }
            }


            val compileOut = target.files()
            val jarOut = target.files()

            // compile api classes
            target.tasks.register(
                ConfigCompileAction(
                    variant,
                    ext,
                    apis,
                    routerApiClasspath,
                    compileOut,
                    target
                )
            )

            // jar api
            target.tasks.register(ConfigJarAction(variant, target, compileOut, jarOut))

            // self usage
            compileClasspath.dependencies.add(target.dependencies.create(jarOut))

            // runtime usage, for bundle aar
            variant.runtimeConfiguration.dependencies.add(target.dependencies.create(jarOut))

            // to Android
            apiElements.dependencies.add(target.dependencies.create(jarOut))
            // to Router
            routerApiElements.extendsFrom(routerApiClasspath)


            // publish jar
            routerApiElements.outgoing
                // no variants, artifact only
                .artifact(target.provider {
                    jarOut.singleFile
                }) {
                    it.builtBy(jarOut)
                }

        }
    }
}