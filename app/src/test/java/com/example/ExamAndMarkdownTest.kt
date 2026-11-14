package com.example

import com.example.api.EssayQuestion
import com.example.api.ExamQuestions
import com.example.api.InvalidAiResponseException
import com.example.api.PgQuestion
import com.example.model.ExamScoring
import com.example.model.PgEvalStatus
import com.example.ui.components.InlineMarkdownToken
import com.example.ui.components.MarkdownBlock
import com.example.ui.components.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamAndMarkdownTest {

    @Test
    fun `markdown parser extracts headings lists tables code blocks and embedded web images`() {
        val md = """
            ## Hukum Newton II

            Gaya sebanding dengan **percepatan**:
            ![Diagram Gaya](https://upload.wikimedia.org/example/newton.png)

            - Massa (`m`)
            - Percepatan (`a`)

            1. Hitung total gaya
            2. Bagi dengan massa

            > Catatan: satuan SI adalah Newton (N).

            | Besaran | Simbol | Satuan |
            | --- | --- | --- |
            | Gaya | F | N |
            | Massa | m | kg |

            ```python
            F = m * a
            ```
        """.trimIndent()

        val blocks = MarkdownParser.parseBlocks(md)
        assertTrue(blocks.any { it is MarkdownBlock.Heading && it.level == 2 && it.text == "Hukum Newton II" })
        assertTrue(blocks.any { it is MarkdownBlock.Image && it.alt == "Diagram Gaya" && it.url.endsWith("newton.png") })
        assertEquals(2, blocks.count { it is MarkdownBlock.BulletListItem })
        assertEquals(2, blocks.count { it is MarkdownBlock.OrderedListItem })
        assertTrue(blocks.any { it is MarkdownBlock.BlockQuote })
        val table = blocks.filterIsInstance<MarkdownBlock.Table>().single()
        assertEquals(listOf("Besaran", "Simbol", "Satuan"), table.headers)
        assertEquals(2, table.rows.size)
        val code = blocks.filterIsInstance<MarkdownBlock.CodeBlock>().single()
        assertEquals("python", code.language)
        assertEquals("F = m * a", code.code)
    }

    @Test
    fun `markdown inline parser keeps snake_case intact and parses bold italic code link`() {
        val tokens = MarkdownParser.parseInline(
            "Gunakan **rumus_utama** dengan *ketelitian* `f_x` dan ~~teks salah~~ di [Referensi](https://example.org)."
        )
        assertTrue(tokens.any { it is InlineMarkdownToken.Text && it.content == "rumus_utama" && it.bold })
        assertTrue(tokens.any { it is InlineMarkdownToken.Text && it.content == "ketelitian" && it.italic })
        assertTrue(tokens.any { it is InlineMarkdownToken.InlineCode && it.code == "f_x" })
        assertTrue(tokens.any { it is InlineMarkdownToken.Text && it.content == "teks salah" && it.strikethrough })
        assertTrue(tokens.any { it is InlineMarkdownToken.Link && it.label == "Referensi" && it.url == "https://example.org" })
    }

    @Test
    fun `validateAndNormalizeExam enforces global option count across all PG questions`() {
        val examFiveOptions = ExamQuestions(
            jumlah_opsi_pg = 5,
            pilihan_ganda = listOf(
                PgQuestion(
                    soal = "Soal 1",
                    pilihan = mapOf("a" to "1", "b" to "2", "c" to "3", "d" to "4", "e" to "5"),
                    kunci_jawaban = listOf("a", "e"),
                    maks_pilihan = 2
                ),
                PgQuestion(
                    soal = "Soal 2",
                    pilihan = mapOf("a" to "P", "b" to "Q", "c" to "R", "d" to "S", "e" to "T"),
                    kunci = "c"
                )
            ),
            esai = listOf(EssayQuestion("Jelaskan!", "Inti jawaban"))
        )

        val normalized = ExamScoring.validateAndNormalizeExam(examFiveOptions, requestedGlobalOptionCount = 5)
        assertEquals(5, normalized.jumlah_opsi_pg)
        assertEquals(listOf("a", "b", "c", "d", "e"), normalized.pilihan_ganda[0].pilihan.keys.toList())
        assertEquals(listOf("a", "e"), normalized.pilihan_ganda[0].kunci_jawaban)
        assertEquals(2, normalized.pilihan_ganda[0].maks_pilihan)
        assertEquals(listOf("c"), normalized.pilihan_ganda[1].kunci_jawaban)
        assertEquals(1, normalized.pilihan_ganda[1].maks_pilihan)
    }

    @Test(expected = InvalidAiResponseException::class)
    fun `validateAndNormalizeExam rejects mixed option counts within same exam`() {
        val inconsistentExam = ExamQuestions(
            jumlah_opsi_pg = 5,
            pilihan_ganda = listOf(
                PgQuestion(
                    soal = "Soal 1 (5 opsi)",
                    pilihan = mapOf("a" to "1", "b" to "2", "c" to "3", "d" to "4", "e" to "5"),
                    kunci_jawaban = listOf("a")
                ),
                PgQuestion(
                    soal = "Soal 2 (cuma 4 opsi)",
                    pilihan = mapOf("a" to "1", "b" to "2", "c" to "3", "d" to "4"),
                    kunci_jawaban = listOf("b")
                )
            ),
            esai = emptyList()
        )
        ExamScoring.validateAndNormalizeExam(inconsistentExam, requestedGlobalOptionCount = null)
    }

    @Test
    fun `multi answer PG toggle limit and partial credit grading behave accurately`() {
        val step1 = ExamScoring.togglePgSelection(null, "c", maxSelections = 2)
        assertFalse(step1.limitReached)
        assertEquals("c", step1.updatedValue)

        val step2 = ExamScoring.togglePgSelection(step1.updatedValue, "a", maxSelections = 2)
        assertFalse(step2.limitReached)
        assertEquals("a,c", step2.updatedValue)

        val step3Blocked = ExamScoring.togglePgSelection(step2.updatedValue, "d", maxSelections = 2)
        assertTrue(step3Blocked.limitReached)
        assertEquals("a,c", step3Blocked.updatedValue)

        val multiPg = PgQuestion(
            soal = "Pilih 2 bilangan prima",
            pilihan = mapOf("a" to "2", "b" to "4", "c" to "5", "d" to "9"),
            kunci_jawaban = listOf("a", "c"),
            maks_pilihan = 2
        )
        val singlePg = PgQuestion(
            soal = "Ibukota Indonesia?",
            pilihan = mapOf("a" to "Jakarta", "b" to "Bandung", "c" to "Surabaya", "d" to "Medan"),
            kunci = "a"
        )

        // Exact multi + exact single => 100%
        val allExact = ExamScoring.calculatePgExamSummary(
            questions = listOf(multiPg, singlePg),
            studentAnswers = mapOf(0 to "a,c", 1 to "a")
        )
        assertEquals(100, allExact.pgScorePercent)
        assertEquals(2, allExact.exactCorrectCount)

        // Multi gets 1/2 (no wrong choice) => 0.5 + 1.0 = 1.5 / 2 => 75%
        val partialSummary = ExamScoring.calculatePgExamSummary(
            questions = listOf(multiPg, singlePg),
            studentAnswers = mapOf(0 to "a", 1 to "a")
        )
        assertEquals(75, partialSummary.pgScorePercent)
        assertEquals(1, partialSummary.exactCorrectCount)
        assertEquals(1, partialSummary.partialCount)
        assertEquals(PgEvalStatus.PARTIAL, partialSummary.evaluations[0].status)

        // Multi picks 1 right + 1 wrong => (1 - 1)/2 = 0.0 -> WRONG
        val cancelledOut = ExamScoring.evaluatePgQuestion(0, multiPg, "a,b")
        assertEquals(0.0, cancelledOut.accuracy, 0.001)
        assertEquals(PgEvalStatus.WRONG, cancelledOut.status)
    }
}
