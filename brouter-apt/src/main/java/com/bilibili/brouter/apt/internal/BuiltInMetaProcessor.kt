package com.bilibili.brouter.apt.internal

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.task.TaskAction
import com.bilibili.brouter.api.task.TaskOptions
import com.bilibili.brouter.api.task.TaskOutput
import com.bilibili.brouter.apt.*
import com.bilibili.brouter.common.util.matcher.RawSegmentsParser
import com.google.auto.service.AutoService
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.*
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.WildcardType

@AutoService(MetaProcessor::class)
class BuiltInMetaProcessor : MetaProcessor {
    override val supportedAnnotations: Set<String>
        get() = setOf(
            Routes::class.java.name,
            Services::class.java.name,
            ModuleOptions::class.java.name,
            Inject::class.java.name
        )


    private val saved = mutableSetOf<String>()
    private val parser = RawSegmentsParser("brouter")

    override fun process(
        annotations: Set<TypeElement>,
        roundEnv: RoundEnvironment,
        processingEnv: ProcessingEnvironment,
        collector: MetaCollector
    ) {
        roundEnv.getElementsAnnotatedWith(Routes::class.java).forEach { e ->
            if (!e.kind.isClass && !e.kind.isInterface) {
                error("Unexpected @Routes on ${e.asType()} ${e.kind}")
            }
            e.requireModifier(setOf(Modifier.PUBLIC))
            e as TypeElement

            val r = e.getAnnotation(Routes::class.java)
            if (r.value.isEmpty()) {
                error("${e.asType()} @Routes must have at least one route.")
            } else {
                r.value.forEach {
                    // pre check the route uri
                    parser.parse(it)
                }
            }
            collector.addRoute { builder ->
                r.routeName.let {
                    if (it.isNotEmpty()) {
                        builder.routeName(it)
                    }
                }
                builder.routeType(r.routeType)
                    .routeRules(r.value.asList())
                    .attributes(e.parseAttributes())
                    .interceptors(r.parseInterceptors())
                    .launcher(r.parseLauncher())
                    .desc(r.desc)
                    .className(e.javaClassName)
                    .exported(r.exported)
                e.parseBelongsTo()?.let {
                    builder.moduleName(it)
                }
            }
        }
        roundEnv.getElementsAnnotatedWith(Services::class.java).forEach { e ->
            collector.addService { builder ->
                val services = e.getAnnotation(Services::class.java)

                val method = when (e.kind) {
                    ElementKind.METHOD -> {
                        e.requireModifier(setOf(Modifier.PUBLIC, Modifier.STATIC))
                        e as ExecutableElement
                        e.enclosingElement.requireModifier(setOf(Modifier.PUBLIC))

                        builder.returnType(e.returnType.asRawType().javaClassName)
                            .sourceClassName((e.enclosingElement as TypeElement).javaClassName)
                        e
                    }
                    ElementKind.CLASS -> {
                        val className = e.asType().asRawType().javaClassName
                        builder.returnType(className)
                            .sourceClassName(className)
                        e.findPublicConstructor()
                    }
                    else -> error(
                        "@Services is not annotate on Class or Method, is ${e.asType()}"
                    )
                }
                builder.sourceMethodName(method.simpleName.toString())
                method.parseMethodDependencies { className, serviceName, type, optional ->
                    builder.addMethodParam(className, serviceName, type, optional)
                }

                builder.serviceTypes(services.getServiceTypes())
                    .desc(services.desc)
                    .taskDependencies(services.dependencies.asList())

                e.getAnnotation(Singleton::class.java)?.let {
                    builder.singleton(true)
                }

                e.getAnnotation(Named::class.java)?.value?.let {
                    builder.serviceName(it)
                }
                e.parseBelongsTo()?.let {
                    builder.moduleName(it)
                }
            }
        }

        val moduleActivator =
            processingEnv.elementUtils.getTypeElement(ModuleActivator::class.java.name).asType()

        roundEnv.getElementsAnnotatedWith(ModuleOptions::class.java).forEach { element ->
            val m = element.getAnnotation(ModuleOptions::class.java)
            val type = element.asType()
            collector.addModule {
                it.moduleName(m.name)
                    .defaultInLibrary(m.defaultModule)
                    .bootStrapMode(m.mode)
                    .desc(m.desc)
                    .attributes(element.parseAttributes())
                if (processingEnv.typeUtils.isSubtype(
                        type,
                        moduleActivator
                    )
                ) {
                    val className = type.asRawType().javaClassName
                    it.activatorClass(className)
                        .onCreate {
                            parseOnCreateOrPostCreate(element, it.className(className), "onCreate")
                        }
                        .onPostCreate {
                            parseOnCreateOrPostCreate(
                                element,
                                it.className(className),
                                "onPostCreate"
                            )
                        }
                }
            }
        }

        val taskAction =
            processingEnv.elementUtils.getTypeElement(TaskAction::class.java.name).asType()

        roundEnv.getElementsAnnotatedWith(TaskOptions::class.java).forEach { element ->
            if (element.kind == ElementKind.CLASS) {
                val elementType = element.asType()
                if (!processingEnv.typeUtils.isAssignable(elementType, taskAction)) {
                    error("$elementType is not TaskAction.")
                }
                element.requireModifier(setOf(Modifier.PUBLIC))
                if (element.modifiers.contains(Modifier.ABSTRACT)) {
                    error("$elementType can't be abstract.")
                }

                element as TypeElement
                val options = element.getAnnotation(TaskOptions::class.java)
                collector.addTask { builder ->
                    builder.taskName(options.name)
                        .priority(options.priority)
                        .threadMode(options.threadMode)
                        .taskDependencies(options.dependencies.asList())
                        .className(element.javaClassName)

                    element.findPublicConstructor()
                        .parseMethodDependencies { className, serviceName, type, optional ->
                            builder.addConstructorParam(className, serviceName, type, optional)
                        }

                    val methods = element.enclosedElements.filter { it.kind == ElementKind.METHOD }
                        .map {
                            it as ExecutableElement
                        }

                    element.enclosedElements
                        .filter {
                            it.kind == ElementKind.FIELD || it.kind == ElementKind.METHOD
                        }
                        .forEach { e ->
                            e.getAnnotation(TaskOutput::class.java)?.let { output ->
                                if (e.modifiers.contains(Modifier.STATIC)) {
                                    error("Don't output static field or method.")
                                }
                                val (returnType, fieldOrMethod, isField) = if (e.kind == ElementKind.METHOD) {
                                    e as ExecutableElement
                                    require(e.parameters.isEmpty()) {
                                        "Method $elementType.$e should not have arguments."
                                    }
                                    Triple(
                                        e.returnType.asRawType().javaClassName,
                                        e.simpleName.toString(),
                                        false
                                    )
                                } else if (!e.modifiers.contains(Modifier.PUBLIC)) {
                                    e as VariableElement
                                    val fieldType = e.asType().asRawType()
                                    val getterName = "get${e.simpleName.toString().capitalize()}"
                                    methods.singleOrNull {
                                        it.simpleName.toString() == getterName
                                                && it.parameters.isEmpty()
                                                && processingEnv.typeUtils.isSameType(
                                            it.returnType,
                                            fieldType
                                        )
                                    }
                                        ?: error("@TaskOutput on none public field $elementType.$e but no getter found.")
                                    Triple(fieldType.javaClassName, getterName, false)
                                } else {
                                    e as VariableElement
                                    Triple(
                                        e.asType().asRawType().javaClassName,
                                        e.simpleName.toString(),
                                        true
                                    )
                                }

                                builder.addProducedService(
                                    e.namedOrDefault,
                                    returnType,
                                    output.getServiceTypes().let {
                                        if (it.isEmpty()) listOf(returnType)
                                        else it
                                    },
                                    output.desc,
                                    fieldOrMethod,
                                    isField
                                )
                            }
                        }

                    element.parseBelongsTo()?.let {
                        builder.moduleName(it)
                    }
                }
            }
        }

        roundEnv.getElementsAnnotatedWith(Inject::class.java).forEach {
            processingEnv.parseInjectors(it, saved, collector)
        }
    }

