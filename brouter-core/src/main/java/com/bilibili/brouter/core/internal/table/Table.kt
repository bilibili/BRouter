package com.bilibili.brouter.core.internal.table

import android.annotation.SuppressLint
import com.bilibili.brouter.api.ServiceCentral
import com.bilibili.brouter.api.internal.IRoutes
import com.bilibili.brouter.api.internal.Registry
import com.bilibili.brouter.api.internal.incubating.ModuleInternal
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.RecursiveTask
import javax.inject.Provider

internal class Table(val serviceTable: ServiceTable, val routeTable: RouteTable) : Registry {
    override fun registerRoutes(routes: IRoutes) {
        routeTable.registerRoutes(routes)
    }

    override fun <T> registerProviderService(
        clazz: Class<T>, name: String,
        provider: Provider<out T>,
        module: ModuleInternal,
        dependencies: Array<out Any>
    ) {
        serviceTable.registerProviderService(clazz, name, provider, module, dependencies)
    }

    override fun <T> registerTaskOutputService(
        clazz: Class<T>,
        name: String,
        module: ModuleInternal,
        taskName: String
    ) {
        serviceTable.registerTaskOutputService(clazz, name, module, taskName)
    }


    override fun deferred(): ServiceCentral = serviceTable.deferred()
}


@SuppressLint("NewApi")
internal class ForkJoinMergeTable(val list: List<Table>) : RecursiveTask<Table>() {

    override fun compute(): Table {
        if (list.size == 1) {
            return list[0]
        }
        val preSize = list.size / 2
        val preTask = ForkJoinMergeTable(list.subList(0, preSize))
        val postTask = ForkJoinMergeTable(list.subList(preSize, list.size))
        postTask.fork()
        val pre = preTask.compute()
        val post = postTask.join()


        val route = MergeRoute(pre.routeTable, post.routeTable)
        route.fork()
        pre.serviceTable.merge(post.serviceTable)
        route.join()

        return pre
    }
}

/**
 * Need (floor(log2(n)) + 1) threads.
 */
internal class DivideAndConquerMergeTable(
    private val executor: ExecutorService,
    private val list: List<Table>
) :
    Callable<Table> {
    override fun call(): Table {
        if (list.size == 1) {
            return list[0]
        }
        val preSize = list.size / 2

        val preCallable = DivideAndConquerMergeTable(executor, list.subList(0, preSize))
        val postCallable = DivideAndConquerMergeTable(executor, list.subList(preSize, list.size))
        val postTask = executor.submit(postCallable)

        val pre = preCallable.call()
        val post = postTask.get()
        pre.routeTable.merge(post.routeTable)
        pre.serviceTable.merge(post.serviceTable)
        return pre
    }
}