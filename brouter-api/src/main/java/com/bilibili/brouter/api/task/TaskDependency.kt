package com.bilibili.brouter.api.task

import com.bilibili.brouter.api.internal.TaskLike

interface TaskDependency {
    /**
     *
     * Determines the dependencies for the given [Task]. This method is called when BRouter assembles the task
     * execution graph for a build.
     *
     * @param task The task to determine the dependencies for.
     * @return The tasks which the given task depends on. Returns an empty set if the task has no dependencies.
     */
    fun getDependencies(task: Task?): Set<TaskLike>
}


interface TaskDependencyContainer {
    /**
     * Adds the dependencies from this container to the given context. Failures to calculate the build dependencies are supplied to the context.
     */
    fun visitDependencies(context: TaskDependencyResolveContext)
}

interface TaskDependencyResolveContext {
    fun add(dependency: Any)
    fun addAll(dependencies: Iterable<Any>)
    fun addAll(vararg dependencies: Any)
    val task: Task?
}