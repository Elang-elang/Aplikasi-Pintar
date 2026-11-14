package com.example.model

import com.example.api.ExamQuestions
import com.example.api.InvalidAiResponseException
import com.example.api.PgQuestion
import kotlin.math.roundToInt

enum class PgEvalStatus {
    EXACT,
    PARTIAL,
    WRONG
}

data class PgQuestionEvaluation(
    val questionIndex: Int,
    val selectedKeys: List<String>,
    val correctKeys: List<String>,
    val accuracy: Double, // 0.0..1.0
    val status: PgEvalStatus
)

data class PgExamScoreSummary(
    val pgScorePercent: Int,
    val exactCorrectCount: Int,
    val partialCount: Int,
    val totalQuestions: Int,
    val totalAccuracyPoints: Double,
    val evaluations: List<PgQuestionEvaluation>
)

data class SelectionUpdateResult(
    val updatedValue: String?,
    val limitReached: Boolean = false
)

object ExamScoring {

    const val MIN_GLOBAL_OPTIONS = 3
    const val MAX_GLOBAL_OPTIONS = 6

    fun optionLettersForCount(count: Int): List<String> {
        val safeCount = count.coerceIn(MIN_GLOBAL_OPTIONS, MAX_GLOBAL_OPTIONS)
        return (0 until safeCount).map { idx -> ('a' + idx).toString() }
    }

    fun optionRangeDisplay(count: Int): String {
        val letters = optionLettersForCount(count)
        return "${letters.first().uppercase()}–${letters.last().uppercase()}"
    }

    /**
     * Validates and normalizes AI-generated exam JSON so that:
     * 1. A single global PG option count (3..6, e.g. A-D or A-E) applies uniformly to ALL PG questions.
     * 2. Every PG question has all required option keys ('a'..'f') with non-blank text.
     * 3. Every PG question has valid single or multi-answer keys within the global option set.
     */
    fun validateAndNormalizeExam(
        exam: ExamQuestions,
        requestedGlobalOptionCount: Int? = null
    ): ExamQuestions {
        if (exam.pilihan_ganda.isEmpty() && exam.esai.isEmpty()) {
            throw InvalidAiResponseException("Paket soal dari AI kosong")
        }

        if (exam.pilihan_ganda.isEmpty()) {
            return exam.copy(
                jumlah_opsi_pg = requestedGlobalOptionCount ?: exam.jumlah_opsi_pg
            )
        }

        val inferredFromFirst = exam.pilihan_ganda.first().pilihan.keys
            .map { it.trim().lowercase() }
            .filter { it.length == 1 && it[0] in 'a'..'f' }
            .distinct()
            .size

        val resolvedOptionCount = when {
            requestedGlobalOptionCount != null -> {
                requestedGlobalOptionCount.coerceIn(MIN_GLOBAL_OPTIONS, MAX_GLOBAL_OPTIONS)
            }
            exam.jumlah_opsi_pg != null && exam.jumlah_opsi_pg in MIN_GLOBAL_OPTIONS..MAX_GLOBAL_OPTIONS -> {
                exam.jumlah_opsi_pg
            }
            inferredFromFirst in MIN_GLOBAL_OPTIONS..MAX_GLOBAL_OPTIONS -> {
                inferredFromFirst
            }
            else -> {
                throw InvalidAiResponseException("Jumlah pilihan ganda global tidak valid (harus 3 hingga 6 opsi)")
            }
        }

        val requiredLetters = optionLettersForCount(resolvedOptionCount)
        val requiredSet = requiredLetters.toSet()

        val normalizedPgs = exam.pilihan_ganda.mapIndexed { idx, rawPg ->
            if (rawPg.soal.isBlank()) {
                throw InvalidAiResponseException("Teks soal PG ke-${idx + 1} kosong")
            }

            val cleanedOptions = linkedMapOf<String, String>()
            rawPg.pilihan.forEach { (rawKey, rawValue) ->
                val cleanKey = rawKey.trim().lowercase()
                val cleanValue = rawValue.trim()
                if (cleanKey.isNotBlank() && cleanValue.isNotBlank()) {
                    cleanedOptions[cleanKey] = cleanValue
                }
            }

            if (cleanedOptions.keys != requiredSet) {
                throw InvalidAiResponseException(
                    "Soal PG ke-${idx + 1} tidak konsisten dengan aturan global $resolvedOptionCount opsi (${optionRangeDisplay(resolvedOptionCount)})"
                )
            }

            val orderedOptions = linkedMapOf<String, String>()
            for (letter in requiredLetters) {
                orderedOptions[letter] = cleanedOptions[letter]
                    ?: throw InvalidAiResponseException("Opsi '$letter' hilang pada soal PG ke-${idx + 1}")
            }

            val tempPg = rawPg.copy(pilihan = orderedOptions)
            val correctKeys = tempPg.resolvedCorrectKeys()
            if (correctKeys.isEmpty() || correctKeys.any { it !in requiredSet }) {
                throw InvalidAiResponseException(
                    "Kunci jawaban pada soal PG ke-${idx + 1} tidak valid untuk opsi ${optionRangeDisplay(resolvedOptionCount)}"
                )
            }

            val maxSelections = maxOf(
                rawPg.maks_pilihan.coerceAtLeast(1),
                correctKeys.size
            ).coerceAtMost(resolvedOptionCount)

            PgQuestion(
                soal = rawPg.soal.trim(),
                pilihan = orderedOptions,
                kunci = correctKeys.joinToString(","),
                kunci_jawaban = correctKeys,
                maks_pilihan = maxSelections
            )
        }

        return ExamQuestions(
            jumlah_opsi_pg = resolvedOptionCount,
            pilihan_ganda = normalizedPgs,
            esai = exam.esai.map { it.copy(soal = it.soal.trim(), kunci_esensi = it.kunci_esensi.trim()) }
        )
    }

