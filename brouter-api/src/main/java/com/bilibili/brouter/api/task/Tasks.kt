package com.bilibili.brouter.api.task

import com.bilibili.brouter.api.Module
import com.bilibili.brouter.api.ServiceCentral
import com.bilibili.brouter.api.internal.HasInternalProtocol
import com.bilibili.brouter.api.internal.TaskLike
import kotlin.reflect.KClass


enum class ThreadMode {
    ANY {
        override fun matchTaskMode(threadMode: ThreadMode) = threadMode === ANY
    },
    MAIN {
        override fun matchTaskMode(threadMode: ThreadMode): Boolean =
            threadMode === ANY || threadMode === MAIN
    },
    ASYNC {
        override fun matchTaskMode(threadMode: ThreadMode): Boolean =
            threadMode === ANY || threadMode === ASYNC
    };

    abstract fun matchTaskMode(threadMode: ThreadMode): Boolean
}

enum class TaskStatus {
    FAILURE,
    UNKNOWN,
    WAITING,
    PENDING,
    EXECUTING,
    EXECUTED,
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class TaskOptions(
    /**
     * 任务名
     */
    val name: String,
    /**
     * 优先级
     */
    val priority: Int = 0,

    /**
     * 执行线程
     */
    val threadMode: ThreadMode = ThreadMode.ANY,

    /**
     * 依赖的任务，如果不是本模块的要加上模块名字，如 moduleName.taskName
     */
    val dependencies: Array<String> = []
)

/**
 * 类似于 [com.bilibili.brouter.api.Services]，由 Task 产生的服务
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FIELD)
annotation class TaskOutput(
    vararg val value: KClass<*>,
    val desc: String = ""
)

interface TaskAction {
    fun execute(task: Task)
}

@HasInternalProtocol
interface TaskContainer {
    fun register(name: String)
    fun register(name: String, configure: (Task.Builder) -> Unit)
    fun create(name: String): Task
    fun create(name: String, configure: (Task.Builder) -> Unit): Task
}


interface TaskOutputs {
    fun <T : Any> output(clazz: Class<T>, name: String, t: T)
}

data class ServiceDependency(val clazz: Class<*>, val name: String, val optional: Boolean)

@HasInternalProtocol
interface Task : TaskLike {
    val name: String
    val module: Module
    val priority: Int
    val threadMode: ThreadMode
    val status: TaskStatus

    fun awaitComplete()
    fun whenComplete(action: (Task) -> Unit)
    val dependencies: TaskDependency

    fun submit()

    val services: ServiceCentral
    val outputs: TaskOutputs

    interface Builder {
        fun threadMode(mode: ThreadMode): Builder
        fun priority(priority: Int): Builder
        fun doFirst(action: (Task) -> Unit): Builder
        fun doLast(action: (Task) -> Unit): Builder
        fun dependsOn(vararg any: Any): Builder
        fun build(): Task
    }
}

/**
 * 任务回调
 */
interface TaskExecutionListener {
    fun beforeExecute(task: Task)
    fun afterExecute(task: Task)

    companion object EMPTY : TaskExecutionListener {
        override fun beforeExecute(task: Task) {
        }

        override fun afterExecute(task: Task) {
        }
    }
}