package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ApiBaseUrlType
import com.example.api.EssayGradingItem
import com.example.api.EssayQuestion
import com.example.api.ExamQuestions
import com.example.api.GeminiApiClient
import com.example.database.ExamAttempt
import com.example.database.ExamDatabase
import com.example.database.SavedExam
import com.example.database.UserSession
import com.example.model.EducationCatalog
import com.example.model.SchoolLevel
import com.example.util.DocumentExtractionResult
import com.example.util.DocumentTextExtractor
import com.example.util.ExtractedDocument
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AttemptStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

data class AttemptLog(
    val attemptNumber: Int,
    val status: AttemptStatus = AttemptStatus.PENDING,
    val message: String = "Menunggu..."
)

data class ApiTestUiState(
    val isRunning: Boolean = false,
    val currentAttempt: Int = 0,
    val maxAttempts: Int = 5,
    val remainingSeconds: Int = 300, // 5 minutes timeout
    val attempts: List<AttemptLog> = (1..5).map { AttemptLog(it) },
    val result: Boolean? = null, // true = success, false = failed, null = idle/running
    val errorDetail: String = ""
)

enum class StudyExplainMode(val label: String, val instruction: String) {
    EXPLAIN_SIMPLE(
        label = "Jelaskan Materi",
        instruction = "Jelaskan materi atau dokumen berikut secara runtut, jelas, dan mudah dipahami untuk belajar. Sertakan analogi sederhana dan poin kunci."
    ),
    SUMMARY(
        label = "Ringkasan Belajar",
        instruction = "Buat ringkasan belajar yang terstruktur berisi konsep inti, istilah penting, dan poin-poin utama agar mudah dipelajari kembali."
    ),
    EXAMPLES(
        label = "Contoh & Latihan",
        instruction = "Berikan contoh penerapan nyata dari materi ini serta 3 pertanyaan pemahaman singkat beserta pembahasan singkatnya untuk melatih pemahaman."
    ),
    FREE_CHAT(
        label = "Tanya Jawab",
        instruction = "Jawab pertanyaan belajar pengguna secara ramah, akurat, dan edukatif."
    )
}

