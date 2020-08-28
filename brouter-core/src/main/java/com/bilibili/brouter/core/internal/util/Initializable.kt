package com.bilibili.brouter.core.internal.util

abstract class Initializable {
    protected var initialized = false

    open fun markInitialized() {
        requireNonInitialized(innerErrorMsg)
        initialized = true
    }

    fun requireNonInitialized(msg: (() -> Any)? = null) {
        require(!initialized, msg ?: innerErrorMsg)
    }

    fun requireInitialized(msg: (() -> Any)? = null) {
        require(initialized, msg ?: innerErrorMsg)
    }

    private val innerErrorMsg: () -> Any
        get() = {
            "Already initialized!"
        }
}