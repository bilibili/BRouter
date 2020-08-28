package com.bilibili.brouter.core.internal.routes

import android.util.LruCache
import com.bilibili.brouter.api.HasAttributes
import com.bilibili.brouter.api.internal.IRoutes
import com.bilibili.brouter.common.util.matcher.*

/**
 * late init capture, record uri only in constructor.
 * Keep memory and speed balance by this way.
 */
internal class SingleRouteRef(
    val routeRule: String,
    val routes: IRoutes
) : HasAttributes by routes {

    fun capture(segments: List<String>): Map<String, String> {
        return RouteCapture[routeRule].capture(segments)
    }

    override fun toString(): String {
        return "SingleRouteRef(routeRule='$routeRule', routes=$routes)"
    }
}

internal class RouteCapture(
    private val routeUri: String,
    private val captures: List<(List<String>, MutableMap<String, String>) -> Unit>,
    private val minSz: Int
) {

    fun capture(segments: List<String>): Map<String, String> {
        if (segments.size < minSz) {
            error("The route $routeUri can't capture $segments.")
        }
        return captures.fold(mutableMapOf<String, String>()) { map, captureFun ->
            captureFun(segments, map)
            map
        }.toMap()
    }

    companion object {

        private val cache = object : LruCache<String, RouteCapture>(128) {
            override fun create(uri: String): RouteCapture {
                return createByUri(uri)
            }
        }
        lateinit var parser: RawSegmentsParser

        operator fun get(uri: String): RouteCapture = cache[uri]

        fun createByUri(uri: String): RouteCapture {
            val rawSegments = parser.parse(uri)
            return RouteCapture(
                uri,
                rawSegments.mapIndexedNotNull { index: Int, rawSegment: RawSegment ->
                    rawSegment.toCaptureFunction(index)
                },
                rawSegments.size
            )
        }
    }
}

private fun RawSegment.toCaptureFunction(index: Int): ((List<String>, MutableMap<String, String>) -> Unit)? {
    return when (this) {
        is NormalRawSegment -> {
            val captureList = this.collectWithCapture("", null)
            val map = captureList.filter {
                it.second.isNotEmpty()
            }.toMap()
            if (map.isEmpty()) {
                null
            } else {
                return { inputs: List<String>, output: MutableMap<String, String> ->
                    val input = inputs[index]
                    requireNotNull(map[input]) {
                        "$this don't exactly match $input."
                    }.forEach {
                        output[it.first] = input.substring(it.second, it.third)
                    }
                }
            }
        }
        is WildCardRawSegment -> {
            this.name?.let { name ->
                val prefix = this.prefix?.length ?: 0
                val suffix = this.suffix?.length ?: 0
                { inputs: List<String>, output: MutableMap<String, String> ->
                    val segment = inputs[index]
                    output[name] = segment.substring(prefix, segment.length - suffix)
                }
            }
        }
        is PrefixRawSegment -> { inputs: List<String>, output: MutableMap<String, String> ->
            output[""] = inputs.subList(index, inputs.size).joinToString("/")
        }
    }
}

internal fun NormalRawSegment.collectWithCapture(
    prefix: String,
    name: String?
): List<Pair<String, List<Triple<String, Int, Int>>>> {
    val text = text
    val prefixLength = prefix.length
    return if (text != null) {
        next?.collectWithCapture(prefix + text, name) ?: listOf(
            prefix + text to if (name == null)
                emptyList()
            else
                listOf(Triple(name, prefixLength, prefixLength + text.length))
        )
    } else {
        val parts = parts!!
        val prefixes = parts.innerSegments.flatMap {
            it.collectWithCapture(prefix, parts.name)
        }
        val next = next
        val result = if (next == null) {
            prefixes
        } else {
            val suffixes = next.collectWithCapture("", null)
            prefixes.fold(ArrayList(prefixes.size * suffixes.size)) { out, pre ->
                val fixedLen = pre.first.length
                suffixes.forEach {
                    out += (pre.first + it.first) to (pre.second + it.second.map {
                        Triple(it.first, fixedLen + it.second, fixedLen + it.third)
                    })
                }
                out
            }
        }
        if (name == null) {
            result
        } else {
            result.map {
                it.first to (it.second + Triple(
                    name,
                    0,
                    it.first.length
                ))
            }
        }
    }
}