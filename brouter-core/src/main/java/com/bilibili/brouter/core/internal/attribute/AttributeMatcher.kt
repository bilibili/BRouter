package com.bilibili.brouter.core.internal.attribute

import com.bilibili.brouter.api.AttributeContainer
import com.bilibili.brouter.api.HasAttributes
import java.util.*
import kotlin.collections.ArrayList


internal interface AttributeMatcher<T : HasAttributes> {
    fun matches(requested: AttributeContainer, candidates: List<T>): List<T>
}


internal class DefaultAttributeMatcher<T : HasAttributes>(
    private val schema: AttributeSelectionSchema
) : AttributeMatcher<T> {

    override fun matches(requested: AttributeContainer, candidates: List<T>): List<T> {
        return when {
            candidates.isEmpty() -> {
                emptyList()
            }
            candidates.size == 1 -> {
                candidates[0].let {
                    if (isMatching(requested, it.attributes)) {
                        listOf(it)
                    } else {
                        emptyList()
                    }
                }
            }
            else -> {
                MultipleCandidateMatcher(
                    schema,
                    requested,
                    candidates
                ).getMatches()
            }
        }
    }

    private fun isMatching(requested: AttributeContainer, candidate: AttributeContainer): Boolean {
        if (requested.isEmpty || candidate.isEmpty) {
            return true
        }

        for (attributeName in requested.keySet) {
            candidate.getAttribute(attributeName)?.let { candidateValue ->
                if (!schema.matchValue(
                        attributeName,
                        requested.getAttribute(attributeName)!!,
                        candidateValue
                    )
                ) {
                    return false
                }
            }
        }
        return true
    }
}

