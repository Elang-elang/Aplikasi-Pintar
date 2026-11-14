package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.EducationCatalog
import com.example.model.RegistrationValidationResult
import com.example.model.SchoolLevel
import com.example.ui.screens.textFieldCosmosColors
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueTint
import com.example.ui.theme.AcademicBorder
import com.example.ui.theme.AcademicError
import com.example.ui.theme.AcademicSurface
import com.example.ui.theme.AcademicTextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicProfileFormFields(
    nama: String,
    onNamaChange: (String) -> Unit,
    selectedSekolah: SchoolLevel?,
    onSekolahSelect: (SchoolLevel) -> Unit,
    selectedKelas: String?,
    onKelasSelect: (String) -> Unit,
    selectedBidang: String?,
    onBidangSelect: (String) -> Unit,
    validation: RegistrationValidationResult,
    showValidationErrors: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step 1: Nama Lengkap
        Column {
            Text(
                text = "Nama Lengkap",
                style = MaterialTheme.typography.titleSmall,
                color = AcademicTextPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = nama,
                onValueChange = onNamaChange,
                placeholder = { Text("Tulis nama panggilan atau nama lengkap") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Nama Lengkap"
                    )
                },
                isError = showValidationErrors && validation.namaError != null,
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input")
            )
            AnimatedVisibility(visible = showValidationErrors && validation.namaError != null) {
                Text(
                    text = validation.namaError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicError,
                    modifier = Modifier
                        .padding(top = 4.dp, start = 4.dp)
                        .testTag("error_nama")
                )
            }
        }

        // Step 2: Pilihan Jenjang Sekolah menyamping (SD, SMP, SMA, SMK)
        Column {
            Text(
                text = "Jenjang Sekolah",
                style = MaterialTheme.typography.titleSmall,
                color = AcademicTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SchoolLevel.entries.forEach { level ->
                    val isSelected = selectedSekolah == level
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) AcademicBlue else AcademicSurface,
                        animationSpec = tween(200),
                        label = "school_bg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) AcademicBlue else AcademicBorder,
                        animationSpec = tween(200),
                        label = "school_border"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else AcademicTextPrimary,
                        animationSpec = tween(200),
                        label = "school_fg"
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSekolahSelect(level) }
                            .testTag("school_option_${level.shortLabel.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = showValidationErrors && validation.sekolahError != null) {
                Text(
                    text = validation.sekolahError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicError,
                    modifier = Modifier
                        .padding(top = 4.dp, start = 4.dp)
                        .testTag("error_sekolah")
                )
            }
        }

        // Step 3: Pilihan Kelas (angka saja, tergantung jenjang)
        AnimatedVisibility(
            visible = selectedSekolah != null,
            enter = fadeIn(tween(240)) + expandVertically(spring(dampingRatio = 0.8f)),
            exit = fadeOut(tween(180)) + shrinkVertically()
        ) {
            selectedSekolah?.let { level ->
                Column {
                    Text(
                        text = "Kelas",
                        style = MaterialTheme.typography.titleSmall,
                        color = AcademicTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AnimatedContent(
                        targetState = level,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(150))
                        },
                        label = "class_options_transition"
                    ) { activeLevel ->
                        val classes = EducationCatalog.classesFor(activeLevel)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            classes.forEach { classOption ->
                                val isSelected = selectedKelas == classOption
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) AcademicBlue else AcademicSurface,
                                    label = "class_bg"
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else AcademicTextPrimary,
                                    label = "class_fg"
                                )
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgColor)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) AcademicBlue else AcademicBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onKelasSelect(classOption) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("class_option_$classOption"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = classOption,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = showValidationErrors && validation.kelasError != null) {
                        Text(
                            text = validation.kelasError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicError,
                            modifier = Modifier
                                .padding(top = 4.dp, start = 4.dp)
                                .testTag("error_kelas")
                        )
                    }
                }
            }
        }

        // Step 4: Pilihan Bidang / Kejurusan (selalu mencakup "Umum" di urutan pertama)
        AnimatedVisibility(
            visible = selectedSekolah != null,
            enter = fadeIn(tween(260)) + expandVertically(spring(dampingRatio = 0.8f)),
            exit = fadeOut(tween(180)) + shrinkVertically()
        ) {
            selectedSekolah?.let { level ->
                Column {
                    Text(
                        text = if (level == SchoolLevel.SMK) "Kejuruan / Bidang" else "Bidang / Mata Pelajaran",
                        style = MaterialTheme.typography.titleSmall,
                        color = AcademicTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AnimatedContent(
                        targetState = level,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(150))
                        },
                        label = "subject_options_transition"
                    ) { activeLevel ->
                        val subjects = EducationCatalog.subjectsFor(activeLevel)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            subjects.forEach { subject ->
                                val isSelected = selectedBidang == subject
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) AcademicBlueTint else AcademicSurface,
                                    label = "subject_bg"
                                )
                                val borderColor by animateColorAsState(
                                    targetValue = if (isSelected) AcademicBlue else AcademicBorder,
                                    label = "subject_border"
                                )
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onBidangSelect(subject) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("subject_option_$subject"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (subject == "Umum") "Umum (Semua Mapel)" else subject,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) AcademicBlue else AcademicTextPrimary
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = showValidationErrors && validation.bidangError != null) {
                        Text(
                            text = validation.bidangError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicError,
                            modifier = Modifier
                                .padding(top = 4.dp, start = 4.dp)
                                .testTag("error_bidang")
                        )
                    }
                }
            }
        }

        if (showValidationErrors && selectedSekolah == null && validation.sekolahError != null) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
