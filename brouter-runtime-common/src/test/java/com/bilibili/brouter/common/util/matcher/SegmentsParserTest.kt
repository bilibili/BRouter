package com.bilibili.brouter.common.util.matcher

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentsParserTest {

    @Test
    fun testParseBasic() {
        val parser = RawSegmentsParser("coffee")
        assertEquals(
            parser.parse("t1/"),
            parser.parse("/t1")
        )
        assertEquals(
            parser.parse("t1"),
            parser.parse("coffee://t1")
        )

        assertEquals(
            parser.parse("{}"),
            parser.parse("*")
        )

        assertEquals(
            parser.parse("ss{}aa"),
            parser.parse("ss*aa")
        )
    }

    @Test
    fun testParseParts() {
        val parser = RawSegmentsParser("stub")
        assertEquals(
            parser.parse("(?<scheme>http|https)://test"),
            listOf(
                NormalRawSegment(
                    null,
                    SegmentParts(
                        "scheme",
                        listOf(
                            NormalRawSegment("http", null, null),
                            NormalRawSegment("https", null, null)
                        )
                    ),
                    null
                ),
                NormalRawSegment("test", null, null)
            )
        )
    }

    /**
     * Ensure not 3x slower than normal separation.
     */
    @Test
    fun ensureSpeed() {
        val parser = RawSegmentsParser("stub")

        var c1 = System.currentTimeMillis()
        for (i in 0..1000000) {
            parser.parse("/asd/zxc/q{we}/asd")
        }
        c1 = System.currentTimeMillis() - c1

        var c2 = System.currentTimeMillis()
        for (i in 0..1000000) {
            "/asd/zxc/q{we}/asd".normalizePath()
        }
        c2 = System.currentTimeMillis() - c2

        assert(c1 < c2 * 3)
    }
}

internal fun String.normalizePath(): List<String> {
    var previous = 0
    var current = indexOf('/', previous)
    return if (current >= 0) {
        val segments = arrayListOf<String>()
        do {
            if (previous < current) {
                val segment = substring(previous, current)
                segments.add(segment)
            }
            previous = current + 1
            current = indexOf('/', previous)
        } while (current >= 0)
        segments.add(substring(previous))
        segments
    } else {
        listOf(this)
    }
}