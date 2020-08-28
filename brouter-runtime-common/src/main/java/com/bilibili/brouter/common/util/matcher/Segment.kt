package com.bilibili.brouter.common.util.matcher

/**
 * @author dieyi
 * Created at 2020/4/20.
 */
sealed class Segment<in T : Segment<T>> : Comparable<Segment<*>> {

    protected abstract val type: Int

    abstract val segment: String

    abstract fun match(input: String): Boolean

    override fun compareTo(other: Segment<*>): Int {
        val diff = type - other.type
        return if (diff == 0) {
            doCompareTo(other as T)
        } else {
            diff
        }
    }

    protected abstract fun doCompareTo(other: T): Int

    override fun equals(other: Any?): Boolean {
        return other is Segment<*> && (other === this || compareTo(other) == 0)
    }

    override fun toString(): String {
        return "Segment($segment)"
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + segment.hashCode()
        return result
    }
}

class ExactSegment(override val segment: String) : Segment<ExactSegment>() {

    override val type: Int get() = 0

    override fun doCompareTo(other: ExactSegment): Int {
        return segment.compareTo(other.segment)
    }

    override fun match(input: String): Boolean {
        return segment == input
    }
}

class WildCardSegment(val prefix: String, val suffix: String) :
    Segment<WildCardSegment>() {
    override val type: Int get() = 1

    override val segment: String get() = "$prefix*$suffix"

    override fun match(input: String): Boolean {
        return input.length >= prefix.length + suffix.length
                && input.startsWith(prefix)
                && input.endsWith(suffix)
    }

    override fun doCompareTo(other: WildCardSegment): Int {
        var diff = this.prefix.compareWithLength(other.prefix)
        if (diff == 0) {
            diff = this.suffix.compareWithLength(other.suffix)
        }
        return diff
    }
}

internal fun String.compareWithLength(other: String): Int {
    val ml = this.length
    val ol = other.length
    // The longer, the higher priority.
    var diff = ol - ml
    return if (diff == 0) {
        // Lexicographical order.
        for (i in 0 until ol) {
            diff = this[i] - other[i]
            if (diff != 0) return diff
        }
        0
    } else {
        diff
    }
}

object PrefixSegment : Segment<PrefixSegment>() {
    override val type: Int get() = 2
    override val segment: String get() = "**"
    override fun doCompareTo(other: PrefixSegment): Int = 0
    override fun match(input: String): Boolean = true
}

object RootSegment : Segment<RootSegment>() {
    override val type: Int get() = 3
    override val segment: String get() = "root"

    override fun match(input: String): Boolean = throw UnsupportedOperationException()

    override fun doCompareTo(other: RootSegment): Int = throw UnsupportedOperationException()
}

fun RawSegments.flatten(): Iterator<List<Segment<*>>> {
    val localSize = size
    val allValues: List<List<Segment<*>>> = this.mapIndexed { i, it ->
        when (it) {
            is NormalRawSegment -> {
                it.collect()
            }
            is WildCardRawSegment -> {
                listOf(
                    WildCardSegment(
                        it.prefix ?: "",
                        it.suffix ?: ""
                    )
                )
            }

            is PrefixRawSegment -> {
                require(i + 1 == localSize) {
                    "** can only appear at the end, but is ${this.joinToString("/")}"
                }
                listOf(PrefixSegment)
            }
        }
    }
    val values = Array<Segment<*>>(localSize) {
        RootSegment
    }.asList() as MutableList<Segment<*>>
    allValues.forEachIndexed { index, list ->
        values[index] = list[0]
    }
    val indexes = IntArray(localSize)
    indexes[localSize - 1] = -1
    return object : Iterator<List<Segment<*>>> {

        val end = allValues[0].size

        /**
         * To simplify the code, we assume each hasNext() followed with one next().
         */
        override fun hasNext(): Boolean {
            if (indexes[0] >= end) return false
            var index = localSize - 1
            while (true) {
                if (indexes[index] < allValues[index].size - 1) {
                    values[index] = allValues[index][++indexes[index]]
                    return true
                }
                if (index > 0) {
                    indexes[index] = 0
                    values[index] = allValues[index][0]
                    index--
                } else {
                    indexes[0]++
                    return false
                }
            }
        }

        override fun next(): List<Segment<*>> {
            return values
        }
    }
}


internal fun NormalRawSegment.collect(): List<ExactSegment> =
    collect("").map {
        ExactSegment(it)
    }

internal fun NormalRawSegment.collect(prefix: String): List<String> {
    val text = text
    return if (text != null) {
        next?.collect(prefix + text) ?: listOf(prefix + text)
    } else {
        val prefixes = parts!!.innerSegments.flatMap {
            it.collect(prefix)
        }
        val next = next
        if (next == null) {
            prefixes
        } else {
            val suffixes = next.collect("")
            prefixes.fold(ArrayList(prefixes.size * suffixes.size)) { out, pre ->
                suffixes.forEach {
                    out += (pre + it)
                }
                out
            }
        }
    }
}