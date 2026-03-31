package com.selfcode.vkplus.domain

import com.selfcode.vkplus.data.repository.VKRepository
import com.selfcode.vkplus.data.repository.VKResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslateUseCase @Inject constructor(
    private val repository: VKRepository
) {
    private val cache = mutableMapOf<String, String>()

    suspend fun translate(text: String, targetLang: String = "ru"): VKResult<String> {
        if (text.isBlank()) return VKResult.Success("")
        val key = "${text.hashCode()}_$targetLang"
        cache[key]?.let { return VKResult.Success(it) }
        val chunks = text.chunked(1024)
        val results = mutableListOf<String>()
        for (chunk in chunks) {
            when (val r = repository.translateTexts(listOf(chunk), targetLang)) {
                is VKResult.Success -> results.add(r.data.firstOrNull() ?: chunk)
                is VKResult.Error -> return r
            }
        }
        val translated = results.joinToString(" ")
        cache[key] = translated
        return VKResult.Success(translated)
    }

    suspend fun translateBatch(texts: List<String>, targetLang: String = "ru"): VKResult<List<String>> {
        val allCached = texts.map { cache["${it.hashCode()}_$targetLang"] }
        if (allCached.none { it == null }) return VKResult.Success(allCached.filterNotNull())
        val batches = texts.chunked(10)
        val allResults = mutableListOf<String>()
        for (batch in batches) {
            when (val r = repository.translateTexts(batch, targetLang)) {
                is VKResult.Success -> {
                    r.data.forEachIndexed { i, t ->
                        cache["${batch[i].hashCode()}_$targetLang"] = t
                        allResults.add(t)
                    }
                }
                is VKResult.Error -> return r
            }
        }
        return VKResult.Success(allResults)
    }
}
