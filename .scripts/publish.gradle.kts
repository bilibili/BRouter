import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

apply<MavenPublishPlugin>()
apply(plugin = "com.jfrog.bintray")

afterEvaluate {
    val isAndroid = plugins.hasPlugin("com.android.library")

    val jar = tasks.register("sourceJar", Jar::class.java) {
        archiveClassifier.set("sources")
        if (!isAndroid) {
            from(project.the<SourceSetContainer>()["main"].allSource)
        } else {
            val sourceSet = project.the<LibraryExtension>().sourceSets["main"]
            from(sourceSet.java.sourceFiles)
            if (plugins.hasPlugin("kotlin-android")) {
                from(sourceSet.withConvention(KotlinSourceSet::class) { kotlin })
            }
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("bintray") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                artifact(jar.flatMap {
                    it.archiveFile
                }) {
                    extension = "jar"
                    classifier = "sources"
                    builtBy(jar)
                }
                if (!isAndroid) {
                    from(components["java"])
                } else {
                    the<LibraryExtension>().libraryVariants
                        .configureEach {
                            if (name == "release") {
                                artifact(packageLibraryProvider.flatMap {
                                    it.archiveFile
                                }) {
                                    extension = "aar"
                                    builtBy(packageLibraryProvider)
                                }
                                pom {
                                    withXml {
                                        val deps =
                                            runtimeConfiguration.allDependencies.filterIsInstance<ModuleDependency>()
                                        if (deps.isNotEmpty()) {
                                            asNode().appendNode("dependencies").apply {
                                                deps.forEach {
                                                    val dep = appendNode("dependency")
                                                    dep.appendNode("groupId", it.group)
                                                    dep.appendNode("artifactId", it.name)
                                                    dep.appendNode("version", it.version)
                                                    dep.appendNode("scope", "compile")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }

        repositories {
            mavenLocal()
        }
    }
}

configure<com.jfrog.bintray.gradle.BintrayExtension> {
    user = project.findProperty("bintray.user").toString()
    key = project.findProperty("bintray.key").toString()
    setPublications("bintray")
    pkg.apply {
        repo = project.findProperty("bintray.repo").toString()
        name = project.name
        userOrg = project.findProperty("bintray.org").toString()
        setLicenses("Apache-2.0")
        websiteUrl = "https://github.com/bilibili/BRouter"
        vcsUrl = "https://github.com/bilibili/BRouter.git"
    }
}