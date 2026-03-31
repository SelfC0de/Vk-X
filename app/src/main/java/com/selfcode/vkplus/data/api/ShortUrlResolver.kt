package com.selfcode.vkplus.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortUrlResolver @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val cache = mutableMapOf<String, String>()

    // Supported short domains
    private val shortDomains = listOf("vk.cc", "vkontakte.ru/away", "bit.ly", "t.co", "tinyurl.com", "ow.ly")

    fun isShortUrl(url: String): Boolean = shortDomains.any { url.contains(it) }

    suspend fun resolve(url: String): String {
        cache[url]?.let { return it }
        return try {
            val request = Request.Builder()
                .url(url)
                .head() // HEAD request only — no content downloaded
                .build()
            val response = httpClient.newBuilder()
                .followRedirects(false)
                .build()
                .newCall(request)
                .execute()
            val location = response.header("Location") ?: url
            val resolved = if (location.startsWith("http")) location else url
            cache[url] = resolved
            resolved
        } catch (e: Exception) {
            url
        }
    }
}
