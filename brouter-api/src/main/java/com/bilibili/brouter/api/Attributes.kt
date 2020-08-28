package com.bilibili.brouter.api

import com.bilibili.brouter.api.internal.DefaultAttributeContainer
import com.bilibili.brouter.api.internal.HasInternalProtocol

interface HasConfigurableAttributes<SELF> : HasAttributes {
    override val attributes: MutableAttributeContainer
    fun attributes(action: (MutableAttributeContainer) -> Unit): SELF
}

interface HasAttributes {
    val attributes: AttributeContainer
}


/**
 * 属性，对应路由协议中的 -A
 */
@HasInternalProtocol
interface AttributeContainer : HasAttributes {

    val isEmpty: Boolean

    fun getAttribute(key: String): String?

    val keySet: Set<String>

    fun contains(key: String): Boolean
}

@HasInternalProtocol
interface MutableAttributeContainer : AttributeContainer,
    HasConfigurableAttributes<MutableAttributeContainer> {

    override val keySet: MutableSet<String>

    fun attribute(key: String, value: String): MutableAttributeContainer
}

fun attributesOf(attributes: Collection<Pair<String, String>>): AttributeContainer {
    return if (attributes.isEmpty()) {
        DefaultAttributeContainer.EMPTY
    } else {
        DefaultAttributeContainer(attributes.toMap())
    }
}

fun attributesOf(vararg attributes: Pair<String, String>): AttributeContainer {
    return attributesOf(attributes.asList())
}

fun attributesOf(map: Map<String, String>): AttributeContainer {
    return if (map.isEmpty()) {
        DefaultAttributeContainer.EMPTY
    } else {
        DefaultAttributeContainer(map.toMap())
    }
}

/**
 * route attribute
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Attribute(
    val name: String,
    val value: String
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Attributes(vararg val value: Attribute)

interface AttributeSchema {
    fun attribute(attributeName: String): AttributeMatchingStrategy
    fun attribute(
        attributeName: String,
        action: ((AttributeMatchingStrategy) -> Unit)?
    ): AttributeMatchingStrategy

    val attributes: Set<String>
    fun hasAttribute(attributeName: String): Boolean
}

interface AttributeMatchingStrategy {
    val attributeName: String
    val attributeCompatibilityRulesSnapshot: List<AttributeCompatibilityRule>
    val attributeDisambiguationRulesSnapshot: List<AttributeDisambiguationRule>
    fun addAttributeCompatibilityRule(attributeCompatibilityRule: AttributeCompatibilityRule)
    fun addAttributeDisambiguationRule(attributeDisambiguationRule: AttributeDisambiguationRule)
}

interface MultipleCandidatesDetails {

    val requestValue: String?

    val candidateValues: Set<String>

    fun closestMatch(candidate: String)
}

interface CompatibilityCheckDetails {

    val requestValue: String

    val producerValue: String

    fun compatible()

    fun incompatible()
}
typealias AttributeDisambiguationRule = (MultipleCandidatesDetails) -> Unit
typealias AttributeCompatibilityRule = (CompatibilityCheckDetails) -> Unit
