package com.bilibili.brouter.core.internal.config

import android.app.Application
import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskExecutionListener
import com.bilibili.brouter.core.defaults.DefaultGlobalLauncher
import com.bilibili.brouter.core.internal.attribute.DefaultAttributeSchema
import com.bilibili.brouter.core.internal.attribute.InternalAttributeSchema
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.collections.ArrayList


internal class ConfigurationImpl private constructor(builder: Builder) :
    InternalGlobalConfiguration {

    override val mutablePreMatchInterceptors: MutableList<RouteInterceptor> =
        CopyOnWriteArrayList(builder.preMatchInterceptors)
    override val mutablePostMatchInterceptors: MutableList<RouteInterceptor> =
        CopyOnWriteArrayList(builder.postMatchInterceptors)
    override val attributeSchema: InternalAttributeSchema = builder.attributeSchema
    override var preMatchInterceptors: List<RouteInterceptor> =
        Collections.unmodifiableList(mutablePreMatchInterceptors)
    override var postMatchInterceptors: List<RouteInterceptor> =
        Collections.unmodifiableList(mutablePostMatchInterceptors)
    override val logger: SimpleLogger = builder.logger
    override val emptyRouteTypeHandler: EmptyTypeHandler = builder.handler
    override val authenticator: RouteAuthenticator = builder.authenticator
    override val executor: ExecutorService = builder.executor ?: Executors.newCachedThreadPool(
        BRouterThreadFactory
    )
    override val servicesMissFactory: OnServicesMissFactory = builder.servicesMissFactory
    override val routerListenerFactory: RouteListener.Factory = builder.routerListenerFactory
    override val moduleMissingReactor: ModuleMissingReactor = builder.moduleMissingReactor
    override val defaultScheme: String = builder.defaultScheme
    override val globalLauncher: GlobalLauncher = builder.globalLauncher
    override val taskExecutionListener: TaskExecutionListener = builder.taskExecutionListener
    override val taskComparator: Comparator<Task> = builder.taskComparator
    override val app: Application = builder.app
    override fun newBuilder(): InternalGlobalConfiguration.Builder = Builder(this)

    internal class Builder : InternalGlobalConfiguration.Builder {
        override val preMatchInterceptors: MutableList<RouteInterceptor>
        override val postMatchInterceptors: MutableList<RouteInterceptor>
        override val attributeSchema: InternalAttributeSchema
        internal val app: Application
        internal var authenticator: RouteAuthenticator
        internal var handler: EmptyTypeHandler
        internal var servicesMissFactory: OnServicesMissFactory
        internal var logger: SimpleLogger
        internal var executor: ExecutorService?
        internal var routerListenerFactory: RouteListener.Factory
        internal var moduleMissingReactor: ModuleMissingReactor
        internal var defaultScheme: String
        internal var globalLauncher: GlobalLauncher
        internal var taskExecutionListener: TaskExecutionListener
        internal var taskComparator: Comparator<Task>

        constructor(app: Application) {
            this.app = app
            preMatchInterceptors = arrayListOf()
            postMatchInterceptors = arrayListOf()
            attributeSchema = DefaultAttributeSchema()
            authenticator = RouteAuthenticator.NONE
            handler = EmptyTypeHandler.DEFAULT
            servicesMissFactory = OnServicesMissFactory.REFLECTION
            logger = SimpleLogger.ANDROID
            executor = null
            routerListenerFactory = RouteListener.factory(RouteListener())
            moduleMissingReactor = ModuleMissingReactor.EMPTY
            defaultScheme = "brouter"
            globalLauncher = DefaultGlobalLauncher()
            taskExecutionListener = TaskExecutionListener.EMPTY
            taskComparator = Comparator { o1, o2 -> o1.priority - o2.priority }
        }

        constructor(configuration: ConfigurationImpl) {
            app = configuration.app
            preMatchInterceptors = ArrayList(configuration.mutablePreMatchInterceptors)
            postMatchInterceptors = ArrayList(configuration.mutablePostMatchInterceptors)
            attributeSchema = configuration.attributeSchema
            authenticator = configuration.authenticator
            handler = configuration.emptyRouteTypeHandler
            servicesMissFactory = configuration.servicesMissFactory
            logger = configuration.logger
            executor = configuration.executor
            routerListenerFactory = configuration.routerListenerFactory
            moduleMissingReactor = configuration.moduleMissingReactor
            defaultScheme = configuration.defaultScheme
            globalLauncher = configuration.globalLauncher
            taskExecutionListener = configuration.taskExecutionListener
            taskComparator = configuration.taskComparator
        }

        override fun defaultScheme(defaultScheme: String): InternalGlobalConfiguration.Builder =
            this.apply {
                this.defaultScheme = defaultScheme
            }

        override fun logger(logger: SimpleLogger): InternalGlobalConfiguration.Builder =
            this.apply {
                this.logger = logger
            }

        override fun emptyRouteTypeHandler(handler: EmptyTypeHandler): InternalGlobalConfiguration.Builder =
            this.apply {
                this.handler = handler
            }

        override fun servicesMissFactory(factory: OnServicesMissFactory): InternalGlobalConfiguration.Builder =
            this.apply {
                this.servicesMissFactory = factory
            }

        override fun authenticator(authenticator: RouteAuthenticator): InternalGlobalConfiguration.Builder =
            this.apply {
                this.authenticator = authenticator
            }

        override fun addPreMatchInterceptor(interceptor: RouteInterceptor): InternalGlobalConfiguration.Builder =
            this.apply {
                this.preMatchInterceptors.add(interceptor)
            }

        override fun addPostMatchInterceptor(interceptor: RouteInterceptor): InternalGlobalConfiguration.Builder =
            this.apply {
                this.postMatchInterceptors.add(interceptor)
            }

        override fun routeListener(listener: RouteListener): InternalGlobalConfiguration.Builder =
            routeListenerFactory(RouteListener.factory(listener))

        override fun routeListenerFactory(factory: RouteListener.Factory): InternalGlobalConfiguration.Builder =
            this.apply {
                routerListenerFactory = factory
            }

        override fun executor(executor: ExecutorService): InternalGlobalConfiguration.Builder =
            this.apply {
                this.executor = executor
            }

        override fun moduleMissingReactor(moduleMissingReactor: ModuleMissingReactor): InternalGlobalConfiguration.Builder =
            this.apply {
                this.moduleMissingReactor = moduleMissingReactor
            }

        override fun attributeSchema(action: (AttributeSchema) -> Unit): InternalGlobalConfiguration.Builder =
            this.apply {
                action(attributeSchema)
            }

        override fun globalLauncher(globalLauncher: GlobalLauncher): InternalGlobalConfiguration.Builder =
            this.apply {
                this.globalLauncher = globalLauncher
            }

        override fun taskExecutionListener(taskExecutionListener: TaskExecutionListener): InternalGlobalConfiguration.Builder =
            this.apply {
                this.taskExecutionListener = taskExecutionListener
            }

        override fun taskComparator(comparator: Comparator<Task>): InternalGlobalConfiguration.Builder =
            this.apply {
                this.taskComparator = comparator
            }

        override fun build(): InternalGlobalConfiguration = ConfigurationImpl(this)
    }
}

private object BRouterThreadFactory : ThreadFactory {
    private val threadNumber = AtomicInteger(1)
    override fun newThread(r: Runnable): Thread {
        val t = Thread(r, "brouter-" + threadNumber.getAndIncrement())
        if (t.isDaemon) t.isDaemon = false
        if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
        return t
    }
}