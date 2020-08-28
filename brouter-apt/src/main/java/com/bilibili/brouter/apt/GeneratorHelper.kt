package com.bilibili.brouter.apt

import com.squareup.javapoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleTypeVisitor6
import javax.tools.Diagnostic

const val PROVIDER = "javax.inject.Provider"
val DeclaredType.javaClassName: String
    get() = (this.asElement() as TypeElement).javaClassName
val TypeElement.javaClassName: String
    get() = ClassName.get(this).run {
        this.packageName() + "." + simpleNames().joinToString(
            separator = "$"
        )
    }

fun String.toClassName(): ClassName {
    val packageIndex = lastIndexOf(".")
    val packageName = if (packageIndex >= 0) {
        substring(0, packageIndex)
    } else {
        ""
    }
    val names = substring(packageIndex + 1).split("$")
    require(names.isNotEmpty() && names.none {
        it.isEmpty()
    }) {
        throw IllegalArgumentException("Illegal class name: $this")
    }
    return ClassName.get(packageName, names[0], *names.subList(1, names.size).toTypedArray())
}

fun ProcessingEnvironment.error(message: () -> Any?) {
    messager.printMessage(Diagnostic.Kind.ERROR, message().toString())
}

fun ProcessingEnvironment.warn(message: () -> Any?) {
    messager.printMessage(Diagnostic.Kind.WARNING, message().toString())
}

fun Element.requireModifier(required: Set<Modifier>) {
    if (!modifiers.containsAll(required)) {
        error("${asType()} is not $required")
    }
}

fun DeclaredType.requireNonTypeArgs() {
    if (typeArguments.isNotEmpty()) {
        error("Don't support inject generic type, but is ${this}.")
    }
}

fun TypeMirror.asRawType(): DeclaredType {
    return this.accept(AsDeclared, null).apply {
        asElement().requireModifier(setOf(Modifier.PUBLIC))
        requireNonTypeArgs()
    }
}


fun TypeMirror.asDeclaredType(): DeclaredType {
    return this.accept(AsDeclared, null).apply {
        asElement().requireModifier(setOf(Modifier.PUBLIC))
    }
}

internal object AsDeclared : SimpleTypeVisitor6<DeclaredType, Void>() {

    override fun defaultAction(p0: TypeMirror, p1: Void?): DeclaredType {
        error("Expect common class, but $p0 is ${p0.kind}")
    }

    override fun visitDeclared(p0: DeclaredType, p1: Void?): DeclaredType = p0
}