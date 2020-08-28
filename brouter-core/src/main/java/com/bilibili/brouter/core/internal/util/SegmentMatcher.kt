package com.bilibili.brouter.core.internal.util

import com.bilibili.brouter.common.util.matcher.PrefixSegment
import com.bilibili.brouter.common.util.matcher.RootSegment
import com.bilibili.brouter.common.util.matcher.Segment
import com.bilibili.brouter.core.internal.table.Merger
import java.io.PrintWriter
import java.io.StringWriter

class SegmentMatcher<T> : Merger<SegmentMatcher<T>> {

    constructor() {
        this.segment = RootSegment
        this.parent = this
        this.pre = this
        this.next = this
    }

    private constructor(
        segment: Segment<*>,
        parent: SegmentMatcher<T>
    ) {
        this.segment = segment
        this.parent = parent
        this.pre = this
        this.next = this
    }

    private constructor(
        segment: Segment<*>,
        parent: SegmentMatcher<T>,
        next: SegmentMatcher<T>,
        pre: SegmentMatcher<T> = next.pre
    ) {
        this.segment = segment
        this.parent = parent
        this.pre = pre
        this.next = next
        pre.next = this
        next.pre = this
    }

    private val isRoot: Boolean get() = segment === RootSegment
    private val segment: Segment<*>
    private var parent: SegmentMatcher<T>
    private var child: SegmentMatcher<T>? = null
    private var next: SegmentMatcher<T> = this
    private var pre: SegmentMatcher<T> = this
    var value: T? = null

    fun walkOrBuildPath(segments: List<Segment<*>>): SegmentMatcher<T> {
        var root = this
        for (s in segments) {
            var iter = root.child

            root = if (iter == null) {
                SegmentMatcher(s, root).let {
                    root.child = it
                    it
                }
            } else {

                var target: SegmentMatcher<T>? = null

                val start = iter
                do {
                    val r = iter!!.segment.compareTo(s)
                    if (r >= 0) {
                        target = if (r == 0) {
                            iter
                        } else {
                            SegmentMatcher(s, root, iter).apply {
                                if (parent.child === iter) {
                                    parent.child = this
                                }
                            }
                        }
                    }
                    iter = iter.next
                } while (target == null && iter !== start)
                target ?: SegmentMatcher(s, root, start, start.pre)
            }
        }
        return root
    }

    override fun merge(other: SegmentMatcher<T>) {
        other.value?.let { otherValue ->
            value.let { myValue ->
                if (myValue == null) {
                    this.value = otherValue
                } else if (myValue is Merger<*>) {
                    (myValue as Merger<T>).merge(otherValue)
                } else {
                    error("Found duplicated values: $myValue, $otherValue")
                }
            }
        }
        other.child?.let { it ->
            var otherChild = it
            child.let {
                if (it == null) {
                    child = otherChild
                    otherChild.loopSibling {
                        it.parent = this
                    }
                } else {
                    var myChild: SegmentMatcher<T> = it
                    var start = myChild

                    // stub end point
                    // remove the loop
                    myChild.pre.next = this
                    otherChild.pre.next = this

                    var preHead: SegmentMatcher<T>? = null
                    lateinit var preTail: SegmentMatcher<T>
                    do {
                        val r = myChild.segment.compareTo(otherChild.segment)
                        when {
                            r == 0 -> {
                                if (preHead != null) {
                                    myChild.linkPreRange(preHead, preTail)
                                    if (myChild === start) {
                                        start = preHead
                                        start.pre.next = this
                                    }
                                    preHead = null
                                }

                                myChild.merge(otherChild)
                                myChild = myChild.next
                                otherChild = otherChild.next
                            }
                            r < 0 -> {
                                if (preHead != null) {
                                    myChild.linkPreRange(preHead, preTail)
                                    if (myChild === start) {
                                        start = preHead
                                        start.pre.next = this
                                    }
                                    preHead = null
                                }
                                myChild = myChild.next
                            }
                            else -> {
                                otherChild.parent = this
                                if (preHead == null) {
                                    preHead = otherChild
                                }
                                preTail = otherChild
                                otherChild = otherChild.next
                            }
                        }
                    } while (myChild !== this && otherChild !== this)

                    while (otherChild !== this) {
                        if (preHead == null) {
                            preHead = otherChild
                        }
                        otherChild.parent = this
                        preTail = otherChild
                        otherChild = otherChild.next
                    }
                    child = start
                    if (preHead != null) {
                        (if (myChild === this) start else myChild).linkPreRange(preHead, preTail)
                        if (myChild === start) {
                            child = preHead
                        }
                    }
                    start.pre.next = start
                }
            }
            other.child = null
        }
    }


    private fun linkPreRange(preHead: SegmentMatcher<T>, preTail: SegmentMatcher<T>) {
        pre.next = preHead
        preHead.pre = pre

        preTail.next = this
        this.pre = preTail
    }

    fun delete() {
        if (child == null) {
            this.pre.next = this.next
            this.next.pre = this.pre
            if (this.parent.child === this) {
                this.parent.child = if (this.next === this) {
                    null
                } else {
                    this.next
                }
            }
        }
        value == null
    }

    fun match(segments: List<String>): T? {
        return doMatch(segments, 0)
    }

    private fun doMatch(segments: List<String>, position: Int): T? {
        if (this.segment is PrefixSegment || position >= segments.size) return this.value
        val cur = segments[position]
        child?.loopSibling {
            if (it.segment.match(cur)) {
                it.doMatch(segments, position + 1)?.let {
                    return it
                }
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        return other is SegmentMatcher<*> &&
                (other === this ||
                        value == other.value &&
                        segment == other.segment &&
                        childrenEquals(other.child))
    }

    private fun childrenEquals(otherChild: SegmentMatcher<*>?): Boolean {
        return child?.let { e1 ->
            otherChild?.let { e2 ->
                var c1 = e1
                var c2 = e2
                do {
                    if (c1 != c2) {
                        return false
                    }
                    c1 = c1.next
                    c2 = c2.next
                } while (c1 !== e1 && c2 !== e2)
                return c1 === e1 && c2 === e2
            } ?: false
        } ?: otherChild === null
    }

    private inline fun loopSibling(action: (SegmentMatcher<T>) -> Unit) {
        var start = this
        val end = this
        do {
            action(start)
            start = start.next
        } while (start !== end)
    }

    override fun toString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        val walker = TreeDumper(pw)
        walker.dump(this)
        pw.flush()
        return sw.toString()
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    private class TreeDumper(val pw: PrintWriter) {
        val prefix = StringBuilder()

        fun dump(matcher: SegmentMatcher<*>) {
            pw.print(prefix)
            pw.print(matcher.segment.toString())
            if (matcher.value != null) {
                pw.println(matcher.value)
            } else {
                pw.println()
            }
            matcher.child?.loopSibling {
                prefix.append("     ")
                dump(it)
                prefix.delete(prefix.length - 5, prefix.length)
            }
        }
    }
}