package com.bilibili.brouter.core.internal.task

import com.bilibili.brouter.api.Module
import com.bilibili.brouter.api.ServiceCentral
import com.bilibili.brouter.api.internal.TaskLike
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import com.bilibili.brouter.api.task.*
import com.bilibili.brouter.core.internal.module.NoModule
import com.bilibili.brouter.core.internal.util.Locked
import java.util.*


internal abstract class AbstractTask(
    override val name: String,
    override val priority: Int,
    override val threadMode: ThreadMode,
    override var actions: List<(Task) -> Unit>,
    private val container: DefaultTaskContainer,
    dependencies: List<Any>
) : TaskInternal, TaskOutputs {

    override val module: ModuleInternal
        get() = container.module

    override val dependencies: TaskDependency =
        if (dependencies.isEmpty()) NoDependency
        else DefaultTaskDependency(module, container.central, dependencies)

    private val locked = Locked(TaskStatus::class.java, TaskStatus.UNKNOWN)

    override val status get() = locked.value

    init {
        locked.whenAtLeast(TaskStatus.EXECUTED) {
            container.markTombstone(name)
        }
    }

    override val services: ServiceCentral
        get() = container.central.services

    override val outputs: TaskOutputs get() = this

    override fun <T : Any> output(clazz: Class<T>, name: String, t: T) {
        container.central.services.fill(clazz, name, module, t)
    }

    override fun submit() {
        container.central.submit(this)
    }

    /**
     * In fact, it will do stealing.
     */
    override fun awaitComplete() {
        container.central.stealing(this)
    }

    override fun whenComplete(action: (Task) -> Unit) {
        whenStatusAtLeast(TaskStatus.EXECUTED, action)
    }

    override fun awaitStatusAtLeast(status: TaskStatus) {
        locked.awaitAtLeast(status)
    }

    override fun whenStatusAtLeast(status: TaskStatus, action: (Task) -> Unit) {
        locked.whenAtLeast(status) {
            action(this)
        }
    }


    override fun setStatus(status: TaskStatus): Boolean {
        return locked.tryIncreaseTo(status)
    }


    override fun toString(): String {
        return "Task(name='$name', priority=$priority, threadMode=$threadMode, status=$status, actions=$actions, dependencies=$dependencies)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractTask) return false

        if (name != other.name) return false
        if (module != other.module) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + module.hashCode()
        return result
    }


}

internal class DefaultTaskDependency(
    private val module: Module,
    private val central: TaskCentral,
    private val dependencies: List<Any>
) : TaskDependency, TaskDependencyContainer {

    override fun getDependencies(task: Task?): Set<TaskLike> {
        val context =
            CachingTaskDependencyResolveContext(false, WorkDependencyResolver.TASK_AS_TASK)
        return context.getDependencies(task, this)
    }

    override fun visitDependencies(context: TaskDependencyResolveContext) {
        val queue: Deque<Any> = ArrayDeque(dependencies)
        queue.addAll(dependencies)
        while (queue.isNotEmpty()) {
            val dependency = queue.removeLast()
            if (dependency is TaskLike || dependency is TaskDependency) {
                context.add(dependency)
            } else if (dependency is ServiceDependency) {
                val buildable = central.services.getDependencies(dependency.clazz, dependency.name)
                if (buildable != null) {
                    context.add(buildable.dependencies)
                } else if (!dependency.optional) {
                    error("Service ${dependency.clazz} named '${dependency.name}' not exists.")
                }
            } else if (dependency is TaskDependencyContainer) {
                dependency.visitDependencies(context)
            } else if (dependency is Iterable<*>) {
                queue.addAll(dependency)
            } else if (dependency is Array<*>) {
                queue.addAll(dependency.asList())
            } else if (dependency is CharSequence) {
                context.add(central.resolveTask(module, dependency.toString()))
            } else {
                error(
                    "Supported type: Task, " +
                            "TaskDependency, " +
                            "TaskDependencyContainer, " +
                            "Iterable<*>, " +
                            "Array<*>, " +
                            "ServiceDependency, " +
                            "CharSequence. " +
                            "But meet ${dependency}."
                )
            }
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultTaskDependency) return false

        if (module != other.module) return false
        if (dependencies != other.dependencies) return false

        return true
    }

    override fun hashCode(): Int {
        var result = module.hashCode()
        result = 31 * result + dependencies.hashCode()
        return result
    }

    override fun toString(): String {
        return "TaskDependency(module=$module, dependencies=$dependencies)"
    }

}

internal object NoDependency : TaskDependency, TaskDependencyContainer {
    override fun getDependencies(task: Task?): Set<TaskLike> = emptySet()

    override fun visitDependencies(context: TaskDependencyResolveContext) = Unit
}

internal class DefaultTask(
    name: String,
    priority: Int, threadMode: ThreadMode, actions: List<(Task) -> Unit>,
    container: DefaultTaskContainer, dependencies: List<Any>
) : AbstractTask(name, priority, threadMode, actions, container, dependencies) {

    internal class Builder(
        private val container: DefaultTaskContainer,
        internal val name: String
    ) : Task.Builder {

        private var priority = 0
        private var threadMode = ThreadMode.ANY
        private val actions = mutableListOf<(Task) -> Unit>()
        internal val dependencies = mutableListOf<Any>()
        override fun threadMode(mode: ThreadMode): Task.Builder = this.apply {
            this.threadMode = mode
        }

        override fun priority(priority: Int): Task.Builder = this.apply {
            this.priority = priority
        }

        override fun doLast(action: (Task) -> Unit): Task.Builder = this.apply {
            this.actions.add(action)
        }

        override fun doFirst(action: (Task) -> Unit): Task.Builder = this.apply {
            this.actions.add(0, action)
        }

        override fun dependsOn(vararg any: Any): Task.Builder = this.apply {
            this.dependencies.addAll(any)
        }

        override fun build(): TaskInternal {
            return DefaultTask(
                name,
                priority,
                threadMode,
                actions.toList(),
                container,
                dependencies.toList()
            )
        }
    }
}

internal object ExecutedStubTask : TaskInternal {
    override fun setStatus(status: TaskStatus): Boolean = false
    override fun awaitStatusAtLeast(status: TaskStatus) {
    }

    override fun whenStatusAtLeast(status: TaskStatus, action: (Task) -> Unit) {
        action(this)
    }

    override fun awaitComplete() {
    }

    override fun whenComplete(action: (Task) -> Unit) {
        action(this)
    }

    override val actions: List<(Task) -> Unit> get() = emptyList()
    override val name: String get() = "ExecutedStubTask"
    override val module: Module get() = NoModule
    override val priority: Int get() = 0
    override val threadMode: ThreadMode get() = ThreadMode.ANY
    override val status: TaskStatus get() = TaskStatus.EXECUTED
    override val dependencies: TaskDependency get() = NoDependency
    override fun submit() {
    }

    override val services: ServiceCentral get() = throw UnsupportedOperationException()
    override val outputs: TaskOutputs get() = throw UnsupportedOperationException()
}