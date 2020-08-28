package com.bilibili.brouter.core.internal.attribute

import com.bilibili.brouter.api.AttributeContainer
import com.bilibili.brouter.api.HasAttributes
import com.bilibili.brouter.core.internal.table.Merger

internal class HasAttributesContainer<T : HasAttributes>(
    private val matcher: AttributeMatcher<T>,
    private val initialValue: Pair<T, Int>
) :
    Merger<HasAttributesContainer<T>> {

    private var map: MutableMap<AttributeContainer, Pair<T, Int>>? = null

    fun add(pair: Pair<T, Int>) {
        val localMap = map ?: hashMapOf<AttributeContainer, Pair<T, Int>>().apply {
            this[initialValue.first.attributes] = initialValue
            map = this
        }
        val pre = localMap.put(pair.first.attributes, pair)
        if (pre != null) {
            require(
                pre.second.hasFlags(FLAG_ALLOW_OVERRIDE) && pair.second.hasFlags(
                    FLAG_OVERRIDE_EXISTS
                )
            ) {
                "Found duplicated values: ${pair.first}, ${pre.first}."
            }
        }
    }

    fun query(request: HasAttributes): List<T> {
        val candidates = map?.let {
            it.values.map {
                it.first
            }
        } ?: listOf(initialValue.first)
        return matcher.matches(request.attributes, candidates)
    }

    override fun merge(other: HasAttributesContainer<T>) {
        other.map?.let {
            it.values.forEach {
                add(it)
            }
        } ?: add(other.initialValue)
    }

    override fun toString(): String {
        return "HasAttributesContainer(${map?.values ?: initialValue})"
    }

    companion object {
        const val FLAG_ALLOW_OVERRIDE = 1
        const val FLAG_OVERRIDE_EXISTS = 2
    }
}

private fun Int.hasFlags(flags: Int): Boolean = (this and flags) == flags
