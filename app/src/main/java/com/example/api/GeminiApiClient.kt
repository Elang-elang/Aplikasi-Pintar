package com.example.api

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GeminiApiClient {
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS) // 5 minutes
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(300, TimeUnit.SECONDS) // 5 minutes
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * Resolves full endpoint URL based on user-entered base URL and selected API type.
     */
    fun resolveEndpointUrl(rawBaseUrl: String, type: ApiBaseUrlType): String {
        val trimmed = rawBaseUrl.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "Base URL tidak boleh kosong" }
        require(trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            "Base URL harus diawali http:// atau https://"
        }

        val resolved = when (type) {
            ApiBaseUrlType.ANTROPIC -> {
                when {
                    trimmed.endsWith("/messages", ignoreCase = true) -> trimmed
                    trimmed.endsWith("/v1", ignoreCase = true) ||
                        trimmed.endsWith("/api/v1", ignoreCase = true) -> "$trimmed/messages"
                    else -> "$trimmed/v1/messages"
                }
            }
            ApiBaseUrlType.CHAT_ID -> {
                when {
                    trimmed.endsWith("/chat/completions", ignoreCase = true) -> trimmed
                    trimmed.endsWith("/v1", ignoreCase = true) ||
                        trimmed.endsWith("/api/v1", ignoreCase = true) -> "$trimmed/chat/completions"
                    trimmed.contains("openrouter.ai", ignoreCase = true) && !trimmed.endsWith("/api", ignoreCase = true) ->
                        "$trimmed/api/v1/chat/completions"
                    else -> "$trimmed/v1/chat/completions"
                }
            }
            ApiBaseUrlType.RESPONSE -> {
                when {
                    trimmed.endsWith("/responses", ignoreCase = true) -> trimmed
                    trimmed.endsWith("/v1", ignoreCase = true) ||
                        trimmed.endsWith("/api/v1", ignoreCase = true) -> "$trimmed/responses"
                    trimmed.contains("openrouter.ai", ignoreCase = true) && !trimmed.endsWith("/api", ignoreCase = true) ->
                        "$trimmed/api/v1/responses"
                    else -> "$trimmed/v1/responses"
                }
            }
        }

        val parsed = resolved.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Format Base URL tidak valid: $rawBaseUrl")
        return parsed.toString()
    }

    /**
     * Normalizes the model name cleanly depending on provider/baseUrl.
     */
    fun normalizeModel(
        model: String,
        baseUrl: String = "",
        baseUrlType: ApiBaseUrlType = ApiBaseUrlType.CHAT_ID
    ): String {
        val trimmed = model.trim()
        if (trimmed.isBlank()) {
            return baseUrlType.defaultModel
        }
        if (baseUrl.contains("openrouter.ai", ignoreCase = true) && !trimmed.contains("/")) {
            if (trimmed.startsWith("gemini-")) {
                return "google/$trimmed"
            }
            if (trimmed.startsWith("llama-")) {
                return "meta-llama/$trimmed"
            }
        }
        return trimmed
    }

    /**
     * Extracts and cleans potential JSON content wrapped in markdown block syntax.
     */
    fun extractJson(rawText: String): String {
        var text = rawText.trim()
        if (text.contains("```json")) {
            text = text.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (text.contains("```")) {
            text = text.substringAfter("```").substringBeforeLast("```").trim()
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }
        return text
    }

    private suspend fun executeCancellable(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val call = okHttpClient.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(ConnectionException("Error karena koneksi internet atau server tidak dapat dijangkau: ${e.message}", e))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        val bodyStr = try {
                            res.body?.string() ?: ""
                        } catch (e: Exception) {
                            ""
                        }
                        if (continuation.isActive) {
                            continuation.resume(Pair(res.code, bodyStr))
                        }
                    }
                }
            })
        }
    }

    private suspend fun executeChatCompletion(
        baseUrl: String,
        baseUrlType: ApiBaseUrlType,
        apiKey: String,
        model: String,
        systemPrompt: String?,
        userPrompt: String,
        temperature: Float = 0.4f,
        expectJson: Boolean = false
    ): String {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            throw AuthenticationException("API Key tidak boleh kosong")
        }

        val endpointUrl = try {
            resolveEndpointUrl(baseUrl, baseUrlType)
        } catch (e: Exception) {
            throw ConnectionException(e.message ?: "Base URL tidak valid", e)
        }

        val finalModel = normalizeModel(model, baseUrl, baseUrlType)

        val requestBuilder = Request.Builder()
            .url(endpointUrl)
            .addHeader("Content-Type", "application/json")

        val jsonBody = when (baseUrlType) {
            ApiBaseUrlType.ANTROPIC -> {
                requestBuilder
                    .addHeader("x-api-key", cleanKey)
                    .addHeader("anthropic-version", "2023-06-01")
                val payload = AnthropicRequest(
                    model = finalModel,
                    maxTokens = if (expectJson) 4096 else 64,
                    system = systemPrompt?.takeIf { it.isNotBlank() },
                    messages = listOf(AnthropicMessage(role = "user", content = userPrompt)),
                    temperature = temperature
                )
                moshi.adapter(AnthropicRequest::class.java).toJson(payload)
            }
            ApiBaseUrlType.CHAT_ID -> {
                requestBuilder.addHeader("Authorization", "Bearer $cleanKey")
                val msgs = buildList {
                    if (!systemPrompt.isNullOrBlank()) {
                        add(OpenRouterMessage(role = "system", content = systemPrompt))
                    }
                    add(OpenRouterMessage(role = "user", content = userPrompt))
                }
                val payload = OpenRouterChatRequest(
                    model = finalModel,
                    messages = msgs,
                    responseFormat = if (expectJson && baseUrl.contains("openrouter.ai", ignoreCase = true)) {
                        OpenRouterResponseFormat(type = "json_object")
                    } else null,
                    temperature = temperature,
                    maxTokens = if (expectJson) null else 64
                )
                moshi.adapter(OpenRouterChatRequest::class.java).toJson(payload)
            }
            ApiBaseUrlType.RESPONSE -> {
                requestBuilder.addHeader("Authorization", "Bearer $cleanKey")
                val payload = ResponsesApiRequest(
                    model = finalModel,
                    instructions = systemPrompt?.takeIf { it.isNotBlank() },
                    input = userPrompt,
                    temperature = temperature,
                    maxOutputTokens = if (expectJson) 4096 else 64
                )
                moshi.adapter(ResponsesApiRequest::class.java).toJson(payload)
            }
        }

        val request = requestBuilder
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val (code, responseBody) = executeCancellable(request)

        if (code !in 200..299) {
            Log.e("GeminiApiClient", "HTTP Error $code on $endpointUrl: ${responseBody.take(300)}")
            when {
                code == 401 || code == 403 -> {
                    throw AuthenticationException("API Key tidak valid atau ditolak server (HTTP $code)")
                }
                code == 429 || code == 402 ||
                    responseBody.contains("limit", ignoreCase = true) ||
                    responseBody.contains("quota", ignoreCase = true) -> {
                    throw RateLimitException("Kesalahan permintaan karena permintaan telah limit (HTTP $code)")
                }
                else -> {
                    throw HttpApiException(code, "HTTP Error $code: ${responseBody.take(200)}")
                }
            }
        }

        val rawText: String? = try {
            when (baseUrlType) {
                ApiBaseUrlType.ANTROPIC -> {
                    val parsed = moshi.adapter(AnthropicResponse::class.java).fromJson(responseBody)
                    parsed?.content?.mapNotNull { it.text }?.joinToString("\n")?.takeIf { it.isNotBlank() }
                }
                ApiBaseUrlType.CHAT_ID -> {
                    val parsed = moshi.adapter(OpenRouterChatResponse::class.java).fromJson(responseBody)
                    parsed?.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                }
                ApiBaseUrlType.RESPONSE -> {
                    val parsed = moshi.adapter(ResponsesApiResponse::class.java).fromJson(responseBody)
                    parsed?.outputText?.takeIf { it.isNotBlank() }
                        ?: parsed?.output
                            ?.flatMap { it.content.orEmpty() }
                            ?.mapNotNull { it.text }
                            ?.joinToString("\n")
                            ?.takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Failed to parse API response: ${responseBody.take(300)}", e)
            throw InvalidAiResponseException("Format respons server tidak sesuai dengan tipe ${baseUrlType.displayName}", e)
        }

        if (rawText.isNullOrBlank()) {
            throw InvalidAiResponseException("Kesalahan permintaan yakni jawaban AI kosong atau tidak sesuai")
        }
        return rawText
    }

    /**
     * Tests the user-configured Base URL, API Key, Base URL Type, and Model with a minimal request.
     */
    suspend fun testConnection(
        baseUrl: String,
        baseUrlType: ApiBaseUrlType,
        apiKey: String,
        model: String
    ): String {
        return executeChatCompletion(
            baseUrl = baseUrl,
            baseUrlType = baseUrlType,
            apiKey = apiKey,
            model = model,
            systemPrompt = null,
            userPrompt = "Reply with OK",
            temperature = 0f,
            expectJson = false
        )
    }

    /**
     * Multi-turn learning chatbot & document/text explanation call across antropic, chat_id, and response API types.
     */
    suspend fun sendStudyChatCompletion(
        baseUrl: String,
        baseUrlType: ApiBaseUrlType,
        apiKey: String,
        model: String,
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>
    ): String {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            throw AuthenticationException("API Key tidak boleh kosong")
        }

        val endpointUrl = try {
            resolveEndpointUrl(baseUrl, baseUrlType)
        } catch (e: Exception) {
            throw ConnectionException(e.message ?: "Base URL tidak valid", e)
        }

        val finalModel = normalizeModel(model, baseUrl, baseUrlType)
        val requestBuilder = Request.Builder()
            .url(endpointUrl)
            .addHeader("Content-Type", "application/json")

        val normalizedHistory = buildList<Pair<String, String>> {
            for ((rawRole, content) in conversationHistory) {
                val role = if (rawRole == "assistant") "assistant" else "user"
                val clean = content.trim()
                if (clean.isEmpty()) continue
                if (isNotEmpty() && last().first == role) {
                    val prev = removeAt(lastIndex)
                    add(role to "${prev.second}\n\n$clean")
                } else {
                    add(role to clean)
                }
            }
        }

        val jsonBody = when (baseUrlType) {
            ApiBaseUrlType.ANTROPIC -> {
                requestBuilder
                    .addHeader("x-api-key", cleanKey)
                    .addHeader("anthropic-version", "2023-06-01")
                val anthropicMessages = normalizedHistory.map { (role, content) ->
                    AnthropicMessage(role = role, content = content)
                }
                val payload = AnthropicRequest(
                    model = finalModel,
                    maxTokens = 4096,
                    system = systemPrompt,
                    messages = anthropicMessages,
                    temperature = 0.5f
                )
                moshi.adapter(AnthropicRequest::class.java).toJson(payload)
            }
            ApiBaseUrlType.CHAT_ID -> {
                requestBuilder.addHeader("Authorization", "Bearer $cleanKey")
                val chatMessages = buildList {
                    add(OpenRouterMessage(role = "system", content = systemPrompt))
                    conversationHistory.forEach { (role, content) ->
                        add(
                            OpenRouterMessage(
                                role = if (role == "assistant") "assistant" else "user",
                                content = content
                            )
                        )
                    }
                }
                val payload = OpenRouterChatRequest(
                    model = finalModel,
                    messages = chatMessages,
                    responseFormat = null,
                    temperature = 0.5f,
                    maxTokens = null
                )
                moshi.adapter(OpenRouterChatRequest::class.java).toJson(payload)
            }
            ApiBaseUrlType.RESPONSE -> {
                requestBuilder.addHeader("Authorization", "Bearer $cleanKey")
                val formattedInput = if (conversationHistory.size <= 1) {
                    conversationHistory.lastOrNull()?.second ?: ""
                } else {
                    conversationHistory.joinToString("\n\n") { (role, content) ->
                        val label = if (role == "assistant") "Tutor AI" else "Pelajar"
                        "[$label]: $content"
                    }
                }
                val payload = ResponsesApiRequest(
                    model = finalModel,
                    instructions = systemPrompt,
                    input = formattedInput,
                    temperature = 0.5f,
                    maxOutputTokens = 4096
                )
                moshi.adapter(ResponsesApiRequest::class.java).toJson(payload)
            }
        }

        val request = requestBuilder
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val (code, responseBody) = executeCancellable(request)

        if (code !in 200..299) {
            Log.e("GeminiApiClient", "Study Chat HTTP Error $code on $endpointUrl: ${responseBody.take(300)}")
            when {
                code == 401 || code == 403 -> {
                    throw AuthenticationException("API Key tidak valid atau ditolak server (HTTP $code)")
                }
                code == 429 || code == 402 ||
                    responseBody.contains("limit", ignoreCase = true) ||
                    responseBody.contains("quota", ignoreCase = true) -> {
                    throw RateLimitException("Kesalahan permintaan karena permintaan telah limit (HTTP $code)")
                }
                else -> {
                    throw HttpApiException(code, "HTTP Error $code: ${responseBody.take(200)}")
                }
            }
        }

        val rawText: String? = try {
            when (baseUrlType) {
                ApiBaseUrlType.ANTROPIC -> {
                    val parsed = moshi.adapter(AnthropicResponse::class.java).fromJson(responseBody)
                    parsed?.content?.mapNotNull { it.text }?.joinToString("\n")?.takeIf { it.isNotBlank() }
                }
                ApiBaseUrlType.CHAT_ID -> {
                    val parsed = moshi.adapter(OpenRouterChatResponse::class.java).fromJson(responseBody)
                    parsed?.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                }
                ApiBaseUrlType.RESPONSE -> {
                    val parsed = moshi.adapter(ResponsesApiResponse::class.java).fromJson(responseBody)
                    parsed?.outputText?.takeIf { it.isNotBlank() }
                        ?: parsed?.output
                            ?.flatMap { it.content.orEmpty() }
                            ?.mapNotNull { it.text }
                            ?.joinToString("\n")
                            ?.takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            throw InvalidAiResponseException("Format respons server tidak sesuai dengan tipe ${baseUrlType.displayName}", e)
        }

        if (rawText.isNullOrBlank()) {
            throw InvalidAiResponseException("Jawaban AI kosong atau tidak sesuai")
        }
        return rawText
    }

    suspend fun generateExamQuestions(
        kisiKisi: String,
        pgCount: Int,
        essayCount: Int,
        baseUrl: String,
        baseUrlType: ApiBaseUrlType,
        customKey: String,
        model: String
    ): ExamQuestions? {
        val systemPrompt = """
            Anda adalah pakar pembuat soal ujian sekolah di Indonesia. 
            Tugas Anda adalah merumuskan soal ujian sekolah berkualitas tinggi berdasarkan kisi-kisi bahan ajar dari pengguna.
            
            Ujian harus terdiri dari:
            - $pgCount Soal Pilihan Ganda (masing-masing memiliki opsi 'a', 'b', 'c', 'd' dan satu 'kunci' jawaban berupa karakter huruf kecil).
            - $essayCount Soal Esai (masing-masing memiliki 'soal' dan deskripsi 'kunci_esensi' berupa kunci pemandu untuk mencocokkan jawaban esai siswa nanti).

            Anda WAJIB memberikan response murni dalam format JSON. Isi JSON harus mematuhi struktur kelas data berikut:
            {
               "pilihan_ganda": [
                   {
                       "soal": "Teks pertanyaan pilihan ganda...",
                       "pilihan": {
                           "a": "Opsi A",
                           "b": "Opsi B",
                           "c": "Opsi C",
                           "d": "Opsi D"
                       },
                       "kunci": "a"
                   }
               ],
               "esai": [
                   {
                       "soal": "Teks pertanyaan esai...",
                       "kunci_esensi": "Poin-poin atau konsep utama jawaban esai yang benar..."
                   }
               ]
            }

            Catatan Penting:
            - Buatlah pertanyaan yang bervariasi secara akademis relevan dengan kisi-kisi.
            - Karakter kunci jawaban harus huruf kecil: "a", "b", "c", atau "d".
            - Jangan sertakan teks penjelasan apa pun di luar blok JSON. Kembalikan JSON siap pakai.
        """.trimIndent()

        val rawResponse = executeChatCompletion(
            baseUrl = baseUrl,
            baseUrlType = baseUrlType,
            apiKey = customKey,
            model = model,
            systemPrompt = systemPrompt,
            userPrompt = "Kisi-kisi bahan ajar:\n$kisiKisi",
            temperature = 0.4f,
            expectJson = true
        )

        return try {
            val cleanJsonStr = extractJson(rawResponse)
            Log.d("GeminiApiClient", "Clean Exam Questions JSON: ${cleanJsonStr.take(200)}")

            val examQuestions = moshi.adapter(ExamQuestions::class.java).fromJson(cleanJsonStr)
            if (examQuestions == null || (examQuestions.pilihan_ganda.isEmpty() && examQuestions.esai.isEmpty())) {
                throw InvalidAiResponseException("Kesalahan permintaan yakni jawaban ai tidak sesuai")
            }
            examQuestions
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "JSON parsing failed on response: $rawResponse", e)
            throw InvalidAiResponseException("Kesalahan permintaan yakni jawaban ai tidak sesuai", e)
        }
    }

    suspend fun gradeEssayAnswers(
        essayQuestionsAndAnswers: List<Pair<EssayQuestion, String>>,
        baseUrl: String,
        baseUrlType: ApiBaseUrlType,
        customKey: String,
        model: String
    ): List<EssayGradingItem>? {
        val textBuilder = StringBuilder()
        textBuilder.append("Silakan nilai jawaban esai siswa berikut:\n\n")
        essayQuestionsAndAnswers.forEachIndexed { index, pair ->
            val q = pair.first
            val ans = pair.second
            textBuilder.append("--- SOAL ${index + 1} ---\n")
            textBuilder.append("Pertanyaan: ${q.soal}\n")
            textBuilder.append("Kunci Esensi Jawab: ${q.kunci_esensi}\n")
            textBuilder.append("Jawaban Siswa: $ans\n\n")
        }

        val systemPrompt = """
            Anda adalah guru profesional pengoreksi ujian esai. Tugas Anda adalah memberikan penilaian yang adil dan konsisten untuk masing-masing esai.
            
            Untuk tiap jawaban siswa, bandingkan dengan 'Kunci Esensi Jawab'.
            Berikan skor numerik (skor) antara 0 hingga 100 berdasarkan ketepatan deskripsi, dan tulis umpan balik singkat (umpan_balik) dalam bahasa Indonesia tentang apa yang benar dan apa yang kurang.

            Anda WAJIB memberikan respons murni dalam JSON terstruktur:
            {
               "evaluasi": [
                   {
                       "skor": 85,
                       "umpan_balik": "Penjelasan sangat lengkap dan memahami inti persoalan. Sangat bagus!"
                   }
               ]
            }

            Wajib menghasilkan urutan evaluasi sesuai urutan pertanyaan esai yang diberikan di prompt (misal jika ada 3 esai, maka kembalikan 3 item evaluasi secara berurutan).
            Jangan sertakan penjelasan obrolan di luar blok JSON.
        """.trimIndent()

        val rawResponse = executeChatCompletion(
            baseUrl = baseUrl,
            baseUrlType = baseUrlType,
            apiKey = customKey,
            model = model,
            systemPrompt = systemPrompt,
            userPrompt = textBuilder.toString(),
            temperature = 0.2f,
            expectJson = true
        )

        return try {
            val cleanJsonStr = extractJson(rawResponse)
            Log.d("GeminiApiClient", "Clean Essay Grading JSON: $cleanJsonStr")

            val parsedRes = moshi.adapter(EssayGradingResponseList::class.java).fromJson(cleanJsonStr)
            val evaluasi = parsedRes?.evaluasi
            if (evaluasi == null || evaluasi.isEmpty()) {
                throw InvalidAiResponseException("Kesalahan permintaan yakni jawaban ai tidak sesuai")
            }
            evaluasi
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "JSON parsing failed on response: $rawResponse", e)
            throw InvalidAiResponseException("Kesalahan permintaan yakni jawaban ai tidak sesuai", e)
        }
    }
}
