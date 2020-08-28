package com.bilibili.brouter.api.internal

import android.net.Uri
import com.bilibili.brouter.api.RouteRequest
import com.bilibili.brouter.api.toRouteRequest

private const val PREFIX_PROP = "-P"
private const val DATA = "-Pup.data"
private const val REQUEST_CODE = "-Pup.code"
private const val FLAGS = "-Pup.flags"
private const val TYPES = "-Pup.types"
private const val PREV = "-Pup.prev"
private const val FORWARD = "-Pup.forward"

private const val PREFIX_ATTR = "-A"

internal val Uri.queryMap: MutableMap<String, MutableList<String>>?
    get() {
        val query = encodedQuery ?: return null

        val res = hashMapOf<String, MutableList<String>>()
        var start = 0
        do {
            val next = query.indexOf('&', start)
            val end = if (next == -1) query.length else next

            var separator = query.indexOf('=', start)
            if (separator > end || separator == -1) {
                separator = end
            }

            val name = query.substring(start, separator).decode

            res.getOrPut(name) { arrayListOf() }.add(
                if (separator == end) {
                    ""
                } else {
                    query.substring(separator + 1, end).decode
                }
            )

            // Move start to end of name.
            start = end + 1
        } while (start < query.length)

        return res
    }


internal fun StringBuilder.appendData(data: Uri) {
    appendQueryAllowComma(DATA, data.toString())
}

internal fun MutableMap<String, MutableList<String>>.parseData(): Uri? = remove(DATA)?.let {
    Uri.parse(it.last())
}

internal fun StringBuilder.appendRequestCode(code: Int) {
    appendQueryAllowComma(REQUEST_CODE, code.toString())
}

internal fun MutableMap<String, MutableList<String>>.parseRequestCode(): Int =
    remove(REQUEST_CODE)?.let {
        it.last().toIntOrNull()
    } ?: -1

internal fun StringBuilder.appendFlags(flags: Int) {
    appendQueryAllowComma(FLAGS, flags.toString(16))
}

internal fun MutableMap<String, MutableList<String>>.parseFlags(): Int = remove(FLAGS)?.let {
    var ret = 0
    for (s in it) {
        s.toIntOrNull(16)?.let {
            ret = ret or it
        }
    }
    ret
} ?: 0

internal fun StringBuilder.appendRouteTypes(routeTypes: List<String>) {
    routeTypes.forEach {
        appendQueryAllowComma(TYPES, it)
    }
}

internal fun MutableMap<String, MutableList<String>>.parseRouteTypes(): List<String> =
    remove(TYPES) ?: emptyList()

internal fun StringBuilder.appendPrev(prev: RouteRequest) {
    appendQueryAllowComma(PREV, prev.uniformUri.toString())
}

internal fun MutableMap<String, MutableList<String>>.parsePrev(): RouteRequest? =
    remove(PREV)?.let {
        Uri.parse(it.last()).toRouteRequest()
    }

internal fun StringBuilder.appendForward(forward: RouteRequest) {
    appendQueryAllowComma(FORWARD, forward.uniformUri.toString())
}

internal fun MutableMap<String, MutableList<String>>.parseForward(): RouteRequest? =
    remove(FORWARD)?.let {
        Uri.parse(it.last()).toRouteRequest()
    }

internal fun StringBuilder.appendAttrs(attributes: AttributeContainerInternal) {
    for ((key, value) in attributes.attributesMap) {
        appendQueryAllowComma(PREFIX_ATTR + key, value)
    }
}

internal fun MutableMap<String, MutableList<String>>.parseAttrs(): MutableAttributeContainerInternal {
    val it = iterator()
    val attributes = DefaultMutableAttributeContainer()

    while (it.hasNext()) {
        val entry = it.next()
        if (entry.key.startsWith(PREFIX_ATTR)) {
            attributes.attribute(entry.key.substring(2), entry.value.last())
            it.remove()
        }
    }
    return attributes
}

internal fun StringBuilder.appendProps(props: MultiMapInternal) {
    for (key in props.keySet) {
        props.getAll(key)!!.forEach { value ->
            appendQueryAllowComma(PREFIX_PROP + key, value)
        }
    }
}

internal fun MutableMap<String, MutableList<String>>.parseProps(): MutableMultiMapInternal {
    val it = iterator()
    val ret = DefaultMutableMultiMap()
    while (it.hasNext()) {
        val entry = it.next()
        if (entry.key.startsWith(PREFIX_PROP)) {
            it.remove()
            ret.appendAll(entry.key.substring(2), entry.value)
        }
    }
    return ret
}

internal fun StringBuilder.appendParams(params: MultiMapInternal) {
    for (key in params.keySet) {
        params.getAll(key)!!.forEach { value ->
            appendQueryAllowComma(key, value)
        }
    }
}


internal fun MutableMap<String, MutableList<String>>.parseParams(): MutableMultiMapInternal {
    return DefaultMutableMultiMap(this)
}

private fun StringBuilder.appendQueryAllowComma(key: String, value: String) {
    if (isNotEmpty()) {
        append("&")
    }
    append(Uri.encode(key, ","))
        .append("=")
        .append(Uri.encode(value, ","))
}

internal val String.decode get() = Uri.decode(this)
