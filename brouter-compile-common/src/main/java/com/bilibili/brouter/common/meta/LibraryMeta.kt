package com.bilibili.brouter.common.meta

data class LibraryMeta(
    val modules: List<ModuleMeta>,
    val consumers: List<ServiceConsumerClass>
)