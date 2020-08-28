package com.bilibili.brouter.example.lib2

import android.util.Log
import com.bilibili.brouter.api.*
import com.bilibili.brouter.api.task.Task
import com.bilibili.brouter.api.task.TaskAction
import com.bilibili.brouter.api.task.TaskOptions
import com.bilibili.brouter.example.lib1.LoginService

//import com.bilibili.brouter.example.lib1.LoginService

/**
 * @author dieyi
 * Created at 2020/6/9.
 */


@ModuleOptions(name = "lib2")
class Lib2Module : ModuleActivator() {

    @TaskOptions(name = "lib2onCreate", dependencies = ["mytask"])
    override fun onCreate(context: ModuleContext, modifier: ModuleConfigurationModifier) {
        super.onCreate(context, modifier)
    }
}


@BelongsTo("lib2")
@TaskOptions("mytask")
class MyTask(private val loginService: LoginService) : TaskAction {
    override fun execute(task: Task) {
        Log.i("BRouter", "lib2.mytask, thread ${Thread.currentThread().name}, is login: ${loginService.isLogin}")
    }
}