package com.bilibili.brouter.plugin.internal.tasks

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider


internal inline fun <reified T : Task> TaskContainer.register(
    action: TaskConfigureAction<in T>
): TaskProvider<T> {
    val wrapper =
        TaskActionWrapper(action)
    return this.register(action.taskName, T::class.java, wrapper).apply {
        wrapper.afterCreate(this)
    }
}

internal interface TaskConfigureAction<T : Task> : Action<T> {

    val taskName: String

    fun preConfigure(taskName: String) {
    }

    override fun execute(t: T)
}

internal class TaskActionWrapper<T : Task>(private val action: TaskConfigureAction<in T>) : Action<T> {
    private var preConfigure = false

    override fun execute(t: T) {
        if (!preConfigure) {
            action.preConfigure(t.name)
        }
        t.group = "brouter"
        action.execute(t)
    }

    fun afterCreate(taskProvider: TaskProvider<T>) {
        if (!preConfigure) {
            action.preConfigure(taskProvider.name)
        }
    }
}