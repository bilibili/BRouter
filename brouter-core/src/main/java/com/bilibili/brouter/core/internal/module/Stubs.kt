package com.bilibili.brouter.core.internal.module

import com.bilibili.brouter.core.internal.attribute.HasAttributesContainer
import com.bilibili.brouter.core.internal.routes.StubRoutesImpl
import com.bilibili.brouter.core.internal.table.Table
import com.bilibili.brouter.model.StubModuleMeta

internal fun StubModuleMeta.register(registry: Table) {
    val routeTable = registry.routeTable
    routes!!.forEach {
        routeTable.registerRoutes(
            StubRoutesImpl(
                it.name,
                it.routes,
                it.routeType,
                it.attributes,
                this.name
            ),
            HasAttributesContainer.FLAG_ALLOW_OVERRIDE
        )
    }
}

