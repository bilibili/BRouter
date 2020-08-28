package com.bilibili.brouter.api.internal.incubating

import com.bilibili.brouter.api.internal.TaskLike
import com.bilibili.brouter.api.task.TaskContainer

/**
 * @author dieyi
 * Created at 2020/5/30.
 */
interface TaskContainerInternal : TaskContainer {

    fun markTombstone(name: String)

    operator fun get(name: String): TaskLike

    fun find(name: String): TaskLike?
}