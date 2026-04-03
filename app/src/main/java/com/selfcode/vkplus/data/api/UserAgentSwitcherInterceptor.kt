package com.selfcode.vkplus.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAgentSwitcherInterceptor @Inject constructor() : Interceptor {

    private val browserUAs = listOf(
        // Chrome Windows
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        // Chrome Android
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
        // Safari macOS
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Safari/605.1.15",
        // Edge Windows
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
        // Firefox Android
        "Mozilla/5.0 (Android 13; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0"
    )

    // Stable per-session UA (randomized once, not per-request — to avoid fingerprint inconsistency)
    private val sessionUA: String = browserUAs.random()

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .removeHeader("User-Agent")
            .header("User-Agent", sessionUA)
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()
        return chain.proceed(req)
    }
}
