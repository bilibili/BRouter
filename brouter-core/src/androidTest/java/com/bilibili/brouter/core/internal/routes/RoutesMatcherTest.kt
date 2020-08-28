package com.bilibili.brouter.core.internal.routes

import android.support.test.runner.AndroidJUnit4
import com.bilibili.brouter.api.AttributeContainer
import com.bilibili.brouter.api.RouteResponse
import com.bilibili.brouter.api.StandardRouteType
import com.bilibili.brouter.api.internal.emptyArrayProvider
import com.bilibili.brouter.api.internal.emptyAttributesArray
import com.bilibili.brouter.api.internal.incubating.RouteInfoInternal
import com.bilibili.brouter.api.internal.routesBean
import com.bilibili.brouter.api.internal.stubLauncherProvider
import com.bilibili.brouter.api.toRouteRequest
import com.bilibili.brouter.common.util.matcher.RawSegmentsParser
import com.bilibili.brouter.core.internal.attribute.AttributeMatcher
import com.bilibili.brouter.core.internal.module.NoModule
import com.bilibili.brouter.core.internal.table.RouteTable
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

@RunWith(AndroidJUnit4::class)
class RoutesMatcherTest {
    private val stub = Provider<Class<*>> {
        Provider::class.java
    }

    internal val routeCentral = RouteManager()

    init {
        val parser = RawSegmentsParser("coffee")
        RouteCapture.parser = parser
        val table = RouteTable(
            object : AttributeMatcher<SingleRouteRef> {
                override fun matches(
                    requested: AttributeContainer,
                    candidates: List<SingleRouteRef>
                ): List<SingleRouteRef> {
                    return candidates
                }
            }, parser
        )
        routeCentral.attachTable(table, "coffee")
        routeCentral.dynamicRegisterRoutes(
            routesBean(
                "route1",
                arrayOf(
                    "{scheme}://**",
                    "coffee://*.tv/watch/av{id}xx/x{id2}x",
                    "coffee://s1",
                    "s1/s2"
                ),
                StandardRouteType.NATIVE,
                emptyAttributesArray(),
                emptyArrayProvider(),
                stubLauncherProvider(),
                stub,
                NoModule
            )
        )
        routeCentral.dynamicRegisterRoutes(
            routesBean(
                "route2",
                arrayOf(
                    "coffee://article/editor",
                    "coffee://article/{id}"
                ),
                StandardRouteType.NATIVE,
                emptyAttributesArray(),
                emptyArrayProvider(),
                stubLauncherProvider(),
                stub,
                NoModule
            )
        )
        routeCentral.dynamicRegisterRoutes(
            routesBean(
                "route3",
                arrayOf(
                    "(http|https)://dieyidezui.com"
                ),
                StandardRouteType.WEB,
                emptyAttributesArray(),
                emptyArrayProvider(),
                stubLauncherProvider(),
                stub,
                NoModule
            )
        )
    }

    @Test
    fun testOpaqueUri() {
        val r = routeCentral.findRoute(
            "mailto:test@coffeepartner".toRouteRequest(),
            StandardRouteType.NATIVE
        )
        Assert.assertEquals(r.code, RouteResponse.Code.UNSUPPORTED)
    }

    @Test
    fun testSpecialUri() {
        val r1 = routeCentral.findRoute("s1/s2".toRouteRequest(), StandardRouteType.NATIVE)
        val r2 = routeCentral.findRoute("coffee://s1/s2".toRouteRequest(), StandardRouteType.NATIVE)
        val r3 = routeCentral.findRoute("//s1/s2".toRouteRequest(), StandardRouteType.NATIVE)
        Assert.assertTrue(r1.code == RouteResponse.Code.OK)
        Assert.assertTrue(r2.code == RouteResponse.Code.OK)
        Assert.assertTrue(r3.code == RouteResponse.Code.OK)
        Assert.assertTrue((r1.routeInfo as RouteInfoInternal).routes == (r2.routeInfo as RouteInfoInternal).routes)
        Assert.assertTrue((r2.routeInfo as RouteInfoInternal).routes == (r3.routeInfo as RouteInfoInternal).routes)
    }

    @Test
    fun testPriority() {
        with(routeCentral) {
            Assert.assertEquals(
                "route2",
                findRoute(
                    "coffee://article/editor".toRouteRequest(),
                    StandardRouteType.NATIVE
                ).routeInfo?.routeName
            )
            Assert.assertEquals(
                "123",
                findRoute(
                    "coffee://article/123".toRouteRequest(),
                    StandardRouteType.NATIVE
                ).routeInfo!!.captures["id"]
            )
        }
    }

    @Test
    fun testPrefixPath() {
        with(routeCentral) {
            with(findRoute("http://test.com".toRouteRequest(), StandardRouteType.NATIVE)) {
                Assert.assertEquals(code, RouteResponse.Code.OK)
                Assert.assertEquals(routeInfo!!.captures[""], "test.com")
                Assert.assertEquals(routeInfo!!.captures["scheme"], "http")
            }
        }
    }

    @Test
    fun testWildcardPath() {
        with(routeCentral) {
            with(
                findRoute(
                    "coffee://omg.tv/watch/av123xx/xox".toRouteRequest(),
                    StandardRouteType.NATIVE
                )
            ) {
                val info = routeInfo!!
                Assert.assertEquals(info.captures["id"], "123")
                Assert.assertEquals(info.captures["id2"], "o")
            }
        }
    }
}