package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.EssayQuestion
import com.example.api.GlobalPgOptionCount
import com.example.api.PgQuestion
import com.example.model.ExamScoring
import com.example.model.PgEvalStatus
import com.example.ui.screens.textFieldCosmosColors
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun GlobalPgOptionSelector(
    selectedOptionCount: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePreset = GlobalPgOptionCount.fromCount(selectedOptionCount)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlobalPgOptionCount.entries.forEach { preset ->
            val isSelected = activePreset == preset
            val tag = if (preset.count == null) "pg_opt_count_auto" else "pg_opt_count_${preset.count}"
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) AcademicBlue else AcademicBorder
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) AcademicBlueTint else AcademicSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag(tag)
                    .clickable { onSelect(preset.count) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = preset.shortTitle,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) AcademicBlue else AcademicTextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = preset.subtitle,
                        color = AcademicTextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownPgCard(
    questionIndex: Int,
    pg: PgQuestion,
    selectedAnswer: String?,
    onSelect: (String) -> Unit
) {
    val selectedKeys = remember(selectedAnswer) {
        ExamScoring.parseSelectedKeys(selectedAnswer)
    }
    val maxSelections = remember(pg) { pg.resolvedMaxSelections() }
    val isMulti = remember(pg) { pg.isMultiAnswer }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soal ${questionIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    color = AcademicBlue,
                    fontSize = 12.sp
                )

                if (isMulti) {
                    Box(
                        modifier = Modifier
                            .background(AcademicBlueTint, RoundedCornerShape(8.dp))
                            .border(0.75.dp, AcademicBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("pg_q${questionIndex}_multi_badge")
                    ) {
                        Text(
                            text = "Pilih maks. $maxSelections jawaban (${selectedKeys.size}/$maxSelections)",
                            color = AcademicBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            RichMarkdownContent(
                markdown = pg.soal,
                textColor = AcademicTextPrimary,
                baseTextStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val sortedOptions = remember(pg.pilihan) { pg.pilihan.entries.sortedBy { it.key.lowercase() } }
            for (entry in sortedOptions) {
                val letter = entry.key.trim().lowercase()
                val value = entry.value
                val isSelected = letter in selectedKeys

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AcademicBlueTint else Color.Transparent)
                        .border(
                            BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) AcademicBlue else AcademicBorder
                            ),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(letter) }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                        .testTag("pg_q${questionIndex}_opt_$letter"),
                    verticalAlignment = Alignment.Top
                ) {
                    val badgeShape = if (isMulti) RoundedCornerShape(7.dp) else CircleShape
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(26.dp)
                            .background(if (isSelected) AcademicBlue else AcademicSurfaceSubtle, badgeShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMulti && isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Opsi ${letter.uppercase()} dipilih",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = letter.uppercase(),
                                color = if (isSelected) Color.White else AcademicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    RichMarkdownContent(
                        markdown = value,
                        textColor = AcademicTextPrimary,
                        baseTextStyle = MaterialTheme.typography.bodyMedium,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownEssayCard(
    questionIndex: Int,
    question: EssayQuestion,
    answerText: String,
    onValueChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Esai ${questionIndex + 1}",
                fontWeight = FontWeight.Bold,
                color = AcademicBlue,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            RichMarkdownContent(
                markdown = question.soal,
                textColor = AcademicTextPrimary,
                baseTextStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = answerText,
                onValueChange = onValueChange,
                placeholder = { Text("Tulis jawabanmu di sini...") },
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(10.dp),
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PgEvaluationReviewCard(
    questionIndex: Int,
    pg: PgQuestion,
    rawStudentAnswer: String?
) {
    val evaluation = remember(questionIndex, pg, rawStudentAnswer) {
        ExamScoring.evaluatePgQuestion(questionIndex, pg, rawStudentAnswer)
    }

    val statusLabel: String
    val statusColor: Color
    when (evaluation.status) {
        PgEvalStatus.EXACT -> {
            statusLabel = "Tepat"
            statusColor = AcademicSuccess
        }
        PgEvalStatus.PARTIAL -> {
            statusLabel = "Parsial ${(evaluation.accuracy * 100).roundToInt()}%"
            statusColor = AcademicWarning
        }
        PgEvalStatus.WRONG -> {
            statusLabel = "Kurang Tepat"
            statusColor = AcademicError
        }
    }

    val chosenLabel = if (evaluation.selectedKeys.isEmpty()) {
        "Belum dijawab"
    } else {
        evaluation.selectedKeys.joinToString(", ") { it.uppercase() }
    }
    val correctLabel = evaluation.correctKeys.joinToString(", ") { it.uppercase() }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (pg.isMultiAnswer) "PG ${questionIndex + 1} (Multi-Jawaban)" else "PG ${questionIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AcademicBlue
                )
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            RichMarkdownContent(
                markdown = pg.soal,
                textColor = AcademicTextPrimary,
                baseTextStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AcademicSurfaceSubtle, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Jawabanmu: $chosenLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Kunci Benar: $correctLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
