package com.bilibili.brouter.core.internal.task

import com.bilibili.brouter.api.internal.TaskLike
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskDependency


interface WorkDependencyResolver<T> {
    /**
     * Resolves dependencies to a specific type.
     *
     * @return `true` if this resolver could resolve the given node, `false` otherwise.
     */
    fun resolve(
        task: Task?,
        node: Any,
        resolveAction: (T) -> Unit
    ): Boolean

    companion object {
        /**
         * Resolves dependencies to [Task] objects.
         */
        val TASK_AS_TASK: WorkDependencyResolver<TaskLike> =
            object : WorkDependencyResolver<TaskLike> {
                override fun resolve(
                    task: Task?,
                    node: Any,
                    resolveAction: (TaskLike) -> Unit
                ): Boolean {
                    if (node is TaskDependency) {
                        for (dependencyTask in node.getDependencies(task)) {
                            resolveAction(dependencyTask)
                        }
                        return true
                    }
                    if (node is TaskLike) {
                        resolveAction(node)
                        return true
                    }
                    return false
                }
            }
    }
}


internal interface Buildable {
    val dependencies: TaskDependency
}