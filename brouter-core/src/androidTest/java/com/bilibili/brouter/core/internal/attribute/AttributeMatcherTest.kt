package com.bilibili.brouter.core.internal.attribute

import android.support.test.runner.AndroidJUnit4
import com.bilibili.brouter.api.AttributeContainer
import com.bilibili.brouter.api.HasAttributes
import com.bilibili.brouter.api.attributesOf
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AttributeMatcherTest {

    @Test
    fun test() {
        val candidates = listOf(
            attributesOf("a" to "1", "b" to "1"),
            attributesOf("a" to "1", "b" to "1", "c" to "1"),
            attributesOf("a" to "2", "b" to "2"),
            attributesOf("a" to "3", "b" to "3"),
            attributesOf("a" to "3", "b" to "3", "c" to "3"),
            attributesOf("a" to "3", "b" to "3", "c" to "4"),
            attributesOf("a" to "4", "b" to "3")
        )
        var matches = matcher.matches(
            attributesOf(),
            candidates
        )
        Assert.assertEquals(matches.size, 2)
        Assert.assertEquals(matches, listOf(candidates[0], candidates[1]))


        matches = matcher.matches(
            attributesOf(
                "a" to "1"
            ),
            candidates
        )
        Assert.assertEquals(matches.size, 1)
        Assert.assertEquals(matches, listOf(candidates[0]))


        matches = matcher.matches(
            attributesOf("a" to "3"),
            candidates
        )
        Assert.assertEquals(matches.size, 1)
        Assert.assertEquals(matches[0], candidates[3])

        matches = matcher.matches(
            attributesOf("b" to "3"),
            candidates
        )
        Assert.assertEquals(matches.size, 1)
        Assert.assertEquals(matches[0], candidates[3])
    }

    @Test
    fun test2() {
        val candidates = listOf(
            attributesOf(),
            attributesOf("b" to "1"),
            attributesOf("b" to "2")
        )

        var matches = matcher.matches(
            attributesOf(),
            candidates
        )

        Assert.assertEquals(matches, candidates.subList(0, 1))

        matches = matcher.matches(
            attributesOf("b" to "1"),
            candidates
        )

        Assert.assertEquals(matches, candidates.subList(1, 2))

        matches = matcher.matches(
            attributesOf("b" to "2"),
            candidates
        )

        Assert.assertEquals(matches, candidates.subList(2, 3))
    }

    companion object {

        @JvmStatic
        val matcher = DefaultAttributeMatcher<AttributeContainer>(
            DefaultAttributeSchema()
                .apply {
                    attribute("a") {
                        // 1 2 3 4
                        // 4 -> 3
                        it.addAttributeCompatibilityRule {
                            if (it.producerValue == "3" && it.requestValue == "4") {
                                it.compatible()
                            }
                        }
                        // 选最小的
                        it.addAttributeDisambiguationRule {
                            it.closestMatch(it.candidateValues.min()!!)
                        }
                    }
                    attribute("b") {

                    }

                    attribute("c") {
                        it.addAttributeDisambiguationRule {
                            Assert.assertNull(it.requestValue)
                        }
                    }
                }
                .asSelector
        )
    }
}


internal class AttributesTestOnly(override val attributes: AttributeContainer) : HasAttributes