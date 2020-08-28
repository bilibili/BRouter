package com.bilibili.brouter.core.internal.util

import android.support.test.runner.AndroidJUnit4
import com.bilibili.brouter.common.util.matcher.ExactSegment
import com.bilibili.brouter.core.internal.util.SegmentMatcher
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SegmentMatcherTests {

    @Test
    fun testMerge1() {
        val s1 = SegmentMatcher<Int>()
        val s2 = SegmentMatcher<Int>()
        val s3 = SegmentMatcher<Int>()
        s1.addSegments(listOf("3"), 3)

        s2.addSegments(listOf("1"), 1)
        s2.addSegments(listOf("4"), 4)
        s2.addSegments(listOf("2"), 2)
        s2.addSegments(listOf("5"), 5)
        s2.addSegments(listOf("6"), 6)

        s3.addSegments(listOf("1"), 1)
        s3.addSegments(listOf("2"), 2)
        s3.addSegments(listOf("3"), 3)
        s3.addSegments(listOf("4"), 4)
        s3.addSegments(listOf("5"), 5)
        s3.addSegments(listOf("6"), 6)



        s1.merge(s2)

        Assert.assertEquals(s2, SegmentMatcher<Int>())
        Assert.assertEquals(s1, s3)
    }


    @Test
    fun testMerge() {
        val s1 = SegmentMatcher<Int>()
        val s2 = SegmentMatcher<Int>()
        val s3 = SegmentMatcher<Int>()
        s1.addSegments(listOf("3"), 3)

        s2.addSegments(listOf("1"), 1)
        s2.addSegments(listOf("4"), 4)
        s2.addSegments(listOf("2"), 2)
        s2.addSegments(listOf("5"), 5)
        s2.addSegments(listOf("6"), 6)

        s3.addSegments(listOf("1"), 1)
        s3.addSegments(listOf("2"), 2)
        s3.addSegments(listOf("3"), 3)
        s3.addSegments(listOf("4"), 4)
        s3.addSegments(listOf("5"), 5)
        s3.addSegments(listOf("6"), 6)



        s1.merge(s2)

        Assert.assertEquals(s2, SegmentMatcher<Int>())
        Assert.assertEquals(s1, s3)
    }

    @Test
    fun testMerge2() {
        val s1 = SegmentMatcher<Int>()
        val s2 = SegmentMatcher<Int>()
        val s3 = SegmentMatcher<Int>()
        s1.addSegments(listOf("1"), 1)
        s1.addSegments(listOf("3"), 3)
        s1.addSegments(listOf("5"), 5)

        s2.addSegments(listOf("4"), 4)
        s2.addSegments(listOf("2"), 2)
        s2.addSegments(listOf("6"), 6)

        s3.addSegments(listOf("1"), 1)
        s3.addSegments(listOf("2"), 2)
        s3.addSegments(listOf("3"), 3)
        s3.addSegments(listOf("4"), 4)
        s3.addSegments(listOf("5"), 5)
        s3.addSegments(listOf("6"), 6)



        s1.merge(s2)

        Assert.assertEquals(s2, SegmentMatcher<Int>())
        Assert.assertEquals(s1, s3)
    }

    @Test
    fun testMerge3() {
        val s1 = SegmentMatcher<Int>()
        val s2 = SegmentMatcher<Int>()
        val s3 = SegmentMatcher<Int>()
        s2.addSegments(listOf("1"), 1)
        s1.addSegments(listOf("3"), 3)
        s2.addSegments(listOf("6"), 6)

        s2.addSegments(listOf("4"), 4)
        s2.addSegments(listOf("2"), 2)
        s2.addSegments(listOf("5"), 5)

        s3.addSegments(listOf("1"), 1)
        s3.addSegments(listOf("2"), 2)
        s3.addSegments(listOf("3"), 3)
        s3.addSegments(listOf("4"), 4)
        s3.addSegments(listOf("5"), 5)
        s3.addSegments(listOf("6"), 6)



        s1.merge(s2)

        Assert.assertEquals(s2, SegmentMatcher<Int>())
        Assert.assertEquals(s1, s3)
    }

    @Test
    fun testMerge4() {
        val s1 = SegmentMatcher<Int>()
        val s2 = SegmentMatcher<Int>()
        val s3 = SegmentMatcher<Int>()
        s2.addSegments(listOf("2"), 2)
        s1.addSegments(listOf("4"), 4)
        s2.addSegments(listOf("6"), 6)

        s2.addSegments(listOf("1"), 1)
        s2.addSegments(listOf("3"), 3)
        s2.addSegments(listOf("5"), 5)

        s3.addSegments(listOf("1"), 1)
        s3.addSegments(listOf("2"), 2)
        s3.addSegments(listOf("3"), 3)
        s3.addSegments(listOf("4"), 4)
        s3.addSegments(listOf("5"), 5)
        s3.addSegments(listOf("6"), 6)



        s1.merge(s2)

        Assert.assertEquals(s2, SegmentMatcher<Int>())
        Assert.assertEquals(s1, s3)
    }

    @Test
    fun testMerge5() {
        val s1 = SegmentMatcher<Int>()
        val s2 = SegmentMatcher<Int>()
        val s3 = SegmentMatcher<Int>()
        s2.addSegments(listOf("2"), 2)

        s2.addSegments(listOf("1"), 1)

        s3.addSegments(listOf("1"), 1)
        s3.addSegments(listOf("2"), 2)

        s1.merge(s2)

        Assert.assertEquals(s2, SegmentMatcher<Int>())
        Assert.assertEquals(s1, s3)
    }

    @Test
    fun testMerge6() {
        val s1 = SegmentMatcher<Int>()
        val s2 = SegmentMatcher<Int>()
        val s3 = SegmentMatcher<Int>()
        s2.addSegments(listOf("1"), 2)

        s2.addSegments(listOf("2"), 2)

        s3.addSegments(listOf("1"), 1)
        s3.addSegments(listOf("2"), 2)

        s1.merge(s2)

        Assert.assertEquals(s2, SegmentMatcher<Int>())
        Assert.assertNotEquals(s1, s3)
    }
}

internal fun <T> SegmentMatcher<T>.addSegments(segments: List<String>, t: T) {
    val matcher = walkOrBuildPath(segments.map {
        ExactSegment(it)
    })
    if (matcher.value == null) {
        matcher.value = t
    } else {
        error("Duplicated value $t.")
    }
}