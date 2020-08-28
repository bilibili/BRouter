package com.bilibili.brouter.example.lib1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.inject.Inject;

import com.bilibili.brouter.api.Routes;
import com.bilibili.brouter.api.BRouter;
import com.bilibili.brouter.example.extensions.base.BaseFragment;

@Routes("coffee://login")
public class LoginFragment extends BaseFragment {

    @Inject
    LoginService service;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BRouter.inject(this);

        view.findViewById(R.id.btn_login).setOnClickListener(v -> {
            service.login();
            performForward();
        });
    }
}
