package com.bilibili.brouter.model

import com.google.gson.annotations.SerializedName

/**
 * Splits model for another library, it's a secret now.
 */
data class StubModuleMeta(
    @SerializedName("name")
    val name: String,
    @SerializedName("entranceClass")
    val entranceClass: String,
    @SerializedName("stubRoutes")
    val routes: List<StubRoutes>?
)

data class StubRoutes(
    @SerializedName("name")
    val name: String,
    @SerializedName("routes")
    val routes: Array<String>,
    @SerializedName("routeType")
    val routeType: String,
    @SerializedName("attributes")
    val attributes: List<AttributeBean>
)

data class AttributeBean(
    val name: String,
    val value: String
)
