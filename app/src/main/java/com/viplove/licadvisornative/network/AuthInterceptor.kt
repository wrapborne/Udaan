package com.viplove.licadvisornative.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = TokenManager.getToken()
        if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(
            originalRequest.newBuilder()
                .addHeader("Accept", "application/json")
                .build()
        )
    }
}
