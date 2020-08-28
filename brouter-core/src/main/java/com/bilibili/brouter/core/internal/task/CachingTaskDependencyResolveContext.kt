package com.bilibili.brouter.core.internal.task

import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskDependencyContainer
import com.bilibili.brouter.api.task.TaskDependencyResolveContext
import com.bilibili.brouter.common.util.graph.CachingDirectedGraphWalker
import com.bilibili.brouter.common.util.graph.DirectedGraph
import java.util.*

class CachingTaskDependencyResolveContext<T>(
    cleanCache: Boolean,
    private vararg val resolvers: WorkDependencyResolver<T>
) :
    TaskDependencyResolveContext {
    private val queue: Deque<Any> =
        ArrayDeque<Any>()
    private val walker =
        CachingDirectedGraphWalker(
            cleanCache,
            TaskGraphImpl()
        )

    override var task: Task? = null
        private set

    @Synchronized
    fun getDependencies(task: Task?, dependencies: Any): Set<T> {
        check(this.task == null)
        this.task = task
        return try {
            walker.add(dependencies)
            walker.findValues()
        } finally {
            queue.clear()
            this.task = null
        }
    }

    override fun add(dependency: Any) {
        queue.add(dependency)
    }

    override fun addAll(dependencies: Iterable<Any>) {
        queue.addAll(dependencies)
    }

    override fun addAll(vararg dependencies: Any) {
        queue.addAll(dependencies)
    }

    private inner class TaskGraphImpl :
        DirectedGraph<Any, T> {

        override fun getNodeValues(
            node: Any,
            values: MutableCollection<in T>,
            connectedNodes: MutableCollection<in Any>
        ) {
            if (node is TaskDependencyContainer) {
                queue.clear()
                node.visitDependencies(this@CachingTaskDependencyResolveContext)
                connectedNodes.addAll(queue)
                return
            }
            resolvers.forEach {
                if (it.resolve(task, node) { e ->
                        values.add(e)
                    })
                    return
            }
            error("Cannot resolve object of unknown type ${node.javaClass.simpleName} to a Task.")
        }
    }
}