package com.example.data.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @property:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @property:Json(name = "role") val role: String = "user",
    @property:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @property:Json(name = "temperature") val temperature: Float? = 0.7f,
    @property:Json(name = "topP") val topP: Float? = 0.95f,
    @property:Json(name = "topK") val topK: Int? = 40,
    @property:Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @property:Json(name = "contents") val contents: List<GeminiContent>,
    @property:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = GeminiGenerationConfig()
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @property:Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @property:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
) {
    fun extractFirstText(): String? {
        return candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    }
}
