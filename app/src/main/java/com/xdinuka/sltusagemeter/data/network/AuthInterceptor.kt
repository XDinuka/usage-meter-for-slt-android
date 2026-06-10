package com.xdinuka.sltusagemeter.data.network

import com.xdinuka.sltusagemeter.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Legacy single-account auth interceptor.
 * Replaced by [ProfileAuthInterceptor] — kept for reference only.
 */
class AuthInterceptor(private val accessToken: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val builder = req.newBuilder().header("X-Ibm-Client-Id", BuildConfig.SLT_CLIENT_ID)
        val path = req.url.encodedPath
        if (!path.contains("Account/Login") && !path.contains("Account/RefreshToken")) {
            accessToken()?.let { builder.header("authorization", "bearer $it") }
        }
        return chain.proceed(builder.build())
    }
}