data class StudyChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val displayText: String,
    val apiContent: String,
    val attachedDocName: String? = null,
    val modeLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ExamDatabase.getDatabase(application)
    private val dao = db.examDao
    private val sharedPrefs = application.getSharedPreferences("exam_settings", Context.MODE_PRIVATE)

    // User session flow (non-code profile identity only)
    private val _userSession = MutableStateFlow<UserSession?>(null)
    val userSession: StateFlow<UserSession?> = _userSession.asStateFlow()

    // Recent attempts list
    private val _pastAttempts = MutableStateFlow<List<ExamAttempt>>(emptyList())
    val pastAttempts: StateFlow<List<ExamAttempt>> = _pastAttempts.asStateFlow()

    // ACTIVE EXAM STATE
    private val _activeExam = MutableStateFlow<ExamQuestions?>(null)
    val activeExam: StateFlow<ExamQuestions?> = _activeExam.asStateFlow()

    private val _kisiKisiText = MutableStateFlow("")
    val kisiKisiText: StateFlow<String> = _kisiKisiText.asStateFlow()

    // Attached Kisi-Kisi Document State (< 5 MB, converted to T2T structured text)
    private val _attachedDocument = MutableStateFlow<ExtractedDocument?>(null)
    val attachedDocument: StateFlow<ExtractedDocument?> = _attachedDocument.asStateFlow()

    private val _documentExtractionState = MutableStateFlow<UiState<ExtractedDocument>>(UiState.Idle)
    val documentExtractionState: StateFlow<UiState<ExtractedDocument>> = _documentExtractionState.asStateFlow()

    // Study Chatbot & Explanation State
    private val _studyMessages = MutableStateFlow<List<StudyChatMessage>>(emptyList())
    val studyMessages: StateFlow<List<StudyChatMessage>> = _studyMessages.asStateFlow()

    private val _studyChatState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val studyChatState: StateFlow<UiState<Unit>> = _studyChatState.asStateFlow()

    private val _studyAttachedDocument = MutableStateFlow<ExtractedDocument?>(null)
    val studyAttachedDocument: StateFlow<ExtractedDocument?> = _studyAttachedDocument.asStateFlow()

    private val _studyDocExtractionState = MutableStateFlow<UiState<ExtractedDocument>>(UiState.Idle)
    val studyDocExtractionState: StateFlow<UiState<ExtractedDocument>> = _studyDocExtractionState.asStateFlow()

    // Question generation state
    private val _generationState = MutableStateFlow<UiState<ExamQuestions>>(UiState.Idle)
    val generationState: StateFlow<UiState<ExamQuestions>> = _generationState.asStateFlow()

    private val _generationProgressMessage = MutableStateFlow("")
    val generationProgressMessage: StateFlow<String> = _generationProgressMessage.asStateFlow()

    // Student Answers State
    private val _studentPgAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val studentPgAnswers: StateFlow<Map<Int, String>> = _studentPgAnswers.asStateFlow()

    private val _studentEssayAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val studentEssayAnswers: StateFlow<Map<Int, String>> = _studentEssayAnswers.asStateFlow()

    // Grading State
    private val _gradingState = MutableStateFlow<UiState<ExamAttempt>>(UiState.Idle)
    val gradingState: StateFlow<UiState<ExamAttempt>> = _gradingState.asStateFlow()

    // Session loaded flag for safe initial navigation
    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    // Manual User-Configured API State (no built-in/hardcoded Base URL or API Key)
    private val _baseUrl = MutableStateFlow(sharedPrefs.getString("base_url", "") ?: "")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _baseUrlType = MutableStateFlow(
        ApiBaseUrlType.fromKey(sharedPrefs.getString("base_url_type", ApiBaseUrlType.WITH_V1.key))
    )
    val baseUrlType: StateFlow<ApiBaseUrlType> = _baseUrlType.asStateFlow()

    private val _customApiKey = MutableStateFlow(sharedPrefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _modelName = MutableStateFlow(sharedPrefs.getString("model_name", "") ?: "")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _isApiConfigured = MutableStateFlow(sharedPrefs.getBoolean("is_api_configured", false))
    val isApiConfigured: StateFlow<Boolean> = _isApiConfigured.asStateFlow()

    // Connection test & toggle message states
    private val _apiTestState = MutableStateFlow(ApiTestUiState())
    val apiTestState: StateFlow<ApiTestUiState> = _apiTestState.asStateFlow()

    private val _apiConfigFailureToggleMessage = MutableStateFlow<String?>(null)
    val apiConfigFailureToggleMessage: StateFlow<String?> = _apiConfigFailureToggleMessage.asStateFlow()

    private val _apiConfigFailureDetail = MutableStateFlow<String?>(null)
    val apiConfigFailureDetail: StateFlow<String?> = _apiConfigFailureDetail.asStateFlow()

    private var apiTestJob: Job? = null
    private var apiTestCountdownJob: Job? = null

    // Configurable number of questions
    private val _selectedPgCount = MutableStateFlow(sharedPrefs.getInt("selected_pg_count", 45))
    val selectedPgCount: StateFlow<Int> = _selectedPgCount.asStateFlow()

    private val _selectedEssayCount = MutableStateFlow(sharedPrefs.getInt("selected_essay_count", 5))
    val selectedEssayCount: StateFlow<Int> = _selectedEssayCount.asStateFlow()

    // Theme state (Light-first clean academic white & blue by default)
    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme_academic_v1", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Timer states
    private val _selectedTimerMinutes = MutableStateFlow(sharedPrefs.getInt("selected_timer_minutes", 90))
    val selectedTimerMinutes: StateFlow<Int> = _selectedTimerMinutes.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadSession()
        loadPastAttempts()
        loadActiveExam()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val session = dao.getUserSession()
            _userSession.value = session
            _isSessionLoaded.value = true
        }
    }

    private fun loadPastAttempts() {
        viewModelScope.launch {
            dao.getAllAttempts()
                .distinctUntilChanged()
                .catch { Log.e("ExamViewModel", "Error fetching attempts", it) }
                .collect { attempts ->
                    _pastAttempts.value = attempts
                }
        }
    }

    private fun loadActiveExam() {
        viewModelScope.launch {
            val saved = dao.getSavedExam()
            if (saved != null) {
                _kisiKisiText.value = saved.kisiKisi
                try {
                    val exam = GeminiApiClient.moshi.adapter(ExamQuestions::class.java).fromJson(saved.questionsJson)
                    _activeExam.value = exam
                } catch (e: Exception) {
                    Log.e("ExamViewModel", "Failed to decode saved active exam", e)
                }
            }
        }
    }

    fun login(nama: String, sekolah: String, kelas: String, bidang: String) {
        viewModelScope.launch {
            val cleanLevel = SchoolLevel.fromDisplayName(sekolah)?.displayName
                ?: sekolah.trim().ifBlank { SchoolLevel.SMA.displayName }
            val session = UserSession(
                nama = nama.trim(),
                sekolah = cleanLevel,
                kelas = kelas.trim().ifBlank { "10" },
                bidang = bidang.trim().ifBlank { "Umum" },
                isLogin = true
            )
            dao.saveUserSession(session)
            _userSession.value = session
        }
    }

    fun logout() {
        viewModelScope.launch {
            cancelConnectionTest()
            _isApiConfigured.value = false
            _apiConfigFailureToggleMessage.value = null
            _apiConfigFailureDetail.value = null
            sharedPrefs.edit().putBoolean("is_api_configured", false).apply()
            dao.clearUserSession()
            dao.clearSavedExam()
            _userSession.value = null
            _activeExam.value = null
            _studentPgAnswers.value = emptyMap()
            _studentEssayAnswers.value = emptyMap()
            _gradingState.value = UiState.Idle
            _generationState.value = UiState.Idle
            _attachedDocument.value = null
            _documentExtractionState.value = UiState.Idle
            _studyMessages.value = emptyList()
            _studyChatState.value = UiState.Idle
            _studyAttachedDocument.value = null
            _studyDocExtractionState.value = UiState.Idle
            stopExamTimer()
        }
    }

    fun toggleTheme() {
        val newVal = !_isDarkTheme.value
        _isDarkTheme.value = newVal
        sharedPrefs.edit().putBoolean("is_dark_theme_academic_v1", newVal).apply()
    }

    fun setTimerMinutes(minutes: Int) {
        _selectedTimerMinutes.value = minutes
        sharedPrefs.edit().putInt("selected_timer_minutes", minutes).apply()
    }

    fun startExamTimer(onTimeUp: () -> Unit) {
        timerJob?.cancel()
        _secondsRemaining.value = _selectedTimerMinutes.value * 60
        timerJob = viewModelScope.launch {
            while (_secondsRemaining.value > 0) {
                delay(1000)
                _secondsRemaining.value -= 1
            }
            onTimeUp()
        }
    }

    fun stopExamTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun dismissApiFailureMessage() {
        _apiConfigFailureToggleMessage.value = null
        _apiConfigFailureDetail.value = null
    }

    fun resetApiTestResult() {
        _apiTestState.value = _apiTestState.value.copy(result = null)
    }

    fun saveDraftApiConfig(
        baseUrl: String,
        baseUrlType: ApiBaseUrlType,
        apiKey: String,
        model: String
    ) {
        val cleanUrl = GeminiApiClient.normalizeBaseUrlInput(baseUrl)
        val cleanKey = apiKey.trim()
        val cleanModel = model.trim()

        _baseUrl.value = cleanUrl
        _baseUrlType.value = baseUrlType
        _customApiKey.value = cleanKey
        _modelName.value = cleanModel
        _isApiConfigured.value = false

        sharedPrefs.edit()
            .putString("base_url", cleanUrl)
            .putString("base_url_type", baseUrlType.key)
            .putString("custom_api_key", cleanKey)
            .putString("model_name", cleanModel)
            .putBoolean("is_api_configured", false)
            .apply()
    }

    /**
     * Tests Base URL + API Key + Base URL Type with max 5 requests and 5-minute (300s) timeout.
     * If 1 succeeds -> entire test succeeds and marks config as verified.
     * If all 5 fail or 5-minute timeout expires -> fails and sets toggle failure message.
     */
    fun startConnectionTest(
        baseUrl: String,
        baseUrlType: ApiBaseUrlType,
        apiKey: String,
        model: String
    ) {
        saveDraftApiConfig(baseUrl, baseUrlType, apiKey, model)
        dismissApiFailureMessage()

        apiTestJob?.cancel()
        apiTestCountdownJob?.cancel()

        val totalTimeoutSeconds = 300 // 5 minutes
        val initialLogs = (1..5).map { AttemptLog(it, AttemptStatus.PENDING, "Menunggu giliran...") }
        _apiTestState.value = ApiTestUiState(
            isRunning = true,
            currentAttempt = 0,
            maxAttempts = 5,
            remainingSeconds = totalTimeoutSeconds,
            attempts = initialLogs,
            result = null,
            errorDetail = ""
        )

        apiTestCountdownJob = viewModelScope.launch {
            while (_apiTestState.value.isRunning && _apiTestState.value.remainingSeconds > 0) {
                delay(1000)
                if (_apiTestState.value.isRunning) {
                    val nextSec = (_apiTestState.value.remainingSeconds - 1).coerceAtLeast(0)
                    _apiTestState.value = _apiTestState.value.copy(remainingSeconds = nextSec)
                }
            }
        }

        apiTestJob = viewModelScope.launch {
            val cleanUrl = GeminiApiClient.normalizeBaseUrlInput(baseUrl)
            val cleanKey = apiKey.trim()
            val cleanModel = model.trim()
            var succeeded = false
            var lastError = "Tidak ada respons dari server."

            try {
                withTimeout(totalTimeoutSeconds * 1000L) {
                    for (attempt in 1..5) {
                        val runningLogs = _apiTestState.value.attempts.map {
                            if (it.attemptNumber == attempt) {
                                it.copy(status = AttemptStatus.RUNNING, message = "Mengirim request ke-$attempt...")
                            } else it
                        }
                        _apiTestState.value = _apiTestState.value.copy(
                            currentAttempt = attempt,
                            attempts = runningLogs
                        )

                        try {
                            GeminiApiClient.testConnection(
                                baseUrl = cleanUrl,
                                baseUrlType = baseUrlType,
                                apiKey = cleanKey,
                                model = cleanModel
                            )
                            val successLogs = _apiTestState.value.attempts.map {
                                if (it.attemptNumber == attempt) {
                                    it.copy(status = AttemptStatus.SUCCESS, message = "Berhasil terhubung!")
                                } else it
                            }
                            _apiTestState.value = _apiTestState.value.copy(attempts = successLogs)
                            succeeded = true
                            break
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            lastError = e.message ?: "Request gagal"
                            Log.e("ExamViewModel", "Connection test attempt $attempt failed", e)
                            val failedLogs = _apiTestState.value.attempts.map {
                                if (it.attemptNumber == attempt) {
                                    it.copy(status = AttemptStatus.FAILED, message = "Gagal: ${lastError.take(80)}")
                                } else it
                            }
                            _apiTestState.value = _apiTestState.value.copy(attempts = failedLogs)
                        }

                        if (attempt < 5) {
                            delay(1500)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                lastError = "Timeout 5 menit terlampaui sebelum koneksi berhasil."
                Log.e("ExamViewModel", "Connection test 5-minute timeout exceeded", e)
            } catch (e: CancellationException) {
                apiTestCountdownJob?.cancel()
                return@launch
            }

            apiTestCountdownJob?.cancel()

            if (succeeded) {
                _isApiConfigured.value = true
                sharedPrefs.edit().putBoolean("is_api_configured", true).apply()
                _apiTestState.value = _apiTestState.value.copy(
                    isRunning = false,
                    result = true,
                    errorDetail = ""
                )
            } else {
                _isApiConfigured.value = false
                sharedPrefs.edit().putBoolean("is_api_configured", false).apply()
                _apiConfigFailureToggleMessage.value = "Base URL atau API Key gagal, atur ulang Base URL & API Key."
                _apiConfigFailureDetail.value = lastError
                _apiTestState.value = _apiTestState.value.copy(
                    isRunning = false,
                    result = false,
                    errorDetail = lastError
                )
            }
        }
    }

    fun cancelConnectionTest() {
        apiTestJob?.cancel()
        apiTestCountdownJob?.cancel()
        apiTestJob = null
        apiTestCountdownJob = null
        _apiTestState.value = ApiTestUiState()
    }

    // --- Kisi-Kisi Document Extraction (< 5 MB) ---
    fun processKisiKisiDocument(uri: Uri) {
        _documentExtractionState.value = UiState.Loading
        viewModelScope.launch {
            when (val res = DocumentTextExtractor.extractFromUri(getApplication(), uri)) {
                is DocumentExtractionResult.Success -> {
                    _attachedDocument.value = res.document
                    _documentExtractionState.value = UiState.Success(res.document)
                }
                is DocumentExtractionResult.Error -> {
                    _documentExtractionState.value = UiState.Error(res.message)
                }
            }
        }
    }

    fun clearAttachedDocument() {
        _attachedDocument.value = null
        _documentExtractionState.value = UiState.Idle
    }

    // --- Study Chatbot & Document/Text Explanation ---
    fun processStudyDocument(uri: Uri) {
        _studyDocExtractionState.value = UiState.Loading
        viewModelScope.launch {
            when (val res = DocumentTextExtractor.extractFromUri(getApplication(), uri)) {
                is DocumentExtractionResult.Success -> {
                    _studyAttachedDocument.value = res.document
                    _studyDocExtractionState.value = UiState.Success(res.document)
                }
                is DocumentExtractionResult.Error -> {
                    _studyDocExtractionState.value = UiState.Error(res.message)
                }
            }
        }
    }

    fun clearStudyDocument() {
        _studyAttachedDocument.value = null
        _studyDocExtractionState.value = UiState.Idle
    }

    fun clearStudyChat() {
        _studyMessages.value = emptyList()
        _studyChatState.value = UiState.Idle
        _studyAttachedDocument.value = null
        _studyDocExtractionState.value = UiState.Idle
    }

    fun sendStudyMessage(userText: String, mode: StudyExplainMode) {
        val trimmedText = userText.trim()
        val attached = _studyAttachedDocument.value
        if (trimmedText.isEmpty() && attached == null) return
        if (_studyChatState.value is UiState.Loading) return

        val displayPrompt = if (trimmedText.isNotEmpty()) {
            trimmedText
        } else {
            "${mode.label}: Tolong jelaskan isi dokumen ini untuk belajar."
        }

        val apiPrompt = buildString {
            append("[Mode Belajar: ").append(mode.label).append("]\n")
            append("Instruksi gaya penjelasan: ").append(mode.instruction).append("\n\n")
            if (trimmedText.isNotEmpty()) {
                append("Uraian / Pertanyaan Pengguna:\n").append(trimmedText).append("\n\n")
            }
            if (attached != null) {
                append(attached.t2tPayload)
            }
        }.trim()

        val userMessage = StudyChatMessage(
            isUser = true,
            displayText = displayPrompt,
            apiContent = apiPrompt,
            attachedDocName = attached?.fileName,
            modeLabel = mode.label
        )

        val updatedHistory = _studyMessages.value + userMessage
        _studyMessages.value = updatedHistory
        // Clear pending attachment chip after attaching it to this turn
        _studyAttachedDocument.value = null
        _studyDocExtractionState.value = UiState.Idle
        _studyChatState.value = UiState.Loading

        viewModelScope.launch {
            val session = _userSession.value
            val learnerName = session?.nama?.takeIf { it.isNotBlank() } ?: "Pelajar"
            val profileLabel = EducationCatalog.formatProfileBadge(
                sekolah = session?.sekolah,
                kelas = session?.kelas,
                bidang = session?.bidang
            )
            val depthGuidance = EducationCatalog.tutorDepthGuidance(
                sekolah = session?.sekolah,
                kelas = session?.kelas,
                bidang = session?.bidang
            )
            val systemPrompt = """
                Anda adalah pendamping belajar yang sabar, jelas, dan terstruktur.
                Anda sedang membantu $learnerName ($profileLabel) memahami materi dari teks atau dokumen yang diberikan.
                Penyesuaian tingkat belajar:
                $depthGuidance
                Aturan penjelasan:
                1. Gunakan Bahasa Indonesia yang alami, runtut, dan mudah dipahami.
                2. Pecah konsep yang sulit menjadi langkah-langkah logis beserta contoh konkret yang relevan.
                3. Jika pengguna melampirkan dokumen (PDF, DOCX, PPTX, dll. yang telah diekstrak menjadi teks), rujuk isi dokumen secara akurat tanpa mengarang informasi di luar konteks jika tidak diminta.
                4. Akhiri dengan ringkasan inti singkat atau pertanyaan pancingan ringan agar pemahaman semakin kuat.
            """.trimIndent()

            val apiConversation = updatedHistory.map { msg ->
                Pair(if (msg.isUser) "user" else "assistant", msg.apiContent)
            }

            try {
                val reply = GeminiApiClient.sendStudyChatCompletion(
                    baseUrl = _baseUrl.value,
                    baseUrlType = _baseUrlType.value,
                    apiKey = _customApiKey.value,
                    model = _modelName.value,
                    systemPrompt = systemPrompt,
                    conversationHistory = apiConversation
                )
                val aiMessage = StudyChatMessage(
                    isUser = false,
                    displayText = reply,
                    apiContent = reply
                )
                _studyMessages.value = _studyMessages.value + aiMessage
                _studyChatState.value = UiState.Success(Unit)
            } catch (e: com.example.api.ConnectionException) {
                Log.e("ExamViewModel", "Study chat connection error", e)
                _studyChatState.value = UiState.Error("Error Karena koneksi internet anda")
            } catch (e: com.example.api.RateLimitException) {
                Log.e("ExamViewModel", "Study chat rate limit error", e)
                _studyChatState.value = UiState.Error("Kesalahan permintaan karena permintaan telah limit")
            } catch (e: com.example.api.InvalidAiResponseException) {
                Log.e("ExamViewModel", "Study chat invalid response error", e)
                _studyChatState.value = UiState.Error("Kesalahan permintaan yakni jawaban ai tidak sesuai")
            } catch (e: Exception) {
                Log.e("ExamViewModel", "Study chat error", e)
                _studyChatState.value = UiState.Error(
                    e.message ?: "Gagal mendapatkan penjelasan dari AI. Periksa koneksi atau konfigurasi API Anda."
                )
            }
        }
    }

    fun setQuestionScale(pg: Int, essay: Int) {
        _selectedPgCount.value = pg
        _selectedEssayCount.value = essay
    }

    fun generateQuestionsFromKisiKisi(manualKisiKisi: String) {
        val docPayload = _attachedDocument.value?.t2tPayload
        val combinedKisiKisi = buildString {
            val trimmedManual = manualKisiKisi.trim()
            if (trimmedManual.isNotEmpty()) {
                append(trimmedManual)
            }
            if (!docPayload.isNullOrBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(docPayload)
            }
        }
        _kisiKisiText.value = combinedKisiKisi
        _generationState.value = UiState.Loading

        viewModelScope.launch {
            var finalExam: ExamQuestions? = null
            var success = false
            val maxRetries = 5
            var lastErrorMsg = "Mungkin server sedang error atau terkena limit, coba lagi beberapa saat!"

            for (attempt in 1..maxRetries) {
                _generationProgressMessage.value = "Menghubungi AI (Percobaan $attempt dari $maxRetries)..."

                try {
                    val session = _userSession.value
                    val learnerContext = if (session != null) {
                        val badge = EducationCatalog.formatProfileBadge(session.sekolah, session.kelas, session.bidang)
                        "Pelajar: ${session.nama} ($badge). ${EducationCatalog.tutorDepthGuidance(session.sekolah, session.kelas, session.bidang)}"
                    } else ""

                    val questions = GeminiApiClient.generateExamQuestions(
                        kisiKisi = combinedKisiKisi,
                        pgCount = _selectedPgCount.value,
                        essayCount = _selectedEssayCount.value,
                        baseUrl = _baseUrl.value,
                        baseUrlType = _baseUrlType.value,
                        customKey = _customApiKey.value,
                        model = _modelName.value,
                        learnerContext = learnerContext
                    )

                    if (questions != null && (questions.pilihan_ganda.isNotEmpty() || questions.esai.isNotEmpty())) {
                        finalExam = questions
                        success = true
                        break
                    } else {
                        lastErrorMsg = "Kesalahan permintaan yakni jawaban ai tidak sesuai"
                    }
                } catch (e: com.example.api.ConnectionException) {
                    lastErrorMsg = "Error Karena koneksi internet anda"
                    Log.e("ExamViewModel", "ConnectionException at attempt $attempt", e)
                } catch (e: com.example.api.RateLimitException) {
                    lastErrorMsg = "Kesalahan permintaan karena permintaan telah limit"
                    Log.e("ExamViewModel", "RateLimitException at attempt $attempt", e)
                    break
                } catch (e: com.example.api.InvalidAiResponseException) {
                    lastErrorMsg = "Kesalahan permintaan yakni jawaban ai tidak sesuai"
                    Log.e("ExamViewModel", "InvalidAiResponseException at attempt $attempt", e)
                } catch (e: Exception) {
                    lastErrorMsg = e.message ?: "Terjadi kesalahan tidak diketahui"
                    Log.e("ExamViewModel", "Exception at attempt $attempt", e)
                }

                if (attempt < maxRetries) {
                    delay(3000)
                }
            }

            if (success && finalExam != null) {
                _studentPgAnswers.value = emptyMap()
                _studentEssayAnswers.value = emptyMap()
                _gradingState.value = UiState.Idle
                _activeExam.value = finalExam

                val questionsJsonStr = GeminiApiClient.moshi.adapter(ExamQuestions::class.java).toJson(finalExam)
                dao.saveExam(SavedExam(kisiKisi = combinedKisiKisi, questionsJson = questionsJsonStr))

                _generationState.value = UiState.Success(finalExam)
            } else {
                _generationState.value = UiState.Error(lastErrorMsg)
            }
        }
    }

    fun selectPgAnswer(questionIndex: Int, answerKey: String) {
        val updated = _studentPgAnswers.value.toMutableMap()
        updated[questionIndex] = answerKey.lowercase()
        _studentPgAnswers.value = updated
    }

    fun setEssayAnswer(questionIndex: Int, text: String) {
        val updated = _studentEssayAnswers.value.toMutableMap()
        updated[questionIndex] = text
        _studentEssayAnswers.value = updated
    }

    fun submitExamAnswers() {
        val exam = _activeExam.value ?: return
        val session = _userSession.value ?: UserSession(
            nama = "Pelajar",
            sekolah = SchoolLevel.SMA.displayName,
            kelas = "10",
            bidang = "Umum",
            isLogin = true
        )

        _gradingState.value = UiState.Loading

        viewModelScope.launch {
            var correctPgCount = 0
            val pgTotal = exam.pilihan_ganda.size
            exam.pilihan_ganda.forEachIndexed { index, pg ->
                val chosen = _studentPgAnswers.value[index]
                if (chosen != null && chosen.trim().lowercase() == pg.kunci.trim().lowercase()) {
                    correctPgCount++
                }
            }

            val pgScore = if (pgTotal > 0) (correctPgCount * 100) / pgTotal else 0

            val essayGrades = mutableListOf<EssayGradingItem>()
            var essaySuccess = false

            val essaysTotal = exam.esai.size
            var lastGradingError = "Mungkin server sedang error atau terkena limit"
            if (essaysTotal > 0) {
                val gradingRequestItems = exam.esai.mapIndexed { index, essayQuestion ->
                    val studentAnswer = _studentEssayAnswers.value[index] ?: ""
                    Pair(essayQuestion, studentAnswer)
                }
                val learnerContext = EducationCatalog.formatProfileBadge(
                    session.sekolah,
                    session.kelas,
                    session.bidang
                )

                stopExamTimer()
                for (retry in 1..3) {
                    try {
                        val evaluations = GeminiApiClient.gradeEssayAnswers(
                            essayQuestionsAndAnswers = gradingRequestItems,
                            baseUrl = _baseUrl.value,
                            baseUrlType = _baseUrlType.value,
                            customKey = _customApiKey.value,
                            model = _modelName.value,
                            learnerContext = learnerContext
                        )
                        if (evaluations != null && evaluations.isNotEmpty()) {
                            essayGrades.addAll(evaluations)
                            essaySuccess = true
                            break
                        } else {
                            lastGradingError = "Kesalahan permintaan yakni jawaban ai tidak sesuai"
                        }
                    } catch (e: com.example.api.ConnectionException) {
                        lastGradingError = "Error Karena koneksi internet anda"
                        Log.e("ExamViewModel", "ConnectionException in essay evaluation retry $retry", e)
                    } catch (e: com.example.api.RateLimitException) {
                        lastGradingError = "Kesalahan permintaan karena permintaan telah limit"
                        Log.e("ExamViewModel", "RateLimitException in essay evaluation retry $retry", e)
                        break
                    } catch (e: com.example.api.InvalidAiResponseException) {
                        lastGradingError = "Kesalahan permintaan yakni jawaban ai tidak sesuai"
                        Log.e("ExamViewModel", "InvalidAiResponseException in essay evaluation retry $retry", e)
                    } catch (e: Exception) {
                        lastGradingError = e.message ?: "Terjadi kesalahan tidak diketahui"
                        Log.e("ExamViewModel", "Exception in essay evaluation retry $retry", e)
                    }
                    if (retry < 3) delay(2000)
                }
            } else {
                essaySuccess = true
            }

            if (!essaySuccess && essaysTotal > 0) {
                exam.esai.forEach { _ ->
                    essayGrades.add(EssayGradingItem(skor = 0, umpan_balik = "Gagal menilai esai secara otomatis. Detail: $lastGradingError"))
                }
            }

            val essayAvgScore = if (essayGrades.isNotEmpty()) {
                essayGrades.map { it.skor }.average().toInt()
            } else {
                100
            }

            val finalScore = if (essaysTotal > 0) {
                (pgScore + essayAvgScore) / 2
            } else {
                pgScore
            }

            val pgAnswersMapAdapter = GeminiApiClient.moshi.adapter(Map::class.java)
            val answersJsonStr = pgAnswersMapAdapter.toJson(
                mapOf(
                    "pg" to _studentPgAnswers.value,
                    "essay" to _studentEssayAnswers.value
                )
            )

            val gradingListAdapter = GeminiApiClient.moshi.adapter(List::class.java)
            val gradingItemsRawList = essayGrades.map { mapOf("skor" to it.skor, "umpan_balik" to it.umpan_balik) }
            val gradingJsonStr = gradingListAdapter.toJson(gradingItemsRawList)

            val questionsJsonStr = GeminiApiClient.moshi.adapter(ExamQuestions::class.java).toJson(exam)
            val profileSummary = EducationCatalog.formatProfileBadge(session.sekolah, session.kelas, session.bidang)
            val attempt = ExamAttempt(
                studentName = session.nama,
                studentClass = profileSummary,
                studentAbsen = "",
                pgCorrectCount = correctPgCount,
                pgTotalCount = pgTotal,
                pgScore = pgScore,
                essayScore = essayAvgScore,
                finalScore = finalScore,
                answersJson = answersJsonStr,
                gradingJson = gradingJsonStr,
                emailSentStatus = "LOCAL",
                questionsJson = questionsJsonStr,
                kisiKisi = _kisiKisiText.value
            )

            val attemptId = dao.saveAttempt(attempt)
            val updatedAttempt = attempt.copy(id = attemptId.toInt())

            _gradingState.value = UiState.Success(updatedAttempt)
            dao.clearSavedExam()
        }
    }

    /**
     * Shares local study/practice results via standard Android Share Sheet (no external email broker).
     */
    fun onShareResults(context: Context, attempt: ExamAttempt) {
        val exam = _activeExam.value ?: try {
            if (attempt.questionsJson.isNotBlank()) {
                GeminiApiClient.moshi.adapter(ExamQuestions::class.java).fromJson(attempt.questionsJson)
            } else null
        } catch (_: Exception) {
            null
        }

        val listMapType = Types.newParameterizedType(List::class.java, Map::class.java)
        val gradingAdapter = GeminiApiClient.moshi.adapter<List<Map<String, Any>>>(listMapType)
        val gradingListRaw = try {
            gradingAdapter.fromJson(attempt.gradingJson) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val shareText = buildString {
            appendLine("RINGKASAN HASIL LATIHAN — PEMBELAJARAN DENGAN AI")
            appendLine("Nama: ${attempt.studentName}")
            appendLine("Profil Belajar: ${attempt.studentClass}")
            appendLine("Nilai Akhir: ${attempt.finalScore} / 100")
            appendLine("- Pilihan Ganda: ${attempt.pgScore} (${attempt.pgCorrectCount}/${attempt.pgTotalCount} benar)")
            appendLine("- Esai: ${attempt.essayScore} / 100")
            if (exam != null && exam.esai.isNotEmpty()) {
                appendLine()
                appendLine("--- EVALUASI ESAI ---")
                exam.esai.forEachIndexed { idx, q ->
                    val gradeMap = gradingListRaw.getOrNull(idx)
                    val scoreVal = (gradeMap?.get("skor") as? Double ?: gradeMap?.get("skor") as? Int ?: 0).toString()
                    val fbText = gradeMap?.get("umpan_balik") as? String ?: "-"
                    appendLine("Soal ${idx + 1}: ${q.soal}")
                    appendLine("Nilai: $scoreVal/100 | Catatan: $fbText")
                }
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Hasil Latihan Belajar - ${attempt.studentName}")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(sendIntent, "Bagikan Ringkasan Belajar...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("ExamViewModel", "Gagal membuka share sheet", e)
        }
    }

    fun onRepeatExam() {
        _activeExam.value = null
        _studentPgAnswers.value = emptyMap()
        _studentEssayAnswers.value = emptyMap()
        _gradingState.value = UiState.Idle
        _generationState.value = UiState.Idle
        _attachedDocument.value = null
        _documentExtractionState.value = UiState.Idle
    }
}
