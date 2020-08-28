package com.bilibili.brouter.core.internal.attribute

import com.bilibili.brouter.api.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


internal interface InternalAttributeSchema : AttributeSchema {
    val asSelector: AttributeSelectionSchema

    override fun attribute(attributeName: String): InternalAttributeMatchingStrategy
}

internal interface InternalAttributeMatchingStrategy : AttributeMatchingStrategy {
    fun execute(result: CompatibilityCheckResult)
    fun execute(result: MultipleCandidatesResult)
}

internal interface CompatibilityCheckResult : CompatibilityCheckDetails {
    val hasResult: Boolean
}

internal interface MultipleCandidatesResult : MultipleCandidatesDetails {
    val hasResult: Boolean
    val matches: Set<String>
}

internal class DefaultAttributeSchema : InternalAttributeSchema {

    private val map = ConcurrentHashMap<String, InternalAttributeMatchingStrategy>()
    override val asSelector: AttributeSelectionSchema
        get() = DefaultAttributeSelectionSchema(this)

    override fun attribute(attributeName: String): InternalAttributeMatchingStrategy {
        return attribute(attributeName, null)
    }

    override fun attribute(
        attributeName: String,
        action: ((AttributeMatchingStrategy) -> Unit)?
    ): InternalAttributeMatchingStrategy {
        return map.getOrPut(attributeName) {
            DefaultAttributeMatchingStrategy(attributeName)
        }.also {
            action?.invoke(it)
        }
    }

    override val attributes: Set<String>
        get() = map.keys

    override fun hasAttribute(attributeName: String): Boolean {
        return map.containsKey(attributeName)
    }
}

private class DefaultAttributeMatchingStrategy(override val attributeName: String) :
    InternalAttributeMatchingStrategy {
    override fun execute(result: CompatibilityCheckResult) {
        for (rule in attributeCompatibilityRules) {
            rule(result)
            if (result.hasResult) {
                return
            }
        }
    }

    override fun execute(result: MultipleCandidatesResult) {
        for (rule in attributeDisambiguationRules) {
            rule(result)
            if (result.hasResult) {
                return
            }
        }
    }

    private val attributeCompatibilityRules: MutableList<AttributeCompatibilityRule> =
        CopyOnWriteArrayList()
    private val attributeDisambiguationRules: MutableList<AttributeDisambiguationRule> =
        CopyOnWriteArrayList()

    override val attributeCompatibilityRulesSnapshot: List<AttributeCompatibilityRule> =
        attributeCompatibilityRules.toList()
    override val attributeDisambiguationRulesSnapshot: List<AttributeDisambiguationRule> =
        attributeDisambiguationRules.toList()

    override fun addAttributeCompatibilityRule(attributeCompatibilityRule: AttributeCompatibilityRule) {
        attributeCompatibilityRules += attributeCompatibilityRule
    }

    override fun addAttributeDisambiguationRule(attributeDisambiguationRule: AttributeDisambiguationRule) {
        attributeDisambiguationRules += attributeDisambiguationRule
    }
}

private class DefaultAttributeSelectionSchema(val schema: InternalAttributeSchema) :
    AttributeSelectionSchema {
    override fun matchValue(attributeName: String, requested: String, candidate: String): Boolean {
        return if (requested == candidate) {
            true
        } else {
            DefaultCompatibilityCheckResult(requested, candidate).let {
                schema.attribute(attributeName).execute(it)
                it.hasResult && it.compatible
            }
        }
    }

    override fun disambiguate(
        attributeName: String,
        requested: String?,
        candidates: Set<String>
    ): Set<String> {
        val result = DefaultMultipleCandidateResult(requested, candidates)
        schema.attribute(attributeName).execute(result)
        return if (result.hasResult) {
            result.matches
        } else if (requested != null && candidates.contains(requested)) {
            setOf(requested)
        } else {
            candidates
        }
    }

    override fun collectExtraAttributes(
        candidatesAttributes: List<AttributeContainer>,
        requested: AttributeContainer
    ): Array<String> {
        val set = linkedSetOf<String>()
        for (c in candidatesAttributes) {
            set += c.keySet
        }
        set -= requested.keySet
        return set.toTypedArray()
    }
}

private class DefaultCompatibilityCheckResult(
    override val requestValue: String,
    override val producerValue: String
) :
    CompatibilityCheckResult {

    override var hasResult: Boolean = false
    var compatible: Boolean = false
        private set(value) {
            hasResult = true
            field = value
        }

    override fun compatible() {
        compatible = true
    }

    override fun incompatible() {
        compatible = false
    }
}

private class DefaultMultipleCandidateResult(
    override val requestValue: String?,
    override val candidateValues: Set<String>
) : MultipleCandidatesResult {
    override var hasResult: Boolean = false
    override val matches: MutableSet<String> = mutableSetOf()

    override fun closestMatch(candidate: String) {
        hasResult = true
        matches += candidate
    }
}