    private fun parseOnCreateOrPostCreate(
        element: Element, builder: TaskMetaBuilder, methodName: String
    ) {
        val options = element.enclosedElements
            .singleOrNull {
                if (it.kind == ElementKind.METHOD) {
                    it as ExecutableElement
                    // Take it easy, optimize later.
                    it.simpleName.toString() == methodName
                } else {
                    false
                }
            }
            ?.getAnnotation(TaskOptions::class.java)

        if (options != null) {
            builder.taskName(options.name)
                .priority(options.priority)
                .taskDependencies(options.dependencies.asList())
                .threadMode(options.threadMode)
        } else {
            builder.taskName(methodName)
        }

        element.findPublicConstructor()
            .parseMethodDependencies { className, serviceName, type, optional ->
                builder.addConstructorParam(className, serviceName, type, optional)
            }
    }
}

private fun Routes.parseLauncher(): String {
    return try {
        launcher
        throw AssertionError()
    } catch (e: MirroredTypeException) {
        e.typeMirror.asRawType().javaClassName
    }
}

private fun Routes.parseInterceptors(): List<String> {
    return try {
        interceptors
        throw AssertionError()
    } catch (e: MirroredTypesException) {
        if (e.typeMirrors.isEmpty()) {
            emptyList()
        } else {
            e.typeMirrors.map {
                it.asRawType().javaClassName
            }
        }
    }
}

