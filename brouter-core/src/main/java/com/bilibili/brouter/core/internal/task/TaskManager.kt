package com.bilibili.brouter.core.internal.task

import com.bilibili.brouter.api.Module
import com.bilibili.brouter.api.internal.TaskLike
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskStatus
import com.bilibili.brouter.api.task.ThreadMode
import com.bilibili.brouter.core.internal.module.ModuleCentralInternal
import com.bilibili.brouter.core.internal.service.ServiceCentralInternal
import java.util.*

/**
 * 实现一个基于优先级的 Work Stealing 算法
 * 每个 await 状态的 module，均会尝试偷取当前 module 依赖链的任务来执行以促使尽快完成。
 */
internal class TaskManager(
    private val modules: ModuleCentralInternal,
    private val executor: TaskExecutor
) : TaskCentral {
    private val nodes = WeakHashMap<TaskInternal, TaskNode>()
    private val dependencyResolver = CachingTaskDependencyResolveContext(
        true,
        WorkDependencyResolver.TASK_AS_TASK
    )

    override val services: ServiceCentralInternal get() = modules.serviceCentral

    override fun resolveTask(module: Module, name: String): TaskLike {
        val dotIndex = name.indexOf('.')
        val (moduleName, taskName) = if (dotIndex >= 0) {
            name.substring(0, dotIndex) to name.substring(dotIndex + 1)
        } else {
            module.name to name
        }
        val moduleImpl = modules.getModuleImpl(moduleName)
            ?: error("Module '${module.name}' not exists.")
        if (!moduleImpl.tasks.initialized) {
            error("No task in module '${module.name}'.")
        }
        return moduleImpl.tasks[taskName]
    }

    @Synchronized
    override fun submit(task: TaskInternal): TaskNode? {
        return when {
            task.setStatus(TaskStatus.WAITING) -> {
                val node = nodes.getOrPut(task) {
                    TaskNode(task)
                }
                val dependencies = dependencyResolver.getDependencies(task, task.dependencies)
                if (dependencies.isNotEmpty()) {
                    val predecessors = dependencies.mapNotNullTo(hashSetOf()) {
                        if (it is TaskInternal) {
                            submit(it)
                        } else {
                            null
                        }
                    }
                    if (predecessors.isNotEmpty()) {
                        predecessors.forEach {
                            it.dependencySuccessors += node
                        }
                        node.dependencyPredecessors = predecessors
                    }
                }
                if (node.checkReady()) {
                    executor.submit(node)
                }
                node
            }
            else -> {
                nodes[task]
            }
        }
    }


    override fun stealing(any: Any) {
        CachingTaskDependencyResolveContext(false, WorkDependencyResolver.TASK_AS_TASK)
            .getDependencies(null, any)
            .forEach {
                if (it is TaskInternal) {
                    var node = synchronized(this) {
                        nodes[it]
                    }
                    if (node != null) {
                        executor.stealing(node)
                    } else {
                        node = submit(it)
                        if (node != null) {
                            executor.stealing(node)
                        }
                    }
                }
            }
    }
}

internal interface TaskCentral : TaskResolver {

    val services: ServiceCentralInternal

    /**
     * return's null, if the task is executed
     */
    fun submit(task: TaskInternal): TaskNode?

    fun stealing(any: Any)
}

internal interface TaskResolver {
    fun resolveTask(module: Module, name: String): TaskLike
}

internal interface TaskExecutor {
    fun submit(node: TaskNode)
    fun stealing(node: TaskNode)
}

internal interface TaskInternal : Task {
    fun setStatus(status: TaskStatus): Boolean
    fun awaitStatusAtLeast(status: TaskStatus)
    fun whenStatusAtLeast(status: TaskStatus, action: (Task) -> Unit)
    val actions: List<(Task) -> Unit>
}

internal class TaskNode(var task: TaskInternal) {
    var dependencySuccessors = emptySet<TaskNode>()
    var dependencyPredecessors = emptySet<TaskNode>()

    fun collectStealingTasks(currentThreadMode: ThreadMode, out: MutableCollection<TaskNode>) {
        if (isExecutedOrExecuting) return

        dependencyPredecessors.forEach {
            it.collectStealingTasks(currentThreadMode, out)
        }
        if (currentThreadMode.matchTaskMode(task.threadMode)) {
            out += this
        }
    }

    fun checkReady(): Boolean {
        return dependencyPredecessors.all {
            it.isExecuted
        } && task.setStatus(TaskStatus.PENDING)
    }

    fun executeActions() {
        task.actions.forEach {
            it(task)
        }
        task = ExecutedStubTask
    }

    override fun toString(): String {
        return "TaskNode(task=$task, dependencySuccessors=$dependencySuccessors)"
    }


    val isExecutedOrExecuting: Boolean get() = task.status >= TaskStatus.EXECUTING

    private val isExecuted: Boolean get() = task.status >= TaskStatus.EXECUTED
}