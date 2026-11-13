package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Custom Exceptions for precise error handling
class ConnectionException(message: String, cause: Throwable? = null) : java.io.IOException(message, cause)
class InvalidAiResponseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class RateLimitException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class AuthenticationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class HttpApiException(val code: Int, message: String, cause: Throwable? = null) : RuntimeException(message, cause)

enum class ApiBaseUrlType(
    val key: String,
    val displayName: String,
    val endpointHint: String,
    val placeholderUrl: String,
    val defaultModel: String
) {
    ANTROPIC(
        key = "antropic",
        displayName = "antropic",
        endpointHint = "Anthropic Messages (/v1/messages)",
        placeholderUrl = "https://api.anthropic.com",
        defaultModel = "claude-3-5-sonnet-latest"
    ),
    CHAT_ID(
        key = "chat_id",
        displayName = "chat_id",
        endpointHint = "Chat Completions (/v1/chat/completions)",
        placeholderUrl = "https://openrouter.ai/api/v1",
        defaultModel = "google/gemini-2.5-flash"
    ),
    RESPONSE(
        key = "response",
        displayName = "response",
        endpointHint = "Responses API (/v1/responses)",
        placeholderUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini"
    );

    companion object {
        fun fromKey(key: String?): ApiBaseUrlType {
            return entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: CHAT_ID
        }
    }
}

// --- Chat Completions (chat_id) Models ---
@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    @Json(name = "response_format")
    val responseFormat: OpenRouterResponseFormat? = null,
    val temperature: Float? = null,
    @Json(name = "max_tokens")
    val maxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String, // "system" or "user"
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponseFormat(
    val type: String // "json_object"
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice>?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val message: OpenRouterChoiceMessage?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoiceMessage(
    val content: String?
)

// --- Anthropic (antropic) Models ---
@JsonClass(generateAdapter = true)
data class AnthropicMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class AnthropicRequest(
    val model: String,
    @Json(name = "max_tokens")
    val maxTokens: Int = 4096,
    val system: String? = null,
    val messages: List<AnthropicMessage>,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicContentBlock(
    val type: String? = null,
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class AnthropicResponse(
    val content: List<AnthropicContentBlock>? = null
)

// --- OpenAI Responses API (response) Models ---
@JsonClass(generateAdapter = true)
data class ResponsesApiRequest(
    val model: String,
    val instructions: String? = null,
    val input: String,
    val temperature: Float? = null,
    @Json(name = "max_output_tokens")
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class ResponsesApiContentBlock(
    val type: String? = null,
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponsesApiOutputItem(
    val type: String? = null,
    val content: List<ResponsesApiContentBlock>? = null
)

@JsonClass(generateAdapter = true)
data class ResponsesApiResponse(
    @Json(name = "output_text")
    val outputText: String? = null,
    val output: List<ResponsesApiOutputItem>? = null
)

// --- Domain Exam Models ---
@JsonClass(generateAdapter = true)
data class PgQuestion(
    val soal: String,
    val pilihan: Map<String, String>, // e.g. {"a": "...", "b": "...", ...}
    val kunci: String // "a", "b", "c", "d"
)

@JsonClass(generateAdapter = true)
data class EssayQuestion(
    val soal: String,
    val kunci_esensi: String
)

@JsonClass(generateAdapter = true)
data class ExamQuestions(
    val pilihan_ganda: List<PgQuestion>,
    val esai: List<EssayQuestion>
)

@JsonClass(generateAdapter = true)
data class EssayGradingItem(
    val skor: Int,
    val umpan_balik: String
)

@JsonClass(generateAdapter = true)
data class EssayGradingResponseList(
    val evaluasi: List<EssayGradingItem>
)
