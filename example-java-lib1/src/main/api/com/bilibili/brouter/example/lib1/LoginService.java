package com.bilibili.brouter.example.lib1;

public interface LoginService {
    boolean isLogin();

    void login();

    void logout();

    void register(Callback callback);

    void unregister(Callback callback);

    interface Callback {
        void onLogin();

        void onLogout();
    }
}

