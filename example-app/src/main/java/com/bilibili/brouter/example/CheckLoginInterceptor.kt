package com.bilibili.brouter.example

import com.bilibili.brouter.api.RouteInterceptor
import com.bilibili.brouter.api.RouteResponse
import com.bilibili.brouter.api.Services
import com.bilibili.brouter.example.lib1.LoginService
import javax.inject.Singleton

@Singleton
@Services
class CheckLoginInterceptor(private val loginService: LoginService) : RouteInterceptor {
    override fun intercept(chain: RouteInterceptor.Chain): RouteResponse {
        return if (!loginService.isLogin) {
            RouteResponse(RouteResponse.Code.UNAUTHORIZED, chain.request)
        } else {
            chain.proceed(chain.request)
        }
    }
}