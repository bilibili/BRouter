package com.bilibili.brouter.apt.internal

import com.bilibili.brouter.api.ServiceCentral
import com.bilibili.brouter.api.ServiceInjector
import com.bilibili.brouter.apt.MetaCollector
import com.bilibili.brouter.apt.javaClassName
import com.bilibili.brouter.apt.toClassName
import com.bilibili.brouter.common.meta.ServiceConsumerClass
import com.squareup.javapoet.*
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Inject
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind


internal fun ProcessingEnvironment.parseInjectors(
    it: Element,
    saved: MutableSet<String>,
    collector: MetaCollector
) {
    // Don't consider constructor.
    if (it.kind == ElementKind.METHOD || it.kind == ElementKind.FIELD) {
        val type = it.enclosingElement as TypeElement
        parseTypeOnly(type, saved, collector)
    }
}

private fun ProcessingEnvironment.parseTypeOnly(
    type: TypeElement,
    saved: MutableSet<String>,
    collector: MetaCollector
) {
    if (saved.contains(type.javaClassName)) return
    if (type.kind == ElementKind.INTERFACE) {
        error("Don't support @AutoWired on interface: $type")
    }

    val className = type.javaClassName
    val elements = type.enclosedElements
    val methodElements = type.enclosedElements.filter {
        it.kind == ElementKind.METHOD
    }.map {
        it as ExecutableElement
    }

    saved += className

    collector.addConsumer { builder ->
        builder.consumerClassName(className)
        type.firstPreChainTypeOrNull()?.let {
            builder.superConsumerClassName(it)
        }
        elements
            .filter {
                it.kind == ElementKind.FIELD || it.kind == ElementKind.METHOD
            }
            .forEach { element ->
                element.getAnnotation(Inject::class.java)?.let {
                    if (element.modifiers.contains(Modifier.STATIC)) {
                        error("Don't support inject static field or method.")
                    }
                    if (element.kind == ElementKind.METHOD) {
                        element as ExecutableElement
                        element.requireSingleArgs()
                            .parseServiceDependency(element.namedOrDefault) { className, serviceName, type, optional ->
                                builder.addConsumerDetail(
                                    className,
                                    serviceName,
                                    type,
                                    optional,
                                    element.simpleName.toString(),
                                    false
                                )
                            }
                    } else {
                        element as VariableElement
                        if (element.modifiers.contains(Modifier.PRIVATE)) {
                            val setterName = "set${element.simpleName.toString().capitalize()}"
                            val setter = methodElements.singleOrNull { e ->
                                e.simpleName.toString() == setterName && e.parameters.size == 1
                                        && this.typeUtils.isSameType(
                                    e.parameters[0].asType(),
                                    element.asType()
                                ) && e.returnType.kind == TypeKind.VOID
                            }
                                ?: error("@Injector on ${element.enclosingElement}.${element.simpleName} which is private field or kotlin val, setter required or remove the private modifier.")
                            setter.parameters[0]
                                .parseServiceDependency(element.namedOrDefault) { className, serviceName, type, optional ->
                                    builder.addConsumerDetail(
                                        className,
                                        serviceName,
                                        type,
                                        optional,
                                        setter.simpleName.toString(),
                                        false
                                    )
                                }
                        } else {
                            element.parseServiceDependency { className, serviceName, type, optional ->
                                builder.addConsumerDetail(
                                    className,
                                    serviceName,
                                    type,
                                    optional,
                                    element.simpleName.toString(),
                                    true
                                )
                            }
                        }
                    }
                }
            }
    }
}

private fun ExecutableElement.requireSingleArgs(): VariableElement {
    require(parameters.size == 1) {
        "@AutoWired supports setter or field only, found on $enclosingElement.${simpleName}(...)."
    }
    return parameters[0]
}

private fun TypeElement.firstPreChainTypeOrNull(): String? {
    val t = this.superclass as DeclaredType
    val e = t.asElement() as TypeElement
    val packageName = e.enclosingElement.toString()
    if (packageName.startsWith("java.") ||
        packageName.startsWith("javax.") ||
        packageName.startsWith("android.") ||
        packageName.startsWith("com.android") ||
        packageName.startsWith("androidx.")
    ) {
        return null
    }
    return if (e.enclosedElements.any {
            (it.kind == ElementKind.FIELD || it.kind == ElementKind.METHOD) && it.getAnnotation(
                Inject::class.java
            ) != null
        }) {
        e.javaClassName
    } else {
        e.firstPreChainTypeOrNull()
    }
}

internal fun ServiceConsumerClass.generateSource(filer: Filer) {
    JavaFile.builder(
        consumerClassName.substring(0, consumerClassName.lastIndexOf('.')),
        TypeSpec.classBuilder(consumerClassName.substring(consumerClassName.lastIndexOf('.') + 1) + "\$\$BRInjector")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(ServiceInjector::class.java),
                    consumerClassName.toClassName()
                )
            )
            .addMethod(
                MethodSpec.methodBuilder("inject")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .addParameter(consumerClassName.toClassName(), "o")
                    .addParameter(ClassName.get(ServiceCentral::class.java), "services")
                    .returns(TypeName.VOID)
                    .addCode(
                        //
                        CodeBlock.builder()
                            .apply {
                                superConsumerClassName?.let {
                                    add(
                                        "services.inject(\$T.class, o);\n",
                                        it.toClassName()
                                    )
                                }
                                consumerDetails.forEach {
                                    add("\n")
                                    add("o.${it.fieldOrMethodName}")
                                    if (it.isField) {
                                        add(" = ")
                                    } else {
                                        add("(")
                                    }
                                    if (!it.dependency.optional) {
                                        add("\$T.requireNonNull(", _BuiltInKt)
                                    }
                                    it.dependency.appendTo(this, "services")
                                    if (!it.dependency.optional) {
                                        add(", \"Service not exists.\")")
                                    }
                                    if (!it.isField) {
                                        add(")")
                                    }
                                    add(";\n")
                                }
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    ).indent("    ").build().writeTo(filer)
}