    fun parseSelectedKeys(rawSelection: String?): List<String> {
        if (rawSelection.isNullOrBlank()) return emptyList()
        return rawSelection
            .split(",", ";", " ")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun formatSelectedKeys(keys: Collection<String>): String? {
        val normalized = keys
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        return if (normalized.isEmpty()) null else normalized.joinToString(",")
    }

    fun togglePgSelection(
        currentSelection: String?,
        clickedKey: String,
        maxSelections: Int
    ): SelectionUpdateResult {
        val cleanKey = clickedKey.trim().lowercase()
        if (cleanKey.isBlank()) return SelectionUpdateResult(currentSelection)

        val safeMax = maxSelections.coerceAtLeast(1)
        if (safeMax == 1) {
            return SelectionUpdateResult(updatedValue = cleanKey, limitReached = false)
        }

        val currentSet = parseSelectedKeys(currentSelection).toMutableSet()
        return if (cleanKey in currentSet) {
            currentSet.remove(cleanKey)
            SelectionUpdateResult(updatedValue = formatSelectedKeys(currentSet), limitReached = false)
        } else if (currentSet.size < safeMax) {
            currentSet.add(cleanKey)
            SelectionUpdateResult(updatedValue = formatSelectedKeys(currentSet), limitReached = false)
        } else {
            SelectionUpdateResult(updatedValue = currentSelection, limitReached = true)
        }
    }

    fun evaluatePgQuestion(
        questionIndex: Int,
        pg: PgQuestion,
        rawStudentAnswer: String?
    ): PgQuestionEvaluation {
        val correctKeys = pg.resolvedCorrectKeys()
        val correctSet = correctKeys.toSet()
        val selectedKeys = parseSelectedKeys(rawStudentAnswer)
        val selectedSet = selectedKeys.toSet()

        val accuracy = when {
            correctSet.isEmpty() || selectedSet.isEmpty() -> 0.0
            !pg.isMultiAnswer -> {
                if (selectedSet == correctSet) 1.0 else 0.0
            }
            else -> {
                val correctChosen = selectedSet.count { it in correctSet }
                val wrongChosen = selectedSet.count { it !in correctSet }
                ((correctChosen - wrongChosen).coerceAtLeast(0).toDouble() / correctSet.size.toDouble())
                    .coerceIn(0.0, 1.0)
            }
        }

        val status = when {
            accuracy >= 0.999 -> PgEvalStatus.EXACT
            accuracy > 0.0 -> PgEvalStatus.PARTIAL
            else -> PgEvalStatus.WRONG
        }

        return PgQuestionEvaluation(
            questionIndex = questionIndex,
            selectedKeys = selectedKeys,
            correctKeys = correctKeys,
            accuracy = accuracy,
            status = status
        )
    }

    fun calculatePgExamSummary(
        questions: List<PgQuestion>,
        studentAnswers: Map<Int, String>
    ): PgExamScoreSummary {
        if (questions.isEmpty()) {
            return PgExamScoreSummary(
                pgScorePercent = 0,
                exactCorrectCount = 0,
                partialCount = 0,
                totalQuestions = 0,
                totalAccuracyPoints = 0.0,
                evaluations = emptyList()
            )
        }

        val evaluations = questions.mapIndexed { idx, pg ->
            evaluatePgQuestion(
                questionIndex = idx,
                pg = pg,
                rawStudentAnswer = studentAnswers[idx]
            )
        }

        val totalAccuracy = evaluations.sumOf { it.accuracy }
        val exactCount = evaluations.count { it.status == PgEvalStatus.EXACT }
        val partialCount = evaluations.count { it.status == PgEvalStatus.PARTIAL }
        val scorePercent = ((totalAccuracy / questions.size.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        return PgExamScoreSummary(
            pgScorePercent = scorePercent,
            exactCorrectCount = exactCount,
            partialCount = partialCount,
            totalQuestions = questions.size,
            totalAccuracyPoints = totalAccuracy,
            evaluations = evaluations
        )
    }
}
