package com.bilibili.brouter.example.apt

import com.bilibili.brouter.apt.MetaCollector
import com.bilibili.brouter.apt.MetaProcessor
import com.bilibili.brouter.apt.javaClassName
import com.google.auto.service.AutoService
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

@AutoService(MetaProcessor::class)
class CompatRouteProcessor : MetaProcessor {
    override val supportedAnnotations: Set<String>
        get() = setOf(Route::class.java.name)

    override fun process(
        annotations: Set<TypeElement>,
        roundEnv: RoundEnvironment,
        processingEnv: ProcessingEnvironment,
        collector: MetaCollector
    ) {
        roundEnv.getElementsAnnotatedWith(Route::class.java).forEach { element ->
            element as TypeElement
            collector.addRoute { builder ->
                val route = element.getAnnotation(Route::class.java)
                builder.className(element.javaClassName)
                    .routeRules(listOf(route.path.also {
                        require(it.isNotEmpty()) {
                            "Path is empty."
                        }
                    }))

                route.name.let {
                    if (it.isNotEmpty()) {
                        builder.routeName(it)
                    }
                }

                builder.attributes(
                    listOf(
                        "group" to route.group,
                        "extras" to route.extras.toString()
                    )
                )
            }
        }
    }
}