package com.bilibili.brouter.example.lib1.test;

import org.jetbrains.annotations.NotNull;

import com.bilibili.brouter.api.ModuleActivator;
import com.bilibili.brouter.api.ModuleConfigurationModifier;
import com.bilibili.brouter.api.ModuleContext;
import com.bilibili.brouter.api.ModuleOptions;
import com.bilibili.brouter.api.RouteRequestKt;
import com.bilibili.brouter.api.RouteResponseKt;
import com.bilibili.brouter.example.lib2.TeenagerService;

@ModuleOptions(
        name = "test",
        defaultModule = false
)
public class TestModule extends ModuleActivator {

    private final TeenagerService service;

    public TestModule(TeenagerService service) {
        this.service = service;
    }

    @Override
    public void onCreate(@NotNull ModuleContext context, @NotNull ModuleConfigurationModifier modifier) {
        super.onCreate(context, modifier);
        modifier.addModuleInterceptors(chain -> {
            if (service.isEnabled()) {
                return RouteResponseKt.redirectTo(chain.getRequest(),
                        RouteRequestKt.toRouteRequest("coffee://teenager"));
            } else {
                return chain.proceed(chain.getRequest());
            }
        });
    }
}
