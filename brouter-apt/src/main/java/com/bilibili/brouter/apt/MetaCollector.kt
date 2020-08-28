package com.bilibili.brouter.apt

import com.bilibili.brouter.api.BootStrapMode
import com.bilibili.brouter.api.task.ThreadMode
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

interface MetaCollector {

    fun addModule(configure: (ModuleMetaBuilder) -> Unit)

    fun addRoute(configure: (RouteMetaBuilder) -> Unit)

    fun addService(configure: (ServiceMetaBuilder) -> Unit)

    fun addConsumer(configure: (ConsumerClassBuilder) -> Unit)

    fun addTask(configure: (TaskMetaBuilder) -> Unit)
}

interface MetaProcessor {

    val supportedAnnotations: Set<String>

    fun process(
        annotations: Set<TypeElement>,
        roundEnv: RoundEnvironment,
        processingEnv: ProcessingEnvironment,
        collector: MetaCollector
    )
}

interface RouteMetaBuilder {
    fun routeName(routeName: String): RouteMetaBuilder
    fun moduleName(moduleName: String): RouteMetaBuilder
    fun routeRules(routeRules: List<String>): RouteMetaBuilder
    fun routeType(routeType: String): RouteMetaBuilder
    fun attributes(attributes: List<Pair<String, String>>): RouteMetaBuilder
    fun interceptors(interceptors: List<String>): RouteMetaBuilder
    fun launcher(launcher: String): RouteMetaBuilder
    fun desc(desc: String): RouteMetaBuilder
    fun className(className: String): RouteMetaBuilder
    fun exported(exported: Boolean): RouteMetaBuilder
}

interface ServiceMetaBuilder {
    fun serviceName(serviceName: String): ServiceMetaBuilder
    fun moduleName(moduleName: String): ServiceMetaBuilder
    fun returnType(returnType: String): ServiceMetaBuilder
    fun sourceClassName(sourceClassName: String): ServiceMetaBuilder
    fun sourceMethodName(sourceMethodName: String): ServiceMetaBuilder
    fun serviceTypes(serviceTypes: List<String>): ServiceMetaBuilder
    fun singleton(singleton: Boolean): ServiceMetaBuilder
    fun desc(desc: String): ServiceMetaBuilder
    fun addMethodParam(
        className: String,
        serviceName: String,
        type: DependencyType,
        optional: Boolean
    ): ServiceMetaBuilder

    fun taskDependencies(taskDependencies: List<String>): ServiceMetaBuilder
}

enum class DependencyType {
    VALUE,
    PROVIDER,
    WILDCARD_PROVIDER
}

interface ModuleMetaBuilder {
    fun moduleName(moduleName: String): ModuleMetaBuilder
    fun defaultInLibrary(defaultInLibrary: Boolean): ModuleMetaBuilder
    fun activatorClass(activatorClass: String): ModuleMetaBuilder
    fun bootStrapMode(bootStrapMode: BootStrapMode): ModuleMetaBuilder
    fun desc(desc: String): ModuleMetaBuilder
    fun attributes(attributes: List<Pair<String, String>>): ModuleMetaBuilder
    fun onCreate(configure: (TaskMetaBuilder) -> Unit): ModuleMetaBuilder
    fun onPostCreate(configure: (TaskMetaBuilder) -> Unit): ModuleMetaBuilder
}

interface ConsumerClassBuilder {
    fun consumerClassName(consumerClassName: String): ConsumerClassBuilder
    fun superConsumerClassName(superConsumerClassName: String): ConsumerClassBuilder
    fun addConsumerDetail(
        className: String,
        serviceName: String,
        type: DependencyType,
        optional: Boolean,
        fieldOrMethodName: String,
        isField: Boolean
    ): ConsumerClassBuilder
}

interface TaskMetaBuilder {
    fun taskName(taskName: String): TaskMetaBuilder
    fun moduleName(moduleName: String): TaskMetaBuilder
    fun priority(priority: Int): TaskMetaBuilder
    fun threadMode(threadMode: ThreadMode): TaskMetaBuilder
    fun className(className: String): TaskMetaBuilder
    fun addConstructorParam(
        className: String,
        serviceName: String,
        type: DependencyType,
        optional: Boolean
    ): TaskMetaBuilder

    fun taskDependencies(taskDependencies: List<String>): TaskMetaBuilder
    fun addProducedService(
        serviceName: String,
        returnType: String,
        serviceTypes: List<String>,
        desc: String,
        fieldOrMethodName: String,
        isField: Boolean
    ): TaskMetaBuilder
}