package com.bilibili.brouter.core.internal.table

import android.annotation.SuppressLint
import com.bilibili.brouter.api.AttributeContainer
import com.bilibili.brouter.api.internal.IRoutes
import com.bilibili.brouter.api.internal.RouteRegistry
import com.bilibili.brouter.common.util.matcher.RawSegmentsParser
import com.bilibili.brouter.core.internal.attribute.AttributeMatcher
import com.bilibili.brouter.core.internal.attribute.HasAttributesContainer
import com.bilibili.brouter.core.internal.routes.SingleRouteRef
import com.bilibili.brouter.core.internal.util.Initializable
import com.bilibili.brouter.core.internal.util.SegmentMatcher
import com.bilibili.brouter.common.util.matcher.flatten
import java.util.concurrent.ForkJoinTask

internal class RouteTable(
    private val attributeMatcher: AttributeMatcher<SingleRouteRef>,
    internal val parser: RawSegmentsParser
) : RouteRegistry, Merger<RouteTable>, Initializable() {

    internal val map =
        mutableMapOf<String, SegmentMatcher<HasAttributesContainer<SingleRouteRef>>>()

    internal var defaultFlag = 0

    override fun registerRoutes(routes: IRoutes) {
        registerRoutes(
            routes,
            defaultFlag
        )
    }

    override fun merge(other: RouteTable) {
        other.map.forEach { (type, otherMatcher) ->
            val selfMatcher = map[type]
            if (selfMatcher == null) {
                map[type] = otherMatcher
            } else {
                selfMatcher.merge(otherMatcher)
            }
        }
    }

    fun registerRoutes(routes: IRoutes, flags: Int) {
        val inited = initialized
        val rootMatcher = map.mayLock(inited) {
            map.getOrPut(routes.routeType) {
                SegmentMatcher()
            }
        }
        for (routeRule in routes.routeRules) {
            val rawSegments = parser.parse(routeRule)
            val routeRef = SingleRouteRef(routeRule, routes) to flags

            // convert
            for (segments in rawSegments.flatten()) {
                rootMatcher.mayLock(inited) {
                    it.walkOrBuildPath(segments)
                }.mayLock(inited) {
                    it.value.let { ctor ->
                        if (ctor != null) {
                            ctor.add(routeRef)
                        } else {
                            it.value = HasAttributesContainer(attributeMatcher, routeRef)
                        }
                    }
                }
            }
        }
    }

    fun findRoute(
        segments: List<String>,
        type: String,
        attributes: AttributeContainer
    ): List<SingleRouteRef>? {
        return synchronized(map) {
            map[type]
        }?.let { rootMatcher ->
            synchronized(rootMatcher) {
                rootMatcher.match(segments)
            }?.let {
                synchronized(it) {
                    it.query(attributes)
                }
            }
        }
    }

    override fun toString(): String {
        return "RouteTable$map"
    }
}

internal inline fun <T : Any, R> T.mayLock(lock: Boolean, block: (T) -> R): R {
    return if (lock) {
        synchronized(this) {
            block(this)
        }
    } else {
        block(this)
    }
}

@SuppressLint("NewApi")
internal class MergeRoute(private val table1: RouteTable, private val table2: RouteTable) :
    ForkJoinTask<Unit>() {
    override fun getRawResult(): Unit = Unit

    override fun exec(): Boolean {
        table1.merge(table2)
        return true
    }

    override fun setRawResult(value: Unit) = Unit
}