internal class MultipleCandidateMatcher<T : HasAttributes>(
    private val schema: AttributeSelectionSchema,
    private val requested: AttributeContainer,
    private val candidates: List<T>
) {
    private val requestedKeys = requested.keySet.toList()
    private val requestedAttributeValues =
        arrayOfNulls<String>((1 + candidates.size) * requestedKeys.size)

    private val candidatesAttributes = candidates.map {
        it.attributes
    }

    private val compatible = BitSet(candidates.size).apply {
        set(0, candidates.size)
    }

    private lateinit var extraAttributes: Array<String>
    private lateinit var remaining: BitSet
    private var candidateWithLongestMatch: Int = 0
    private var lengthOfLongestMatch: Int = 0

    /**
     * Thanks the gradle, source: org.gradle.internal.component.model.MultipleCandidateMatcher.
     */
    fun getMatches(): List<T> {
        if (requestedKeys.isNotEmpty()) {
            // fill requested
            requestedKeys.forEachIndexed { i, key ->
                requestedAttributeValues[i] = requested.getAttribute(key)
            }

            // find compatible candidates
            candidatesAttributes.forEachIndexed { index, c ->
                var matchLength = 0
                for (i in requestedKeys.indices) {
                    val key = requestedKeys[i]
                    val requestedValue = requestedAttributeValues[i]!!
                    val candidateValue = c.getAttribute(key)
                    if (candidateValue != null) {
                        if (!schema.matchValue(key, requestedValue, candidateValue)) {
                            compatible.clear(index)
                            return@forEachIndexed
                        } else {
                            requestedAttributeValues[(1 + index) * requestedKeys.size + i] =
                                candidateValue
                        }
                        matchLength++
                    }
                }
                if (matchLength > lengthOfLongestMatch) {
                    lengthOfLongestMatch = matchLength;
                    candidateWithLongestMatch = index
                }
            }

            if (compatible.cardinality() <= 1) {
                return getCandidates(compatible)
            }

            // 最长匹配是其他的匹配的超集
            if (longestMatchIsSuperSetOfAllOthers()) {
                return listOf(candidates[candidateWithLongestMatch])
            }
        } else {
            // 请求为空集合时，优先匹配空集合
            // 因为这里的 candidates 不可能存在一样的，所以空集合只有一个
            val i = candidatesAttributes.indexOfFirst {
                it.isEmpty
            }
            if (i >= 0) {
                return listOf(candidates[i])
            }
        }


        remaining = BitSet(candidates.size)
        remaining.or(compatible)

        // 抉择请求中包含的 key
        for (a in requestedKeys.indices) {
            disambiguateWithAttribute(a)
            if (remaining.cardinality() == 0) {
                break
            }
        }

        if (remaining.cardinality() > 1) {
            // 抉择在备选中但是不在请求中的 key
            extraAttributes = schema.collectExtraAttributes(candidatesAttributes, requested);
            for (a in requestedKeys.size until (requestedKeys.size + extraAttributes.size)) {
                disambiguateWithAttribute(a)
                if (remaining.cardinality() == 0) {
                    break
                }
            }
        }

        if (remaining.cardinality() > 1 && requestedKeys.isNotEmpty()) {
            // 在请求的特性不为空时，对于额外的特性，我们认为越少，越匹配。

            val all = candidatesAttributes.size

            for (key in extraAttributes) {
                val containsKey = BitSet(all)
                for (i in 0 until all) {
                    if (candidatesAttributes[i].contains(key)) {
                        containsKey.set(i)
                    }
                }
                // 如果所有的备选者都包含这个特性，不进行过滤
                if (containsKey.cardinality().let { it > 0 && it != candidates.size }) {
                    remaining.andNot(containsKey)
                    if (remaining.cardinality() == 0) {
                        break
                    }
                }
            }
        }
        return getCandidates(if (remaining.cardinality() == 0) compatible else remaining)
    }

    private fun getCandidates(liveSet: BitSet): List<T> {
        val lives = liveSet.cardinality()
        return if (lives == 0) {
            emptyList()
        } else if (lives == 1) {
            listOf(candidates[liveSet.nextSetBit(0)])
        } else {
            ArrayList<T>(lives).apply {
                var c = liveSet.nextSetBit(0)
                while (c >= 0) {
                    add(candidates[c])
                    c = liveSet.nextSetBit(c + 1)
                }
            }
        }
    }

    private fun disambiguateWithAttribute(a: Int) {
        val candidateValues: Set<String> = getCandidateValues(a)
        if (candidateValues.size <= 1) {
            return
        }
        val matches: Set<String> =
            schema.disambiguate(getAttribute(a), getRequestedValue(a), candidateValues)
        if (matches.size < candidateValues.size) {
            removeCandidatesWithValueNotIn(a, matches)
        }
    }

    private fun longestMatchIsSuperSetOfAllOthers(): Boolean {
        var c = compatible.nextSetBit(0)
        while (c >= 0) {
            if (c == candidateWithLongestMatch) {
                c = compatible.nextSetBit(c + 1)
                continue
            }
            var lengthOfOtherMatch = 0
            for (a in requestedKeys.indices) {
                if (getCandidateValue(c, a) == null) {
                    continue
                }
                lengthOfOtherMatch++
                if (getCandidateValue(candidateWithLongestMatch, a) == null) {
                    return false
                }
            }
            if (lengthOfOtherMatch == lengthOfLongestMatch) {
                return false
            }
            c = compatible.nextSetBit(c + 1)
        }
        return true
    }

    private fun getCandidateValues(a: Int): Set<String> {
        var candidateValues: MutableSet<String>? = null
        var compatibleValue: String? = null
        var c = compatible.nextSetBit(0)
        while (c >= 0) {
            val candidateValue: String? = getCandidateValue(c, a)
            if (candidateValue == null) {
                c = compatible.nextSetBit(c + 1)
                continue
            }
            if (compatibleValue == null) {
                compatibleValue = candidateValue
            } else if (compatibleValue != candidateValue || candidateValues != null) {
                if (candidateValues == null) {
                    candidateValues = mutableSetOf()
                    candidateValues.add(compatibleValue)
                }
                candidateValues.add(candidateValue)
            }
            c = compatible.nextSetBit(c + 1)
        }
        return candidateValues ?: if (compatibleValue == null) {
            emptySet()
        } else {
            setOf(compatibleValue)
        }
    }

    private fun removeCandidatesWithValueNotIn(
        a: Int,
        matchedValues: Set<String>
    ) {
        var c = remaining.nextSetBit(0)
        while (c >= 0) {
            getCandidateValue(c, a).let {
                if (it == null || !matchedValues.contains(it)) {
                    remaining.clear(c)
                }
            }
            c = remaining.nextSetBit(c + 1)
        }
    }

    private fun getCandidateValue(c: Int, a: Int): String? {
        return if (a < requestedKeys.size) {
            requestedAttributeValues[getValueIndex(c, a)]
        } else {
            candidatesAttributes[c].getAttribute(getAttribute(a))
        }
    }

    private fun getAttribute(a: Int): String {
        return if (a < requestedKeys.size) {
            requestedKeys[a]
        } else {
            extraAttributes[a - requestedKeys.size]
        }
    }

    private fun getRequestedValue(a: Int): String? {
        return if (a < requestedKeys.size) {
            requestedAttributeValues[a]
        } else {
            null;
        }
    }

    private fun getValueIndex(c: Int, a: Int): Int {
        return (1 + c) * requestedKeys.size + a
    }
}