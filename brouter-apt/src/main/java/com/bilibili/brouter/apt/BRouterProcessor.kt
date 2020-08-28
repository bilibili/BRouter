package com.bilibili.brouter.apt

import com.bilibili.brouter.apt.internal.MetaCollectorImpl
import com.google.auto.service.AutoService
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class BRouterProcessor : AbstractProcessor() {

    private lateinit var metaProcessors: List<MetaProcessor>
    private val metaCollector = MetaCollectorImpl()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        metaProcessors =
            ServiceLoader.load(MetaProcessor::class.java, MetaProcessor::class.java.classLoader)
                .toList()
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        metaProcessors.flatMapTo(hashSetOf()) {
            it.supportedAnnotations
        }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (!roundEnv.processingOver()) {
            metaProcessors.forEach {
                it.process(annotations, roundEnv, processingEnv, metaCollector)
            }
        } else {
            metaCollector.done(processingEnv)
        }
        return true
    }
}







