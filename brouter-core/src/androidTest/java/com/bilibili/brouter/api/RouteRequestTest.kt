package com.bilibili.brouter.api

import android.net.Uri
import android.os.Parcel
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteRequestTest {

    @Test
    fun testParse() {
        val str = "http://test?q=1,1"
        val r = str.toRouteRequest()
        Assert.assertEquals(r.uniformUri, r.pureUri)
        Assert.assertEquals(r.uniformUri.toString(), str)
        Assert.assertEquals(r.params["q"], "1,1")
        Assert.assertEquals(r.uniformUri.encodedQuery, "q=1,1")
    }

    @Test
    fun testProtocol() {
        val str = "http://test?-Pq=1&-Pup.types=${StandardRouteType.WEB}"
        val r = str.toRouteRequest()
        Assert.assertEquals(r.props["q"], "1")
        Assert.assertEquals(r.routeTypes, listOf(StandardRouteType.WEB))

        val r2 = r.newBuilder()
            .routeTypes(listOf(StandardRouteType.NATIVE, StandardRouteType.WEB))
            .props {
                it["q"] = "2"
            }
            .params {
                it["q"] = "2"
            }
            .build()
        Assert.assertEquals(
            r2.uniformUri.getQueryParameters("-Pup.types"),
            listOf(StandardRouteType.NATIVE, StandardRouteType.WEB)
        )
        Assert.assertEquals(r2.uniformUri.getQueryParameter("-Pq"), "2")
        Assert.assertEquals(r2.uniformUri.getQueryParameter("q"), "2")
    }

    @Test
    fun testPrev() {
        val req =
            "http://test1?-Pup.prev=${Uri.encode("http://test2")}&-Pup.forward=${Uri.encode("http://test3")}".toRouteRequest()
        Assert.assertEquals(req.targetUri.toString(), "http://test1")
        Assert.assertEquals(req.prev!!.targetUri.toString(), "http://test2")
        Assert.assertEquals(req.forward!!.targetUri.toString(), "http://test3")
    }

    @Test
    fun testParcel() {
        val req =
            "http://test1?-Pup.prev=${Uri.encode("http://test2")}&-Pup.forward=${Uri.encode("http://test3")}".toRouteRequest()
        val parcel = Parcel.obtain()
        parcel.writeParcelable(req, 0)
        parcel.setDataPosition(0)
        val req2 = parcel.readParcelable<RouteRequest>(RouteRequest::class.java.classLoader)
        Assert.assertNotNull(req2)
        Assert.assertEquals(req2!!.targetUri, req.targetUri)
    }

    @Test
    fun testMutable() {
        val req = Uri.parse("ok://test").toBuilder()
            .params {
                it.put("q", "1")
            }
            .build()
        Assert.assertEquals(req.params["q"], "1")
        val req2 = req.newBuilder()
            .params {
                it.clear()
                it.put("uri", req.pureUri.toString())
            }
            .targetUri(Uri.parse("ok://test2"))
            .build()
        Assert.assertEquals(req.params["q"], "1")
        Assert.assertEquals(req2.params["uri"], "ok://test?q=1")
        Assert.assertEquals(req2.targetUri, Uri.parse("ok://test2"))
    }

    @Test
    fun testAttr() {
        val builder = Uri.parse("ok://test").toBuilder()
            .attributes {
                it.attribute("q", "1")
                it.attribute("q", "2")
            }
        val req1 = builder.build()

        builder.attributes.attribute("q", "3")

        val req2 = req1.newBuilder()
            .attributes {
                it.attribute("q", "4")
            }
            .build()

        Assert.assertEquals(req2.attributes.getAttribute("q"), "4")
        Assert.assertEquals(builder.attributes.getAttribute("q"), "3")
        Assert.assertEquals(req1.attributes.getAttribute("q"), "2")
        Assert.assertEquals(req1.uniformUri.toString(), "ok://test?-Aq=2")
    }
}