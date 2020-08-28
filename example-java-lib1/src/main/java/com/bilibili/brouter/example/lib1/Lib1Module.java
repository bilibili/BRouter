package com.bilibili.brouter.example.lib1;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import com.bilibili.brouter.api.BootStrapMode;
import com.bilibili.brouter.api.ModuleActivator;
import com.bilibili.brouter.api.ModuleConfigurationModifier;
import com.bilibili.brouter.api.ModuleContext;
import com.bilibili.brouter.api.ModuleOptions;
import com.bilibili.brouter.api.task.TaskOptions;
import com.bilibili.brouter.api.task.ThreadMode;

@ModuleOptions(name = "lib1", mode = BootStrapMode.ON_DEMAND)
public class Lib1Module extends ModuleActivator {

    @TaskOptions(name = "lib1onCreate", threadMode = ThreadMode.MAIN)
    @Override
    public void onCreate(@NotNull ModuleContext context, @NotNull ModuleConfigurationModifier modifier) {
        super.onCreate(context, modifier);
        Log.i("BRouter", "lib1 onCreate, thread: " + Thread.currentThread().getName());
    }

    @Override
    public void onPostCreate(@NotNull ModuleContext context) {
        super.onPostCreate(context);
        Log.i("BRouter", "lib1 onPostCreate, thread: " + Thread.currentThread().getName());
    }
}