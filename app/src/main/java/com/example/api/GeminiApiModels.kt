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
    val badgeLabel: String,
    val endpointHint: String,
    val placeholderUrl: String,
    val defaultModel: String
) {
    WITH_V1(
        key = "with_v1",
        displayName = "Dengan /v1",
        badgeLabel = "Tipe OpenAI (/v1/chat/completions)",
        endpointHint = "Format OpenAI (pakai /v1): .../v1/chat/completions",
        placeholderUrl = "https://api.openai.com/v1 atau https://localhost:8080/v1",
        defaultModel = "gpt-4o-mini"
    ),
    WITHOUT_V1(
        key = "without_v1",
        displayName = "Tanpa /v1",
        badgeLabel = "Tipe Anthropic (/v1/messages)",
        endpointHint = "Format Anthropic (Base URL tanpa /v1): .../v1/messages",
        placeholderUrl = "https://api.anthropic.com atau https://127.0.0.1:8080",
        defaultModel = "claude-3-5-sonnet-latest"
    );

    companion object {
        fun fromKey(key: String?): ApiBaseUrlType {
            return when (key?.trim()?.lowercase()) {
                "without_v1", "antropic", "anthropic" -> WITHOUT_V1
                "with_v1", "chat_id", "response", "openai" -> WITH_V1
                else -> WITH_V1
            }
        }
    }
}

// --- OpenAI Chat Completions (WITH_V1) Models ---
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
    val role: String, // "system", "user", or "assistant"
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

// --- Anthropic (WITHOUT_V1) Models ---
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

enum class GlobalPgOptionCount(
    val count: Int?,
    val shortTitle: String,
    val subtitle: String
) {
    AUTO_AI(null, "AI", "Otomatis"),
    THREE(3, "3", "A–C"),
    FOUR(4, "4", "A–D"),
    FIVE(5, "5", "A–E"),
    SIX(6, "6", "A–F");

    companion object {
        fun fromCount(count: Int?): GlobalPgOptionCount {
            return entries.firstOrNull { it.count == count } ?: AUTO_AI
        }
    }
}

// --- Domain Exam Models ---
@JsonClass(generateAdapter = true)
data class PgQuestion(
    val soal: String,
    val pilihan: Map<String, String>, // e.g. {"a": "...", "b": "...", ...}
    val kunci: String? = null, // backward compatible single key ("a") or comma-separated ("a,c")
    val kunci_jawaban: List<String> = emptyList(), // multi-answer keys e.g. ["a", "c"]
    val maks_pilihan: Int = 1 // max selections permitted on this question (1 for single, 2+ for multi-answer)
) {
    fun resolvedCorrectKeys(): List<String> {
        val validKeys = pilihan.keys.map { it.trim().lowercase() }.toSet()
        val fromList = kunci_jawaban
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() && (validKeys.isEmpty() || it in validKeys) }
            .distinct()
        if (fromList.isNotEmpty()) return fromList.sorted()

        return kunci
            ?.split(",", ";", " ")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() && (validKeys.isEmpty() || it in validKeys) }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }

    fun resolvedMaxSelections(): Int {
        val correctCount = resolvedCorrectKeys().size.coerceAtLeast(1)
        val optionCount = pilihan.size.coerceAtLeast(1)
        return maxOf(maks_pilihan.coerceAtLeast(1), correctCount).coerceAtMost(optionCount)
    }

    val isMultiAnswer: Boolean
        get() = resolvedMaxSelections() > 1 || resolvedCorrectKeys().size > 1
}

@JsonClass(generateAdapter = true)
data class EssayQuestion(
    val soal: String,
    val kunci_esensi: String
)

@JsonClass(generateAdapter = true)
data class ExamQuestions(
    val jumlah_opsi_pg: Int? = null,
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
