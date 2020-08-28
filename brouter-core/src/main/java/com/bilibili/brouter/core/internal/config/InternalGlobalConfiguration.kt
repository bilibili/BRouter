package com.bilibili.brouter.core.internal.config

import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskExecutionListener
import com.bilibili.brouter.core.internal.attribute.InternalAttributeSchema
import java.util.concurrent.ExecutorService

internal interface InternalGlobalConfiguration : GlobalConfiguration {

    val mutablePreMatchInterceptors: MutableList<RouteInterceptor>
    val mutablePostMatchInterceptors: MutableList<RouteInterceptor>
    val attributeSchema: InternalAttributeSchema

    override fun newBuilder(): Builder

    interface Builder : GlobalConfiguration.Builder {

        override fun defaultScheme(defaultScheme: String): Builder

        override fun logger(logger: SimpleLogger): Builder

        override fun emptyRouteTypeHandler(handler: EmptyTypeHandler): Builder

        override fun servicesMissFactory(factory: OnServicesMissFactory): Builder

        override fun authenticator(authenticator: RouteAuthenticator): Builder

        override fun addPreMatchInterceptor(interceptor: RouteInterceptor): Builder

        override fun addPostMatchInterceptor(interceptor: RouteInterceptor): Builder

        override fun routeListener(listener: RouteListener): Builder

        override fun routeListenerFactory(factory: RouteListener.Factory): Builder

        override fun executor(executor: ExecutorService): Builder

        /**
         * Hide now.
         */
        fun moduleMissingReactor(moduleMissingReactor: ModuleMissingReactor): Builder

        override fun attributeSchema(action: (AttributeSchema) -> Unit): Builder

        override fun globalLauncher(globalLauncher: GlobalLauncher): Builder

        override fun taskExecutionListener(taskExecutionListener: TaskExecutionListener): Builder

        override fun taskComparator(comparator: Comparator<Task>): Builder

        fun build(): InternalGlobalConfiguration
    }
}