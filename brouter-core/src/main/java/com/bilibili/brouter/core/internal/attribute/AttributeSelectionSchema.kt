package com.bilibili.brouter.core.internal.attribute

import com.bilibili.brouter.api.AttributeContainer


interface AttributeSelectionSchema {

    fun matchValue(attributeName: String, requested: String, candidate: String): Boolean

    fun disambiguate(
        attributeName: String,
        requested: String?,
        candidates: Set<String>
    ): Set<String>

    fun collectExtraAttributes(
        candidatesAttributes: List<AttributeContainer>,
        requested: AttributeContainer
    ): Array<String>
}