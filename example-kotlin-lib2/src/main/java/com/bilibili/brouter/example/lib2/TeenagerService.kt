package com.bilibili.brouter.example.lib2

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.bilibili.brouter.api.Services
import java.util.*
import javax.inject.Named
import javax.inject.Singleton


interface TeenagerService {
    val isEnabled: Boolean
    fun enable()
    fun disable()
    fun register(callback: Callback)
    fun unregister(callback: Callback)
    interface Callback {
        fun onChanged(enabled: Boolean)
    }
}


@Singleton
@Services(TeenagerService::class)
class TeenagerManager constructor(@Named("app") app: Application) :
    TeenagerService,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val listeners: MutableList<TeenagerService.Callback> =
        ArrayList()

    private val sp = app.getSharedPreferences("teenager", Context.MODE_PRIVATE)

    override val isEnabled: Boolean get() = sp.getBoolean(KEY, false)


    override fun register(callback: TeenagerService.Callback) {
        synchronized(listeners) { listeners.add(callback) }
    }

    override fun unregister(callback: TeenagerService.Callback) {
        synchronized(listeners) { listeners.remove(callback) }
    }

    override fun enable() {
        sp.edit().putBoolean(KEY, true).apply()
    }

    override fun disable() {
        sp.edit().putBoolean(KEY, false).apply()
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        if (key == KEY) {
            notify(isEnabled)
        }
    }

    private fun notify(enabled: Boolean) {
        synchronized(listeners) { listeners.toTypedArray() }
            .forEach {
                it.onChanged(enabled)
            }
    }

    companion object {
        private const val KEY = "enable"
    }

    init {
        sp.registerOnSharedPreferenceChangeListener(this)
    }
}