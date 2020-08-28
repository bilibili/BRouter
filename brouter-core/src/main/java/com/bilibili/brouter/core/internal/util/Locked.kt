package com.bilibili.brouter.core.internal.util

import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class Locked<T : Enum<T>>(val clazz: Class<T>, initialValue: T) {
    var value: T = initialValue
    val lock: Lock = ReentrantLock()
    private val awaitCondition = lock.newCondition()

    private val oneShotListeners = EnumMap<T, MutableList<() -> Unit>>(clazz)

    fun tryIncreaseTo(target: T): Boolean {
        var listeners: List<() -> Unit>? = emptyList()
        ifLessThanTarget(target) {
            this.value = target
            listeners = oneShotListeners.remove(target)
            awaitCondition.signalAll()
        }
        listeners?.forEach {
            it()
        }
        return listeners !== emptyList<() -> Unit>()
    }

    fun whenAtLeast(target: T, action: () -> Unit) {
        if (this.value < target) {
            lock.withLock {
                if (this.value < target) {
                    oneShotListeners.getOrPut(target) {
                        mutableListOf()
                    } += action
                    return
                }
            }
        }
        action()
    }

    fun awaitAtLeast(target: T) {
        ifLessThanTarget(target) {
            do {
                awaitCondition.await()
            } while (this.value < target)
        }
    }

    inline fun ifLessThanTarget(target: T, action: () -> Unit) {
        if (this.value < target) {
            lock.withLock {
                if (this.value < target) {
                    action()
                }
            }
        }
    }
}