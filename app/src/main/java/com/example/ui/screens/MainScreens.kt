package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.api.EssayGradingItem
import com.example.api.EssayQuestion
import com.example.api.ExamQuestions
import com.example.api.GeminiApiClient
import com.example.api.PgQuestion
import com.example.database.ExamAttempt
import com.example.model.EducationCatalog
import com.example.model.SchoolLevel
import com.example.ui.components.AcademicBookEmblem
import com.example.ui.components.AcademicBookLoadingAnimation
import com.example.ui.components.AcademicProfileFormFields
import com.example.ui.theme.*
import com.example.viewmodel.ExamViewModel
import com.example.viewmodel.UiState
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(
    viewModel: ExamViewModel,
    onLoginSuccess: () -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var selectedSekolah by remember { mutableStateOf<SchoolLevel?>(null) }
    var selectedKelas by remember { mutableStateOf<String?>(null) }
    var selectedBidang by remember { mutableStateOf<String?>(null) }
    var showValidationErrors by remember { mutableStateOf(false) }

    val validation = remember(nama, selectedSekolah, selectedKelas, selectedBidang) {
        EducationCatalog.validateRegistration(nama, selectedSekolah, selectedKelas, selectedBidang)
    }

    val headerAlpha = remember { Animatable(0f) }
    val formOffsetY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        formOffsetY.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AcademicBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AcademicBookEmblem(
                size = 64.dp,
                modifier = Modifier.alpha(headerAlpha.value)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Pembelajaran dengan AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AcademicTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(headerAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Lengkapi profil belajarmu agar materi yang dijelaskan sesuai jenjangmu.",
                style = MaterialTheme.typography.bodyMedium,
                color = AcademicTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(headerAlpha.value)
            )

            Spacer(modifier = Modifier.height(26.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                border = BorderStroke(1.dp, AcademicBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = formOffsetY.value.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AcademicProfileFormFields(
                        nama = nama,
                        onNamaChange = { nama = it },
                        selectedSekolah = selectedSekolah,
                        onSekolahSelect = { newLevel ->
                            if (selectedSekolah != newLevel) {
                                selectedSekolah = newLevel
                                if (selectedKelas !in newLevel.classes) {
                                    selectedKelas = null
                                }
                                if (selectedBidang !in newLevel.subjects) {
                                    selectedBidang = "Umum"
                                }
                            }
                        },
                        selectedKelas = selectedKelas,
                        onKelasSelect = { selectedKelas = it },
                        selectedBidang = selectedBidang,
                        onBidangSelect = { selectedBidang = it },
                        validation = validation,
                        showValidationErrors = showValidationErrors
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (validation.isValid && selectedSekolah != null && selectedKelas != null && selectedBidang != null) {
                                viewModel.login(
                                    nama = nama,
                                    sekolah = selectedSekolah!!.displayName,
                                    kelas = selectedKelas!!,
                                    bidang = selectedBidang!!
                                )
                                onLoginSuccess()
                            } else {
                                showValidationErrors = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcademicBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button")
                    ) {
                        Text(
                            text = "Lanjutkan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        ThemeToggleButton(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .zIndex(10f)
        )
    }
}

@Composable
fun DashboardScreen(
    viewModel: ExamViewModel,
    onNavigateToStudyChat: () -> Unit,
    onNavigateToKisiKisi: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onViewResult: (Int) -> Unit
) {
    val session by viewModel.userSession.collectAsState()
    val attempts by viewModel.pastAttempts.collectAsState()
    val context = LocalContext.current
    var showProfileDialog by remember { mutableStateOf(false) }

    val currentBaseUrl by viewModel.baseUrl.collectAsState()
    val currentBaseUrlType by viewModel.baseUrlType.collectAsState()

    val profileBadge = remember(session) {
        EducationCatalog.formatProfileBadge(session?.sekolah, session?.kelas, session?.bidang)
    }

    if (showProfileDialog) {
        ProfileAccountDialog(
            session = session,
            attemptsCount = attempts.size,
            baseUrl = currentBaseUrl,
            baseUrlType = currentBaseUrlType,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { nama, sekolah, kelas, bidang ->
                viewModel.login(nama, sekolah, kelas, bidang)
                Toast.makeText(context, "Profil belajar diperbarui", Toast.LENGTH_SHORT).show()
                showProfileDialog = false
            },
            onNavigateToSettings = {
                showProfileDialog = false
                onNavigateToSettings()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AcademicBackground,
        topBar = {
            Surface(color = AcademicSurface) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pembelajaran dengan AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val greetingLine = buildString {
                                append("Halo, ${session?.nama ?: "Pelajar"}")
                                if (profileBadge.isNotBlank()) {
                                    append(" • $profileBadge")
                                }
                            }
                            Text(
                                text = greetingLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = AcademicTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ThemeToggleButton(viewModel = viewModel)

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AcademicBlue)
                                    .clickable { showProfileDialog = true }
                                    .testTag("settings_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                val firstChar = session?.nama?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                                Text(
                                    text = firstChar,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = AcademicBorder)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AcademicBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStudyChat() }
                        .testTag("open_study_chat_card")
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Tutor Belajar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tanya & Jelaskan Materi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Diskusikan pelajaran atau unggah dokumen (< 5 MB) untuk dijelaskan bertahap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Buka Tutor",
                            tint = Color.White
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                    border = BorderStroke(1.dp, AcademicBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToKisiKisi() }
                        .testTag("open_kisi_kisi_card")
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(AcademicBlueTint, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Buat Latihan Soal",
                                tint = AcademicBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Latihan Soal Mandiri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AcademicTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Masukkan kisi-kisi atau file materi (< 5 MB) untuk membuat soal PG dan esai.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AcademicTextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Mulai Latihan",
                            tint = AcademicTextMuted
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Riwayat Latihan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AcademicTextPrimary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            if (attempts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                        border = BorderStroke(1.dp, AcademicBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AcademicTextMuted.copy(alpha = 0.55f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum ada riwayat latihan",
                                color = AcademicTextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Hasil latihan soal pilihan ganda dan esai akan tersimpan di sini.",
                                color = AcademicTextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(attempts, key = { _, item -> item.id }) { _, attempt ->
                    HistoryCard(attempt = attempt, onClick = { onViewResult(attempt.id) })
                }
            }
        }
    }
}

@Composable
fun HistoryCard(attempt: ExamAttempt, onClick: () -> Unit) {
    val dateStr = remember(attempt.timestamp) {
        try {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(attempt.timestamp))
        } catch (_: Exception) {
            "-"
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (attempt.finalScore >= 70) AcademicSuccess.copy(alpha = 0.12f) else AcademicWarning.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${attempt.finalScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (attempt.finalScore >= 70) AcademicSuccess else AcademicWarning
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.studentClass.ifBlank { "Sesi Latihan" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AcademicTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateStr • PG ${attempt.pgCorrectCount}/${attempt.pgTotalCount} • Esai ${attempt.essayScore}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicTextMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Lihat Hasil",
                tint = AcademicTextMuted
            )
        }
    }
}

@Composable
fun KisiKisiScreen(
    viewModel: ExamViewModel,
    onGenerateStarted: () -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedId by remember { mutableIntStateOf(1) }
    val attachedDoc by viewModel.attachedDocument.collectAsState()
    val docExtractionState by viewModel.documentExtractionState.collectAsState()
    val selectedTimerMin by viewModel.selectedTimerMinutes.collectAsState()

    Scaffold(
        topBar = {
            Surface(color = AcademicSurface) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = AcademicTextPrimary
                            )
                        }
                        Text(
                            text = "Materi Latihan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AcademicTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ThemeToggleButton(viewModel = viewModel, modifier = Modifier.padding(end = 8.dp))
                    }
                    HorizontalDivider(color = AcademicBorder)
                }
            }
        },
        containerColor = AcademicBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tulis atau unggah bahan belajar. Soal latihan akan disesuaikan dengan jenjang dan bidang profilmu.",
                style = MaterialTheme.typography.bodyMedium,
                color = AcademicTextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Contoh: Hukum Newton, teks eksplanasi, atau topik ujian...") },
                minLines = 5,
                maxLines = 10,
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kisi_kisi_text_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            KisiKisiDocumentSection(
                viewModel = viewModel,
                onInsertTextToEditor = { extracted ->
                    text = if (text.isBlank()) extracted else "${text.trim()}\n\n$extracted"
                }
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Komposisi Soal",
                style = MaterialTheme.typography.titleSmall,
                color = AcademicTextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestionScaleCard(
                    title = "Ringkas",
                    desc = "5 PG • 2 Esai",
                    selected = selectedId == 1,
                    onClick = { selectedId = 1 },
                    modifier = Modifier.weight(1f)
                )
                QuestionScaleCard(
                    title = "Sedang",
                    desc = "15 PG • 3 Esai",
                    selected = selectedId == 2,
                    onClick = { selectedId = 2 },
                    modifier = Modifier.weight(1f)
                )
                QuestionScaleCard(
                    title = "Lengkap",
                    desc = "40 PG • 5 Esai",
                    selected = selectedId == 3,
                    onClick = { selectedId = 3 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Durasi Latihan",
                style = MaterialTheme.typography.titleSmall,
                color = AcademicTextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(60 to "Ringkas", 90 to "Standar", 120 to "Panjang").forEach { (minutes, note) ->
                    val isSelected = selectedTimerMin == minutes
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
                            .clickable { viewModel.setTimerMinutes(minutes) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$minutes Menit",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AcademicBlue else AcademicTextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(note, color = AcademicTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            val canGenerate = (text.isNotBlank() || attachedDoc != null) && docExtractionState !is UiState.Loading

            Button(
                onClick = {
                    if (canGenerate) {
                        when (selectedId) {
                            1 -> viewModel.setQuestionScale(5, 2)
                            2 -> viewModel.setQuestionScale(15, 3)
                            3 -> viewModel.setQuestionScale(40, 5)
                        }
                        viewModel.generateQuestionsFromKisiKisi(text)
                        onGenerateStarted()
                    }
                },
                enabled = canGenerate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcademicBlue,
                    contentColor = Color.White,
                    disabledContainerColor = AcademicBlue.copy(alpha = 0.28f),
                    disabledContentColor = Color.White.copy(alpha = 0.65f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_exam_button")
            ) {
                Text(
                    text = "Buat Soal Latihan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuestionScaleCard(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) AcademicBlue else AcademicBorder
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AcademicBlueTint else AcademicSurface
        ),
        modifier = modifier
            .testTag("scale_$title")
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (selected) AcademicBlue else AcademicTextPrimary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = AcademicTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun QuestionsGeneratingScreen(
    viewModel: ExamViewModel,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val genState by viewModel.generationState.collectAsState()
    val progressMsg by viewModel.generationProgressMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AcademicBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (genState) {
                is UiState.Loading -> {
                    AcademicBookLoadingAnimation(
                        title = "Menyusun Soal Latihan",
                        subtitle = progressMsg,
                        tips = listOf(
                            "Baca setiap pertanyaan perlahan sebelum memilih jawaban.",
                            "Untuk soal esai, tuliskan alasan atau langkah penyelesaian dengan jelas.",
                            "Soal disusun sesuai topik dan jenjang belajarmu."
                        )
                    )
                }
                is UiState.Success -> {
                    LaunchedEffect(Unit) {
                        onSuccess()
                    }
                }
                is UiState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Gagal",
                        tint = AcademicError,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Gagal Membuat Soal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AcademicTextPrimary
                    )
                    Text(
                        text = (genState as UiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AcademicTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Kembali", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }

        ThemeToggleButton(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(10f)
        )
    }
}

@Composable
fun ExamScreen(
    viewModel: ExamViewModel,
    onSubmitSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val exam by viewModel.activeExam.collectAsState()
    val pgAnswers by viewModel.studentPgAnswers.collectAsState()
    val essayAnswers by viewModel.studentEssayAnswers.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()

    var showConfirmSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startExamTimer {
            viewModel.submitExamAnswers()
            onSubmitSuccess()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopExamTimer()
        }
    }

    val minVal = secondsRemaining / 60
    val secVal = secondsRemaining % 60
    val timerText = String.format("%02d:%02d", minVal, secVal)
    val isTimeCrit = secondsRemaining < 300

    Scaffold(
        topBar = {
            Surface(color = AcademicSurface) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = AcademicTextPrimary
                            )
                        }
                        Text(
                            text = "Lembar Latihan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AcademicTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .background(
                                    if (isTimeCrit) AcademicError.copy(alpha = 0.12f) else AcademicBlueTint,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = timerText,
                                color = if (isTimeCrit) AcademicError else AcademicBlue,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        ThemeToggleButton(viewModel = viewModel, modifier = Modifier.padding(end = 4.dp))
                    }
                    HorizontalDivider(color = AcademicBorder)
                }
            }
        },
        containerColor = AcademicBackground
    ) { innerPadding ->
        if (exam == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AcademicBlue)
            }
        } else {
            val examVal = exam!!
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (examVal.pilihan_ganda.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pilihan Ganda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue
                            )
                        }
                        itemsIndexed(examVal.pilihan_ganda) { index, pg ->
                            PgCard(
                                questionIndex = index,
                                pg = pg,
                                selectedAnswer = pgAnswers[index],
                                onSelect = { viewModel.selectPgAnswer(index, it) }
                            )
                        }
                    }

                    if (examVal.esai.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Esai",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue
                            )
                        }
                        itemsIndexed(examVal.esai) { index, essay ->
                            EssayCard(
                                questionIndex = index,
                                question = essay,
                                answerText = essayAnswers[index] ?: "",
                                onValueChange = { viewModel.setEssayAnswer(index, it) }
                            )
                        }
                    }
                }

                Surface(
                    color = AcademicSurface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = AcademicBorder)
                        Button(
                            onClick = { showConfirmSubmit = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(50.dp)
                        ) {
                            Text("Selesai & Kumpulkan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showConfirmSubmit) {
            AlertDialog(
                onDismissRequest = { showConfirmSubmit = false },
                title = { Text("Kumpulkan Jawaban?", color = AcademicTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Seluruh jawaban pilihan ganda dan esai akan langsung diperiksa untuk melihat hasilnya.",
                        color = AcademicTextMuted
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmSubmit = false
                            viewModel.submitExamAnswers()
                            onSubmitSuccess()
                        }
                    ) {
                        Text("Kumpulkan", color = AcademicBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmSubmit = false }) {
                        Text("Periksa Lagi", color = AcademicTextMuted)
                    }
                },
                containerColor = AcademicSurface
            )
        }
    }
}

@Composable
fun PgCard(
    questionIndex: Int,
    pg: PgQuestion,
    selectedAnswer: String?,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Soal ${questionIndex + 1}",
                fontWeight = FontWeight.Bold,
                color = AcademicBlue,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = pg.soal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AcademicTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            val sortedOptions = remember(pg.pilihan) { pg.pilihan.entries.sortedBy { it.key } }
            for (entry in sortedOptions) {
                val letter = entry.key.lowercase()
                val value = entry.value
                val isSelected = selectedAnswer == letter

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pg_q${questionIndex}_opt_$letter")
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
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(if (isSelected) AcademicBlue else AcademicSurfaceSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.uppercase(),
                            color = if (isSelected) Color.White else AcademicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AcademicTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun EssayCard(
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
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = question.soal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AcademicTextPrimary
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
fun ResultScreen(
    viewModel: ExamViewModel,
    attemptId: Int,
    onNavigateBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val gradingState by viewModel.gradingState.collectAsState()
    val attempts by viewModel.pastAttempts.collectAsState()
    val activeExam by viewModel.activeExam.collectAsState()

    val attempt = attempts.find { it.id == attemptId }

    val attemptExam = remember(attempt, activeExam) {
        if (attempt != null && attempt.questionsJson.isNotBlank()) {
            try {
                GeminiApiClient.moshi.adapter(ExamQuestions::class.java).fromJson(attempt.questionsJson)
            } catch (_: Exception) {
                activeExam
            }
        } else {
            activeExam
        }
    }

    Scaffold(
        topBar = {
            Surface(color = AcademicSurface) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBackToDashboard) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = AcademicTextPrimary
                            )
                        }
                        Text(
                            text = "Hasil Latihan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AcademicTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ThemeToggleButton(viewModel = viewModel, modifier = Modifier.padding(end = 8.dp))
                    }
                    HorizontalDivider(color = AcademicBorder)
                }
            }
        },
        containerColor = AcademicBackground
    ) { innerPadding ->
        if (attempt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (gradingState is UiState.Loading) {
                    AcademicBookLoadingAnimation(
                        title = "Menilai Jawaban Esai",
                        subtitle = "Menyusun catatan umpan balik...",
                        tips = emptyList(),
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AcademicError, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Hasil latihan tidak ditemukan.", color = AcademicTextPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateBackToDashboard,
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue, contentColor = Color.White)
                        ) {
                            Text("Kembali ke Beranda")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                        border = BorderStroke(1.dp, AcademicBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nilai Akhir",
                                style = MaterialTheme.typography.labelLarge,
                                color = AcademicTextMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            DonutChart(
                                correct = attempt.pgCorrectCount,
                                total = attempt.pgTotalCount
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ScoreComponentCard(
                                    label = "Pilihan Ganda",
                                    score = attempt.pgScore,
                                    color = AcademicBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                ScoreComponentCard(
                                    label = "Esai",
                                    score = attempt.essayScore,
                                    color = AcademicSuccess,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.onShareResults(context, attempt) },
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bagikan")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.onRepeatExam()
                                onNavigateBackToDashboard()
                            },
                            border = BorderStroke(1.dp, AcademicBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AcademicTextPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Latihan Baru")
                        }
                    }
                }

                if (attempt.kisiKisi.isNotBlank()) {
                    item {
                        var isExpanded by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                            border = BorderStroke(1.dp, AcademicBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Materi Acuan",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AcademicTextPrimary
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Lihat Materi",
                                        tint = AcademicTextMuted
                                    )
                                }
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        HorizontalDivider(color = AcademicBorder)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = attempt.kisiKisi,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AcademicTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (attemptExam != null && attemptExam.esai.isNotEmpty()) {
                    item {
                        Text(
                            text = "Evaluasi Jawaban Esai",
                            fontWeight = FontWeight.Bold,
                            color = AcademicTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    val listType = Types.newParameterizedType(List::class.java, EssayGradingItem::class.java)
                    val gradingAdapter = GeminiApiClient.moshi.adapter<List<EssayGradingItem>>(listType)
                    val gradingList = try {
                        gradingAdapter.fromJson(attempt.gradingJson) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    itemsIndexed(attemptExam.esai) { index, essayQuestion ->
                        val grade = gradingList.getOrNull(index)
                        val scoreNum = grade?.skor?.toString() ?: "0"
                        val feedback = grade?.umpan_balik ?: "Catatan evaluasi belum tersedia."

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
                                        text = "Esai ${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AcademicBlue
                                    )
                                    Text(
                                        text = "Skor $scoreNum/100",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AcademicBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = essayQuestion.soal,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = AcademicTextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = feedback,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AcademicTextMuted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AcademicSurfaceSubtle, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreComponentCard(
    label: String,
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurfaceSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = AcademicTextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$score",
                color = color,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DonutChart(
    correct: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (correct.toFloat() / total.toFloat()) else 0f
    val sweepAngle = percentage * 360f

    val trackColor = AcademicBorder
    val fillColor = AcademicBlue

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(130.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = fillColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AcademicTextPrimary
            )
            Text(
                text = "$correct / $total Benar",
                style = MaterialTheme.typography.labelSmall,
                color = AcademicTextMuted
            )
        }
    }
}
