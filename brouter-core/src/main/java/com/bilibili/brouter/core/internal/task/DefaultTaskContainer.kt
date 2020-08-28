package com.bilibili.brouter.core.internal.task

import com.bilibili.brouter.api.Module
import com.bilibili.brouter.api.internal.TaskLike
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.api.internal.incubating.TaskContainerInternal
import com.bilibili.brouter.api.task.Task

internal class DefaultTaskContainer(internal val module: ModuleInternal) :
    TaskContainerInternal {

    internal lateinit var central: TaskCentral

    private val pendingMap = mutableMapOf<String, (Task.Builder) -> Unit>()
    private val taskMap = mutableMapOf<String, TaskInternal>()
    private val tombstone = mutableSetOf<String>()


    override fun register(name: String) = register(name, EMPTY_ACTION)

    @Synchronized
    override fun register(name: String, configure: (Task.Builder) -> Unit) {
        if (taskMap.containsKey(name)
            || pendingMap.put(name, configure) != null
        ) {
            duplicated(name)
        }
    }

    override fun create(name: String): TaskInternal = create(name, EMPTY_ACTION)

    @Synchronized
    override fun create(name: String, configure: (Task.Builder) -> Unit): TaskInternal {
        return if (pendingMap.containsKey(name)) {
            duplicated(name)
        } else {
            doCreate(name, configure)
        }
    }

    private fun doCreate(name: String, configure: (Task.Builder) -> Unit): TaskInternal {
        val builder = DefaultTask.Builder(this, name)
        configure(builder)
        return builder.build().apply {
            if (taskMap.put(name, this) != null) {
                duplicated(name)
            }
        }
    }

    @Synchronized
    override fun markTombstone(name: String) {
        taskMap.remove(name) ?: error("No task named '$name'.")
        tombstone += name
    }

    override fun get(name: String): TaskLike =
        find(name) ?: error("Task named '$name' not found.")

    @Synchronized
    override fun find(name: String): TaskLike? {
        return taskMap[name] ?: pendingMap.remove(name)?.let {
            doCreate(name, it)
        } ?: if (tombstone.contains(name)) {
            DefaultTaskStub(name, module)
        } else {
            null
        }
    }

    private fun duplicated(name: String): Nothing {
        error("Duplicated task: $name")
    }

    companion object {
        private val EMPTY_ACTION: (Task.Builder) -> Unit = {
        }
    }
}

class DefaultTaskStub(override val name: String, override val module: Module) : TaskStub {
    override fun toString(): String {
        return "TaskStub(name='$name', module=$module)"
    }
}

internal interface TaskStub : TaskLike {
    val name: String
    val module: Module
}