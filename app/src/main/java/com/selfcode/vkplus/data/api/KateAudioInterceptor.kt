package com.selfcode.vkplus.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KateAudioInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val url = req.url
        return if (url.encodedPath.contains("audio.")) {
            val newUrl = url.newBuilder()
                .setQueryParameter("v", "5.95")
                .setQueryParameter("https", "1")
                .build()
            val newReq = req.newBuilder()
                .url(newUrl)
                .header("User-Agent", "KateMobileAndroid/56 lite (Android 11; SDK 30; arm64-v8a; Xiaomi M2007J20CG; ru)")
                .header("X-VK-Android-Client", "new")
                .header("Accept-Language", "ru-RU, ru;q=0.9, en-US;q=0.8, en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .build()
            chain.proceed(newReq)
        } else {
            chain.proceed(req)
        }
    }
}
