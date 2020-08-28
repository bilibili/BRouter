package com.bilibili.brouter.example.lib1;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import com.bilibili.brouter.api.Services;

public class LoginManager implements LoginService, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOGIN_KEY = "logined";
    private static LoginManager INSTANCE = null;

    private final SharedPreferences sp;
    private final List<Callback> listeners = new ArrayList<>();

    private LoginManager(SharedPreferences sp) {
        this.sp = sp;
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    @Services(LoginService.class)
    synchronized public static LoginManager getInstance(@Named("app") Application app) {
        if (INSTANCE == null) {
            INSTANCE = new LoginManager(app.getSharedPreferences("login", Context.MODE_PRIVATE));
        }
        return INSTANCE;
    }

    @Override
    public boolean isLogin() {
        return sp.getBoolean(LOGIN_KEY, false);
    }

    @Override
    public void register(Callback callback) {
        synchronized (listeners) {
            listeners.add(callback);
        }
    }

    @Override
    public void unregister(Callback callback) {
        synchronized (listeners) {
            listeners.remove(callback);
        }
    }

    @Override
    public void login() {
        sp.edit().putBoolean(LOGIN_KEY, true).apply();
    }

    @Override
    public void logout() {
        sp.edit().putBoolean(LOGIN_KEY, false).apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(LOGIN_KEY)) {
            notify(isLogin());
        }
    }

    private void notify(boolean login) {
        Callback[] array;
        synchronized (listeners) {
            array = listeners.toArray(new Callback[0]);
        }
        for (Callback callback : array) {
            if (login) {
                callback.onLogin();
            } else {
                callback.onLogout();
            }
        }
    }
}
