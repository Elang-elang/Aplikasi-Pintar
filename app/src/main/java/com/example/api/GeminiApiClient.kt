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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
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

    // Dedicated client used ONLY for local loopback endpoints (https://localhost, https://127.0.0.1)
    // so local dev servers with self-signed TLS certificates work without disabling TLS for remote hosts.
    private val loopbackOkHttpClient: OkHttpClient by lazy {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
        okHttpClient.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(HostnameVerifier { hostname, _ -> isLoopbackHost(hostname) })
            .build()
    }

    fun isLoopbackHost(host: String?): Boolean {
        val normalized = host?.trim()?.lowercase()?.removePrefix("[")?.removeSuffix("]") ?: return false
        return normalized == "localhost" ||
            normalized == "127.0.0.1" ||
            normalized == "::1" ||
            normalized.startsWith("127.")
    }

    fun normalizeBaseUrlInput(rawBaseUrl: String): String {
        val trimmed = rawBaseUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        val lower = trimmed.lowercase()
        val isBareLoopback = lower == "localhost" ||
            lower.startsWith("localhost:") ||
            lower.startsWith("localhost/") ||
            lower == "127.0.0.1" ||
            lower.startsWith("127.0.0.1:") ||
            lower.startsWith("127.0.0.1/") ||
            lower.startsWith("[::1]")
        return if (isBareLoopback) "http://$trimmed" else trimmed
    }

    fun isValidBaseUrlInput(rawBaseUrl: String): Boolean {
        val normalized = normalizeBaseUrlInput(rawBaseUrl)
        if (!normalized.startsWith("http://", ignoreCase = true) &&
            !normalized.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        return normalized.toHttpUrlOrNull() != null
    }

    /**
     * Resolves full endpoint URL based on user-entered base URL and selected API type:
     * - WITH_V1 (OpenAI style): ensures /v1/chat/completions
     * - WITHOUT_V1 (Anthropic style): takes base URL without /v1 and appends /v1/messages
     */
    fun resolveEndpointUrl(rawBaseUrl: String, type: ApiBaseUrlType): String {
        val trimmed = normalizeBaseUrlInput(rawBaseUrl)
        require(trimmed.isNotEmpty()) { "Base URL tidak boleh kosong" }
        require(trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            "Base URL harus diawali http:// atau https:// (atau localhost / 127.0.0.1)"
        }

        val resolved = when (type) {
            ApiBaseUrlType.WITH_V1 -> {
                when {
                    trimmed.endsWith("/chat/completions", ignoreCase = true) -> trimmed
                    trimmed.endsWith("/v1", ignoreCase = true) ||
                        trimmed.endsWith("/api/v1", ignoreCase = true) -> "$trimmed/chat/completions"
                    trimmed.contains("openrouter.ai", ignoreCase = true) && !trimmed.endsWith("/api", ignoreCase = true) ->
                        "$trimmed/api/v1/chat/completions"
                    else -> "$trimmed/v1/chat/completions"
                }
            }
            ApiBaseUrlType.WITHOUT_V1 -> {
                when {
                    trimmed.endsWith("/messages", ignoreCase = true) -> trimmed
                    trimmed.endsWith("/v1", ignoreCase = true) ||
                        trimmed.endsWith("/api/v1", ignoreCase = true) -> "$trimmed/messages"
                    else -> "$trimmed/v1/messages"
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
        baseUrlType: ApiBaseUrlType = ApiBaseUrlType.WITH_V1
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
        val client = if (isLoopbackHost(request.url.host)) loopbackOkHttpClient else okHttpClient
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
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
            ApiBaseUrlType.WITH_V1 -> {
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
            ApiBaseUrlType.WITHOUT_V1 -> {
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
                ApiBaseUrlType.WITH_V1 -> {
                    val parsed = moshi.adapter(OpenRouterChatResponse::class.java).fromJson(responseBody)
                    parsed?.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                }
                ApiBaseUrlType.WITHOUT_V1 -> {
                    val parsed = moshi.adapter(AnthropicResponse::class.java).fromJson(responseBody)
                    parsed?.content?.mapNotNull { it.text }?.joinToString("\n")?.takeIf { it.isNotBlank() }
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
     * Multi-turn learning chatbot & document/text explanation call across WITH_V1 (OpenAI) and WITHOUT_V1 (Anthropic).
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
            ApiBaseUrlType.WITH_V1 -> {
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
            ApiBaseUrlType.WITHOUT_V1 -> {
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
                ApiBaseUrlType.WITH_V1 -> {
                    val parsed = moshi.adapter(OpenRouterChatResponse::class.java).fromJson(responseBody)
                    parsed?.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                }
                ApiBaseUrlType.WITHOUT_V1 -> {
                    val parsed = moshi.adapter(AnthropicResponse::class.java).fromJson(responseBody)
                    parsed?.content?.mapNotNull { it.text }?.joinToString("\n")?.takeIf { it.isNotBlank() }
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
        model: String,
        learnerContext: String = "",
        globalPgOptionCount: Int? = null
    ): ExamQuestions? {
        val contextSection = if (learnerContext.isNotBlank()) {
            "\nKonteks Pelajar:\n$learnerContext\nSesuaikan tingkat kesulitan, kosakata, dan kedalaman soal dengan jenjang, kelas, dan bidang pelajar tersebut."
        } else ""

        val effectiveCount = globalPgOptionCount?.coerceIn(
            com.example.model.ExamScoring.MIN_GLOBAL_OPTIONS,
            com.example.model.ExamScoring.MAX_GLOBAL_OPTIONS
        )
        val exampleCount = effectiveCount ?: 5
        val exampleLetters = com.example.model.ExamScoring.optionLettersForCount(exampleCount)
        val optionsJsonExample = exampleLetters.joinToString(",\n                        ") { letter ->
            "\"$letter\": \"Teks opsi ${letter.uppercase()} (boleh Markdown)\""
        }

        val globalOptionRule = if (effectiveCount != null) {
            val requiredLettersStr = exampleLetters.joinToString(", ") { "'$it'" }
            "- ATURAN GLOBAL OPSI PG (WAJIB DIPATUHI): Seluruh soal Pilihan Ganda dalam paket ini WAJIB memiliki tepat $effectiveCount pilihan ($requiredLettersStr). Nilai \"jumlah_opsi_pg\" wajib $effectiveCount, dan SETIAP soal PG tanpa terkecuali harus memiliki seluruh kunci $requiredLettersStr."
        } else {
            "- ATURAN GLOBAL OPSI PG (WAJIB DIPATUHI): Tentukan satu jumlah pilihan ganda yang seragam secara global (antara 3 hingga 6 pilihan, misalnya 4 untuk a–d atau 5 untuk a–e) pada field \"jumlah_opsi_pg\". Setelah ditentukan, SETIAP soal Pilihan Ganda tanpa terkecuali wajib memiliki seluruh huruf opsi berurutan dari 'a' sebanyak jumlah global tersebut (tidak boleh ada soal yang berbeda jumlah opsinya)."
        }

        val systemPrompt = """
            Anda adalah pakar pembuat soal latihan belajar di Indonesia.
            Tugas Anda adalah merumuskan soal latihan berkualitas tinggi berdasarkan kisi-kisi atau materi ajar dari pengguna.$contextSection

            Latihan harus terdiri dari:
            - $pgCount Soal Pilihan Ganda (mendukung 1 jawaban benar atau multi-jawaban benar).
            - $essayCount Soal Esai (masing-masing memiliki 'soal' dan deskripsi 'kunci_esensi' untuk evaluasi).

            Aturan Format & Konten:
            1. FORMAT MARKDOWN & GAMBAR WEB: Anda DIIZINKAN menggunakan format Markdown yang rapi (heading, **tebal**, *miring*, daftar, kode ```...```, maupun tabel Markdown) pada 'soal', teks 'pilihan', dan 'kunci_esensi' bila memperjelas persoalan. Jika visualisasi gambar dari web membantu menggambarkan soal (misalnya diagram sains, tokoh sejarah, grafik publik), Anda boleh menyertakan gambar Markdown `![deskripsi singkat](https://...)` dengan tautan HTTPS langsung yang valid dan terpercaya. Jangan mengarang URL palsu/broken.
            2. MULTI-JAWABAN PG: Anda diizinkan membuat sebagian soal PG memiliki lebih dari satu jawaban benar (misalnya 2 atau 3 pilihan benar). Isi field "kunci_jawaban" sebagai array huruf kecil seperti ["a", "c"] dan "maks_pilihan" sesuai batas maksimal pilihan yang boleh dicentang pelajar (misal 2 atau 3). Untuk soal PG satu jawaban biasa, isi "kunci_jawaban": ["a"] dan "maks_pilihan": 1.
            $globalOptionRule

            Anda WAJIB memberikan response murni dalam format JSON sesuai struktur berikut:
            {
               "jumlah_opsi_pg": $exampleCount,
               "pilihan_ganda": [
                   {
                       "soal": "Teks pertanyaan pilihan ganda (boleh menggunakan Markdown & `![alt](https://...)`)...",
                       "pilihan": {
                           $optionsJsonExample
                       },
                       "kunci_jawaban": ["a"],
                       "maks_pilihan": 1
                   },
                   {
                       "soal": "Contoh soal multi-jawaban (pilih 2 yang benar)...",
                       "pilihan": {
                           $optionsJsonExample
                       },
                       "kunci_jawaban": ["a", "c"],
                       "maks_pilihan": 2
                   }
               ],
               "esai": [
                   {
                       "soal": "Teks pertanyaan esai (boleh menggunakan Markdown)...",
                       "kunci_esensi": "Poin-poin utama jawaban esai yang benar..."
                   }
               ]
            }

            Catatan Penting:
            - Pastikan jumlah item pada "pilihan_ganda" mendekati/sama dengan $pgCount dan "esai" sama dengan $essayCount (kecuali salah satunya bernilai 0).
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

            val parsed = moshi.adapter(ExamQuestions::class.java).fromJson(cleanJsonStr)
                ?: throw InvalidAiResponseException("Kesalahan permintaan yakni jawaban ai tidak sesuai")

            com.example.model.ExamScoring.validateAndNormalizeExam(
                exam = parsed,
                requestedGlobalOptionCount = effectiveCount
            )
        } catch (e: InvalidAiResponseException) {
            throw e
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
        model: String,
        learnerContext: String = ""
    ): List<EssayGradingItem>? {
        val textBuilder = StringBuilder()
        textBuilder.append("Silakan nilai jawaban esai berikut:\n\n")
        essayQuestionsAndAnswers.forEachIndexed { index, pair ->
            val q = pair.first
            val ans = pair.second
            textBuilder.append("--- SOAL ${index + 1} ---\n")
            textBuilder.append("Pertanyaan: ${q.soal}\n")
            textBuilder.append("Kunci Esensi Jawab: ${q.kunci_esensi}\n")
            textBuilder.append("Jawaban Pelajar: $ans\n\n")
        }

        val contextSection = if (learnerContext.isNotBlank()) {
            "\nKonteks Pelajar: $learnerContext. Sesuaikan kedalaman umpan balik dengan jenjang tersebut."
        } else ""

        val systemPrompt = """
            Anda adalah pengajar profesional yang mengevaluasi jawaban esai. Tugas Anda adalah memberikan penilaian yang adil dan membangun untuk masing-masing esai.$contextSection

            Untuk tiap jawaban, bandingkan dengan 'Kunci Esensi Jawab'.
            Berikan skor numerik (skor) antara 0 hingga 100 berdasarkan ketepatan deskripsi, dan tulis umpan balik (umpan_balik) dalam bahasa Indonesia tentang apa yang sudah tepat dan apa yang perlu dilengkapi.
            Anda boleh menggunakan format Markdown (**tebal**, daftar poin, kode, atau tabel) pada teks "umpan_balik" agar mudah dibaca.

            Anda WAJIB memberikan respons murni dalam JSON terstruktur:
            {
               "evaluasi": [
                   {
                       "skor": 85,
                       "umpan_balik": "Penjelasan sangat lengkap dan memahami inti persoalan. Bagus!"
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