private fun Services.getServiceTypes(): List<String> {
    return try {
        value
        throw AssertionError()
    } catch (e: MirroredTypesException) {
        e.typeMirrors.map {
            it.asRawType().javaClassName
        }
    }
}

private fun TaskOutput.getServiceTypes(): List<String> {
    return try {
        value
        throw AssertionError()
    } catch (e: MirroredTypesException) {
        e.typeMirrors.map {
            it.asRawType().javaClassName
        }
    }
}

private fun Element.findPublicConstructor(): ExecutableElement {
    if (modifiers.contains(Modifier.ABSTRACT)) error(
        "${asType()} can't be abstract if inject service by constructor"
    )
    val publicConstructors = enclosedElements.filter {
        it.kind == ElementKind.CONSTRUCTOR && it.modifiers.contains(Modifier.PUBLIC)
    }.map {
        it as ExecutableElement
    }
    return when {
        publicConstructors.isEmpty() -> {
            error(
                "${asType()} has no public constructor."
            )
        }
        publicConstructors.size > 1 -> {
            publicConstructors.filter {
                it.getAnnotation(Inject::class.java) != null
            }.let {
                if (it.size == 1) {
                    it[0]
                } else {
                    error(
                        "${asType()} has more than one public constructor and " +
                                "${if (it.isEmpty()) "none of them" else "more than one"} have Annotation @Inject."
                    )
                }
            }
        }
        else -> {
            publicConstructors[0]
        }
    }
}

fun AnnotatedConstruct.parseBelongsTo(): String? =
    getAnnotation(BelongsTo::class.java)?.value

fun AnnotatedConstruct.parseAttributes(): List<Pair<String, String>> {
    val res = mutableListOf<Pair<String, String>>()
    getAnnotation(Attribute::class.java)?.let {
        res += it.name to it.value
    }
    getAnnotation(Attributes::class.java)?.let {
        it.value.forEach {
            res += it.name to it.value
        }
    }
    return res
}

fun ExecutableElement.parseMethodDependencies(consumer: (String, String, DependencyType, Boolean) -> Unit) {
    return parameters.forEach {
        it.parseServiceDependency(serviceName = null, consumer = consumer)
    }
}

val AnnotatedConstruct.namedOrDefault: String
    get() = getAnnotation(Named::class.java)?.value ?: DEFAULT

fun VariableElement.parseServiceDependency(
    serviceName: String? = null,
    consumer: (String, String, DependencyType, Boolean) -> Unit
) {
    val t = asType().asDeclaredType()
    val dpName = serviceName ?: this.namedOrDefault
    val (dpClassName, dpType) = if (t.asElement().toString() == PROVIDER) {
        if (t.typeArguments.size != 1) error(
            "Provider without type parameter in ${asType()}"
        )
        t.typeArguments[0].run {
            if (this is WildcardType) {
                require(this.superBound == null && this.extendsBound != null) {
                    "Should inject provider like Provider<? extends XX> or Provider<XX>, but is $t"
                }
                this.extendsBound.asRawType().let {
                    it.asRawType().javaClassName to DependencyType.WILDCARD_PROVIDER
                }
            } else {
                asRawType().javaClassName to DependencyType.PROVIDER
            }
        }
    } else {
        if (t.typeArguments.isNotEmpty()) error(
            "Only accept inject services or javax.inject.Provider<T>,javax.inject.Provider<? extends T>, but is $t"
        )
        t.javaClassName to DependencyType.VALUE
    }
    consumer(dpClassName, dpName, dpType, annotationMirrors.any {
        it.annotationType.toString().endsWith(".Nullable")
    })
}