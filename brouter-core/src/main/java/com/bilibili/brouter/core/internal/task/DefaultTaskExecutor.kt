package com.bilibili.brouter.core.internal.task

import android.os.Looper
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskExecutionListener
import com.bilibili.brouter.api.task.TaskStatus
import com.bilibili.brouter.api.task.ThreadMode
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.getOrSet

/**
 * 实现一个基于优先级的 Work Stealing 算法
 * 每个 await 状态的 module，均会尝试偷取当前 module 依赖链的任务来执行以促使尽快完成。
 */
internal class DefaultTaskExecutor(
    private val listener: TaskExecutionListener,
    taskComparator: Comparator<Task>,
    asyncExecutor: Executor,
    mainExecutor: Executor,
    parallelism: Int
) : TaskExecutor, NodeExecutor {

    private val nodeComparator: Comparator<TaskNode> =
        Comparator { o1, o2 ->
            taskComparator.compare(o1.task, o2.task)
        }

    private val asyncDispatcher =
        ExecutorNodeDispatcher(
            asyncExecutor,
            parallelism,
            ThreadMode.ASYNC,
            PriorityBlockingQueue(11, nodeComparator)
        )
    private val mainDispatcher =
        ExecutorNodeDispatcher(
            mainExecutor,
            1,
            ThreadMode.MAIN,
            PriorityBlockingQueue(8, nodeComparator)
        )

    override fun submit(node: TaskNode) {
        when (node.task.threadMode) {
            ThreadMode.ANY, ThreadMode.ASYNC -> asyncDispatcher(node, this)
            ThreadMode.MAIN -> mainDispatcher(node, this)
        }
    }

    override fun stealing(node: TaskNode) {
        while (true) {
            when (node.task.status) {
                TaskStatus.EXECUTED -> {
                    return
                }
                TaskStatus.WAITING, TaskStatus.PENDING -> {
                    doStealing(node)
                }
                TaskStatus.EXECUTING -> {
                    node.task.awaitStatusAtLeast(TaskStatus.EXECUTED)
                }
                TaskStatus.UNKNOWN -> {
                    // spin?
                }
            }
        }
    }

    private fun doStealing(node: TaskNode) {
        StealingDispatcher(
            if (Looper.myLooper() === Looper.getMainLooper()) {
                ThreadMode.MAIN
            } else {
                ThreadMode.ANY
            }, PriorityBlockingQueue(3, nodeComparator)
        )(node, this)

        node.task.awaitStatusAtLeast(TaskStatus.EXECUTED)
    }

    override fun invoke(plan: ExecutionPlan, startNode: TaskNode?, context: TaskContext) {
        var node = startNode ?: plan.selectNext(null) ?: return
        do {
            val task = node.task
            if (task.setStatus(TaskStatus.EXECUTING)) {
                listener.beforeExecute(task)

                context.runningTasks += node
                node.executeActions()
                task.setStatus(TaskStatus.EXECUTED)
                context.runningTasks.removeAt(context.runningTasks.lastIndex)

                listener.afterExecute(task)

                node.dependencySuccessors.forEach {
                    if (it.checkReady()) {
                        submit(it)
                    }
                }
                node.dependencySuccessors = emptySet()
                node.dependencyPredecessors = emptySet()
            }
            node = plan.selectNext(node) ?: return
        } while (true)
    }
}

internal data class TaskContext(
    val threadMode: ThreadMode,
    val runningTasks: MutableList<TaskNode> = arrayListOf()
)

internal object ThreadLocalContext {
    private val taskContext = ThreadLocal<TaskContext>()

    inline fun <T> withContext(mode: ThreadMode, action: (TaskContext) -> T): T {
        var clear = false
        val context = taskContext.getOrSet {
            clear = true
            TaskContext(mode)
        }

        try {
            return action(context)
        } finally {
            if (clear) {
                taskContext.remove()
            }
        }
    }
}

internal interface NodeExecutor : (ExecutionPlan, TaskNode?, TaskContext) -> Unit

internal interface ExecutionPlan {
    fun selectNext(node: TaskNode?): TaskNode?
}

internal interface NodeDispatcher : (TaskNode, NodeExecutor) -> Unit, ExecutionPlan

internal open class ExecutorNodeDispatcher(
    private val executor: Executor,
    private val parallelism: Int,
    private val mode: ThreadMode,
    private val queue: Queue<TaskNode>
) : NodeDispatcher {

    private var workers = AtomicInteger(0)

    override fun invoke(node: TaskNode, nodeExecutor: NodeExecutor) {
        if (workers.get() < parallelism) {
            if (addWorker(node, nodeExecutor))
                return
        }
        queue.offer(node)
        if (workers.get() < parallelism) {
            addWorker(null, nodeExecutor)
        }
    }

    private fun addWorker(
        node: TaskNode?,
        nodeExecutor: NodeExecutor
    ): Boolean {
        while (true) {
            val workerCount = workers.get()
            if (workerCount >= parallelism) {
                return false
            }
            if (workers.compareAndSet(workerCount, workerCount + 1)) {
                executor.execute {
                    ThreadLocalContext.withContext(mode) {
                        nodeExecutor(this, node, it)
                    }
                }
                return true
            }
        }
    }

    override fun selectNext(node: TaskNode?): TaskNode? {
        while (true) {
            val r = queue.poll()
            if (r != null) return r
            val workerCount = workers.get()
            if (workers.compareAndSet(workerCount, workerCount - 1)) {
                return null
            }
        }
    }
}

internal class StealingDispatcher(
    val mode: ThreadMode,
    private val queue: BlockingQueue<TaskNode>
) : NodeDispatcher {

    private var remaining = 0

    override fun invoke(node: TaskNode, nodeExecutor: NodeExecutor) {
        ThreadLocalContext.withContext(mode) { context ->
            if (!node.isExecutedOrExecuting) {
                val pendingTasks = HashSet<TaskNode>()
                node.collectStealingTasks(mode, pendingTasks)
                if (context.runningTasks.any {
                        pendingTasks.contains(it)
                    }) {
                    error("Found circle, running tasks: ${context.runningTasks}, stealing tasks: ${pendingTasks}.")
                }

                remaining = pendingTasks.size

                pendingTasks.forEach { n ->
                    n.task.whenStatusAtLeast(TaskStatus.PENDING) {
                        queue.add(n)
                    }
                }
                nodeExecutor(this, null, context)
            }
        }
    }

    override fun selectNext(node: TaskNode?): TaskNode? {
        return if (remaining-- > 0) {
            queue.take()
        } else {
            null
        }
    }
}