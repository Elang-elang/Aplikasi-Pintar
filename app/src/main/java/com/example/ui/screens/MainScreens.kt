package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.*
import kotlinx.coroutines.flow.StateFlow
import com.example.database.ExamAttempt
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
    var kelas by remember { mutableStateOf("") }
    var absen by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Theme Toggle at Top Right
        ThemeToggleButton(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Logo / Icon Frame
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CosmosTeal.copy(alpha = 0.18f), CircleShape)
                    .border(2.dp, CosmosTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Logo Profil Belajar",
                    tint = CosmosTeal,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Ujian Pintar AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Chatbot AI Belajar, Penjelasan Materi & Latihan Soal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Text(
                text = "Masuk Profil Belajar (Tanpa Kode)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CosmosTeal,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = nama,
                onValueChange = {
                    nama = it
                    showError = false
                },
                label = { Text("Nama Lengkap *") },
                placeholder = { Text("Masukkan nama Anda...") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nama") },
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = kelas,
                onValueChange = { kelas = it },
                label = { Text("Kelas / Bidang Belajar (Opsional)") },
                placeholder = { Text("Contoh: XII IPA / Mahasiswa / Umum") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Kelas atau Bidang") },
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("class_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = absen,
                onValueChange = { absen = it },
                label = { Text("Nomor Absen / Catatan (Opsional)") },
                placeholder = { Text("Opsional") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Absen") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("absen_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (showError) {
                Text(
                    text = "Mohon isi Nama Lengkap terlebih dahulu!",
                    color = CosmosError,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    if (nama.isNotBlank()) {
                        viewModel.login(nama, kelas, absen)
                        onLoginSuccess()
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmosTeal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_button")
            ) {
                Text(
                    text = "Masuk ke Aplikasi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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

    if (showProfileDialog) {
        ProfileAccountDialog(
            session = session,
            attemptsCount = attempts.size,
            baseUrl = currentBaseUrl,
            baseUrlType = currentBaseUrlType,
            onDismiss = { showProfileDialog = false },
            onRelogin = { nama, kelas, absen ->
                viewModel.login(nama, kelas, absen)
                Toast.makeText(context, "Profil belajar diperbarui!", Toast.LENGTH_SHORT).show()
                showProfileDialog = false
            },
            onNavigateToSettings = {
                showProfileDialog = false
                onNavigateToSettings()
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmosBackground),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Belajar AI Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = CosmosTeal
                    )
                    Text(
                        text = "Halo, ${session?.nama ?: "Pelajar"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmosTextMuted
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeToggleButton(viewModel = viewModel)

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(CosmosTeal, CosmosSecondary)))
                            .clickable { showProfileDialog = true }
                            .testTag("settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val firstChar = session?.nama?.trim()?.firstOrNull()?.let { it.uppercaseChar().toString() } ?: "P"
                        Text(
                            text = firstChar,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = CosmosBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Primary Card: AI Study Chatbot & Material/Document Explanation
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmosTeal.copy(alpha = 0.14f)),
                border = BorderStroke(1.5.dp, CosmosTeal.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp)
                    .clickable { onNavigateToStudyChat() }
                    .testTag("open_study_chat_card")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(CosmosTeal, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Tutor Belajar AI",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tutor & Penjelasan Materi AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmosTextLight
                        )
                        Text(
                            text = "Tanya jawab chatbot AI serta jelaskan uraian teks atau file dokumen (< 5 MB) untuk belajar",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTextMuted
                        )
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Buka Tutor AI", tint = CosmosTeal)
                }
            }

            // Secondary Card: Practice Quiz / Exam Simulation
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                border = BorderStroke(1.dp, CosmosCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onNavigateToKisiKisi() }
                    .testTag("open_kisi_kisi_card")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(CosmosSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Mulai Latihan Soal", tint = CosmosSecondary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Latihan Soal & Simulasi Ujian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmosTextLight
                        )
                        Text(
                            text = "Tempel teks atau unggah dokumen kisi-kisi (< 5 MB) untuk membuat soal latihan PG & Esai",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTextMuted
                        )
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Mulai", tint = CosmosTextMuted)
                }
            }

            Text(
                text = "Riwayat Latihan & Ujian",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmosTextLight,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )

            if (attempts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No items",
                            tint = CosmosTextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada riwayat ujian",
                            color = CosmosTextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(attempts) { _, attempt ->
                        HistoryCard(attempt = attempt, onClick = { onViewResult(attempt.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(attempt: ExamAttempt, onClick: () -> Unit) {
    val dateStr = try {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(attempt.timestamp))
    } catch (e: Exception) {
        "-"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmosSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nilai Akhir: ${attempt.finalScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (attempt.finalScore >= 70) CosmosSuccess else CosmosWarning
                )
                Text(
                    text = "Mata Ujian: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmosTextMuted
                )
                Text(
                    text = "PG Benar: ${attempt.pgCorrectCount}/${attempt.pgTotalCount} (Skor: ${attempt.pgScore}) | Esai: ${attempt.essayScore}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmosTextMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CosmosTeal.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Selesai",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmosTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun KisiKisiScreen(
    viewModel: ExamViewModel,
    onGenerateStarted: () -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateFlowOf("") }
    var selectedId by remember { mutableIntStateOf(1) } // 1: Cepat, 2: Sedang, 3: Lengkap
    val attachedDoc by viewModel.attachedDocument.collectAsState()
    val docExtractionState by viewModel.documentExtractionState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CosmosTextLight)
                }
                Text(
                    text = "Input Kisi-Kisi Bahan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CosmosTextLight,
                    modifier = Modifier.weight(1f)
                )
                ThemeToggleButton(viewModel = viewModel, modifier = Modifier.padding(end = 8.dp))
            }
        },
        containerColor = CosmosBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tulis atau tempelkan (copy-paste) kisi-kisi atau ringkasan materi bahan ajar sekolah di bawah ini. AI akan merumuskan simulasi ujian unik untuk Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmosTextMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Contoh: Pancasila sebagai dasar negara, masa pra-aksara di Indonesia, atau catatan tambahan untuk dokumen...") },
                minLines = 5,
                maxLines = 10,
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kisi_kisi_text_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            KisiKisiDocumentSection(
                viewModel = viewModel,
                onInsertTextToEditor = { extracted ->
                    text = if (text.isBlank()) extracted else "${text.trim()}\n\n$extracted"
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Pilih Komposisi Soal AI:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CosmosTextLight,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Composite Plan Row Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestionScaleCard(
                    title = "Cepat",
                    desc = "5 PG | 2 Esai",
                    selected = selectedId == 1,
                    onClick = { selectedId = 1 },
                    modifier = Modifier.weight(1f)
                )
                QuestionScaleCard(
                    title = "Sedang",
                    desc = "15 PG | 3 Esai",
                    selected = selectedId == 2,
                    onClick = { selectedId = 2 },
                    modifier = Modifier.weight(1f)
                )
                QuestionScaleCard(
                    title = "Lengkap",
                    desc = "40 PG | 5 Esai",
                    selected = selectedId == 3,
                    onClick = { selectedId = 3 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Pilih Durasi Sesi Ujian:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CosmosTextLight,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val selectedTimerMin by viewModel.selectedTimerMinutes.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    border = BorderStroke(1.5.dp, if (selectedTimerMin == 60) CosmosSecondary else CosmosCardBorder),
                    colors = CardDefaults.cardColors(containerColor = if (selectedTimerMin == 60) CosmosSecondary.copy(alpha = 0.15f) else CosmosSurface),
                    modifier = Modifier.weight(1f).clickable { viewModel.setTimerMinutes(60) }
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("60 Menit", fontWeight = FontWeight.Bold, color = CosmosTextLight, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Pengerjaan Cepat", color = CosmosTextMuted, fontSize = 11.sp)
                    }
                }
                Card(
                    border = BorderStroke(1.5.dp, if (selectedTimerMin == 90) CosmosSecondary else CosmosCardBorder),
                    colors = CardDefaults.cardColors(containerColor = if (selectedTimerMin == 90) CosmosSecondary.copy(alpha = 0.15f) else CosmosSurface),
                    modifier = Modifier.weight(1f).clickable { viewModel.setTimerMinutes(90) }
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("90 Menit", fontWeight = FontWeight.Bold, color = CosmosTextLight, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Rekomendasi", color = CosmosTextMuted, fontSize = 11.sp)
                    }
                }
                Card(
                    border = BorderStroke(1.5.dp, if (selectedTimerMin == 120) CosmosSecondary else CosmosCardBorder),
                    colors = CardDefaults.cardColors(containerColor = if (selectedTimerMin == 120) CosmosSecondary.copy(alpha = 0.15f) else CosmosSurface),
                    modifier = Modifier.weight(1f).clickable { viewModel.setTimerMinutes(120) }
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("120 Menit", fontWeight = FontWeight.Bold, color = CosmosTextLight, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Pengerjaan Detail", color = CosmosTextMuted, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

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
                    containerColor = CosmosTeal,
                    contentColor = CosmosTextLight,
                    disabledContainerColor = CosmosTeal.copy(alpha = 0.3f),
                    disabledContentColor = CosmosTextLight.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Buat Ujian Otomatis",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
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
        border = BorderStroke(
            1.5.dp, 
            if (selected) CosmosTeal else CosmosCardBorder
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) CosmosTeal.copy(alpha = 0.15f) else CosmosSurface
        ),
        modifier = modifier
            .testTag("scale_$title")
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = CosmosTextLight, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, color = CosmosTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
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
            .background(CosmosBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Theme Toggle at Top Right
        ThemeToggleButton(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (genState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        color = CosmosTeal,
                        strokeWidth = 5.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Sedang Merumuskan Soal...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmosTextLight,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = progressMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmosTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Helpful learning quote card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "\"Kesulitan adalah modal dasar pembetulan akal.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = CosmosTeal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "- Motivasi Belajar Pintar",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmosTextMuted,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    LaunchedEffect(Unit) {
                        onSuccess()
                    }
                }
                is UiState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = CosmosError,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pembuatan Soal Gagal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmosTextLight
                    )
                    Text(
                        text = (genState as UiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmosTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = CosmosSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Kembali dan Coba Lagi", color = CosmosTextLight)
                    }
                }
                else -> {}
            }
        }
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
    val gradingState by viewModel.gradingState.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()

    var showConfirmSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startExamTimer {
            // Auto submit when time runs out
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
    val isTimeCrit = secondsRemaining < 300 // less than 5 mins

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showConfirmSubmit = true }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CosmosTextLight)
                }
                Text(
                    text = "Lembar Sesi UjianAI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CosmosTextLight,
                    modifier = Modifier.weight(1f)
                )
                // Pulsating Countdown Timer Bubble
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            if (isTimeCrit) CosmosError.copy(alpha = 0.2f) else CosmosTeal.copy(alpha = 0.15f),
                            RoundedCornerShape(30.dp)
                        )
                        .border(
                            1.dp,
                            if (isTimeCrit) CosmosError else CosmosTeal,
                            RoundedCornerShape(30.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, // Clock visual representation
                            contentDescription = "Timer Icon",
                            tint = if (isTimeCrit) CosmosError else CosmosTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = timerText,
                            color = if (isTimeCrit) CosmosError else CosmosTeal,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                ThemeToggleButton(viewModel = viewModel, modifier = Modifier.padding(end = 8.dp))
            }
        },
        containerColor = CosmosBackground
    ) { innerPadding ->
        if (exam == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CosmosPrimary)
            }
        } else {
            val examVal = exam!!
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "BAGIAN I: PILIHAN GANDA",
                            fontWeight = FontWeight.Bold,
                            color = CosmosTeal,
                            fontSize = 14.sp
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

                    if (examVal.esai.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "BAGIAN II: ESAI (Koreksi AI Otomatis)",
                                fontWeight = FontWeight.Bold,
                                color = CosmosSecondary,
                                fontSize = 14.sp
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

                // SUBMIT OVERLAY / BUTTON BAR
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(CosmosBackground.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showConfirmSubmit = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmosPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Kumpulkan Jawaban", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CONFIRM SUBMIT DIALOG
        if (showConfirmSubmit) {
            AlertDialog(
                onDismissRequest = { showConfirmSubmit = false },
                title = { Text("Konfirmasi Selesai Ujian", color = CosmosTextLight) },
                text = {
                    Text(
                        "Apakah Anda yakin ingin mengumpulkan seluruh lembar jawaban sekarang? Jawaban PG dan Esai Anda akan langsung dianalisis dan dievaluasi otomatis oleh AI.",
                        color = CosmosTextLight
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
                        Text("Ya, Kumpulkan!", color = CosmosTeal, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmSubmit = false }) {
                        Text("Batal", color = CosmosTextMuted)
                    }
                },
                containerColor = CosmosSurface
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmosSurface),
        border = BorderStroke(1.dp, CosmosCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Soal ${questionIndex + 1}:",
                fontWeight = FontWeight.Bold,
                color = CosmosTeal,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pg.soal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = CosmosTextLight
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options A, B, C, D
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CosmosPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .border(BorderStroke(1.dp, if (isSelected) CosmosPrimary else CosmosCardBorder.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                        .clickable { onSelect(letter) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (isSelected) CosmosPrimary else CosmosCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.uppercase(),
                            color = CosmosTextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmosTextLight
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmosSurface),
        border = BorderStroke(1.dp, CosmosCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Esai Soal ${questionIndex + 1}:",
                fontWeight = FontWeight.Bold,
                color = CosmosSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = question.soal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = CosmosTextLight
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = answerText,
                onValueChange = onValueChange,
                placeholder = { Text("Tulis deskripsi penjelasan lengkap jawaban Anda di sini...") },
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

    // Find target attempt
    val attempt = attempts.find { it.id == attemptId }

    val attemptExam = remember(attempt, activeExam) {
        if (attempt != null && !attempt.questionsJson.isNullOrBlank()) {
            try {
                GeminiApiClient.moshi.adapter(ExamQuestions::class.java).fromJson(attempt.questionsJson)
            } catch (e: Exception) {
                activeExam
            }
        } else {
            activeExam
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBackToDashboard) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CosmosTextLight)
                }
                Text(
                    text = "Rapor Hasil Simulasi AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CosmosTextLight,
                    modifier = Modifier.weight(1f)
                )
                ThemeToggleButton(viewModel = viewModel, modifier = Modifier.padding(end = 8.dp))
            }
        },
        containerColor = CosmosBackground
    ) { innerPadding ->
        if (attempt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (gradingState is UiState.Loading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CosmosTeal)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sedang Menilai Jawaban Esai...", color = CosmosTextLight)
                        Text(
                            text = "Meminta evaluasi analitis & memberi koreksi skor dari guru AI...",
                            color = CosmosTextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = CosmosError, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Hasil ujian tidak ditemukan atau error dianalisis.", color = CosmosTextLight)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBackToDashboard) {
                            Text("Kembali")
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Main Score Metrics Header Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NILAI AKHIR SIMULASI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = CosmosTextMuted
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Donut Chart Correct PG representer
                            DonutChart(
                                correct = attempt.pgCorrectCount,
                                total = attempt.pgTotalCount
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Comparative mini cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ScoreComponentCard(
                                    label = "Pilihan Ganda",
                                    score = attempt.pgScore,
                                    color = CosmosTeal,
                                    modifier = Modifier.weight(1f)
                                )
                                ScoreComponentCard(
                                    label = "Evaluasi Esai",
                                    score = attempt.essayScore,
                                    color = CosmosSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Share / Action button row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.onShareResults(context, attempt) },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmosPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bagikan Hasil")
                        }

                        Button(
                            onClick = {
                                viewModel.onRepeatExam()
                                onNavigateBackToDashboard()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmosSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CosmosCardBorder),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ujian Baru", color = CosmosTextLight)
                        }
                    }
                }

                // Collapsible Kisi-kisi Card
                if (!attempt.kisiKisi.isNullOrBlank()) {
                    item {
                        var isExpanded by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                            border = BorderStroke(1.dp, CosmosCardBorder),
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = "KisiKisi", tint = CosmosTeal, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Materi Kisi-Kisi Pelajaran",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CosmosTextLight
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = CosmosTextMuted
                                    )
                                }
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        HorizontalDivider(color = CosmosCardBorder)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = attempt.kisiKisi,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CosmosTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Feedback and Details list if offline-loaded/active attempt exam available
                if (attemptExam != null) {
                    val exam = attemptExam!!
                    item {
                        Text(
                            text = "Koreksi Lembar Jawaban Esai Anda:",
                            fontWeight = FontWeight.Bold,
                            color = CosmosSecondary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    // Parse grading details
                    val listType = Types.newParameterizedType(List::class.java, EssayGradingItem::class.java)
                    val gradingAdapter = GeminiApiClient.moshi.adapter<List<EssayGradingItem>>(listType)
                    val gradingList = try {
                        gradingAdapter.fromJson(attempt.gradingJson) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    itemsIndexed(exam.esai) { index, essayQuestion ->
                        val grade = gradingList.getOrNull(index)
                        val scoreNum = grade?.skor?.toString() ?: "0"
                        val feedback = grade?.umpan_balik ?: "Gagal mendapatkan analisis koreksi AI."

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                            border = BorderStroke(1.dp, CosmosCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Soal Esai ${index + 1}: ${essayQuestion.soal}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmosTextLight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    Text(
                                        text = "Skor: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmosTextMuted
                                    )
                                    Text(
                                        text = "$scoreNum / 100",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmosSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Koreksi AI:\n$feedback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmosTextLight,
                                    modifier = Modifier
                                        .background(CosmosBackground, RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                        .fillMaxWidth()
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
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CosmosBackground),
        border = BorderStroke(1.dp, CosmosCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = CosmosTextMuted, fontSize = 11.sp)
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

    val colorBorder = CosmosCardBorder
    val colorTeal = CosmosTeal

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(130.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            // Outer backing stroke (Grey border)
            drawArc(
                color = colorBorder,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Active Correct arc (Teal highlight)
            drawArc(
                color = colorTeal,
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
                color = CosmosTextLight
            )
            Text(
                text = "$correct / $total Jawaban",
                style = MaterialTheme.typography.labelSmall,
                color = CosmosTextMuted
            )
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onStartTest: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    androidx.activity.compose.BackHandler(enabled = true) {
        onBack()
    }

    val baseUrlVal by viewModel.baseUrl.collectAsState()
    val baseUrlTypeVal by viewModel.baseUrlType.collectAsState()
    val customKeyVal by viewModel.customApiKey.collectAsState()
    val modelNameVal by viewModel.modelName.collectAsState()
    val failureToggleMessage by viewModel.apiConfigFailureToggleMessage.collectAsState()
    val failureDetail by viewModel.apiConfigFailureDetail.collectAsState()

    var baseUrl by remember(baseUrlVal) { mutableStateOf(baseUrlVal) }
    var baseUrlType by remember(baseUrlTypeVal) { mutableStateOf(baseUrlTypeVal) }
    var apiKey by remember(customKeyVal) { mutableStateOf(customKeyVal) }
    var modelName by remember(modelNameVal) { mutableStateOf(modelNameVal) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(failureToggleMessage) {
        if (!failureToggleMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = failureToggleMessage!!,
                duration = SnackbarDuration.Long
            )
        }
    }

    val resolvedEndpointPreview = remember(baseUrl, baseUrlType) {
        try {
            if (baseUrl.isBlank()) null else GeminiApiClient.resolveEndpointUrl(baseUrl, baseUrlType)
        } catch (_: Exception) {
            null
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CosmosError,
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CosmosTextLight)
                }
                Text(
                    text = "Pengaturan Base URL & API",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CosmosTextLight,
                    modifier = Modifier.weight(1f)
                )
                ThemeToggleButton(viewModel = viewModel, modifier = Modifier.padding(end = 8.dp))
            }
        },
        containerColor = CosmosBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ApiFailureToggleBanner(
                message = failureToggleMessage,
                detail = failureDetail,
                onDismiss = { viewModel.dismissApiFailureMessage() },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(CosmosTeal.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, CosmosTeal.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "ApiConfig", tint = CosmosTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Konfigurasi API Belajar AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmosTextLight
                        )
                        Text(
                            text = "Atur Base URL, Tipe Base URL (antropic / chat_id / response), API Key, dan Model AI Anda secara manual. Semua proses berjalan langsung ke endpoint pilihan Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTextMuted
                        )
                    }
                }
            }

            Text(
                text = "Kredensial, Base URL & Pilihan Model AI",
                style = MaterialTheme.typography.titleSmall,
                color = CosmosTeal,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Text(
                text = "Setiap perubahan Base URL atau API Key akan diuji terlebih dahulu (maks. 5 percobaan, timeout 5 menit).",
                style = MaterialTheme.typography.bodySmall,
                color = CosmosTextMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            ApiBaseUrlTypeSelector(
                selectedType = baseUrlType,
                onSelectType = {
                    baseUrlType = it
                    validationError = ""
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    validationError = ""
                },
                enabled = true,
                label = { Text("Base URL") },
                placeholder = { Text(baseUrlType.placeholderUrl) },
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_base_url_input"),
                singleLine = true
            )

            if (resolvedEndpointPreview != null) {
                Text(
                    text = "Endpoint tujuan: $resolvedEndpointPreview",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmosTeal,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 6.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    validationError = ""
                },
                enabled = true,
                label = { Text("API Key") },
                placeholder = { Text("Masukkan API Key...") },
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.Done else Icons.Default.Lock,
                            contentDescription = if (isApiKeyVisible) "Sembunyikan" else "Tampilkan",
                            tint = CosmosTeal
                        )
                    }
                },
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_api_key_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                enabled = true,
                label = { Text("Model AI (Default: ${baseUrlType.defaultModel})") },
                placeholder = { Text(baseUrlType.defaultModel) },
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_model_name_input"),
                singleLine = true
            )

            if (validationError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError,
                    color = CosmosError,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val trimmedUrl = baseUrl.trim()
                        val trimmedKey = apiKey.trim()
                        when {
                            trimmedUrl.isBlank() -> {
                                validationError = "Base URL wajib diisi!"
                            }
                            !trimmedUrl.startsWith("http://", ignoreCase = true) &&
                                !trimmedUrl.startsWith("https://", ignoreCase = true) -> {
                                validationError = "Base URL harus diawali http:// atau https://"
                            }
                            trimmedKey.isBlank() -> {
                                validationError = "API Key wajib diisi!"
                            }
                            else -> {
                                val effectiveModel = modelName.trim().ifBlank { baseUrlType.defaultModel }
                                viewModel.startConnectionTest(
                                    baseUrl = trimmedUrl,
                                    baseUrlType = baseUrlType,
                                    apiKey = trimmedKey,
                                    model = effectiveModel
                                )
                                onStartTest()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmosTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(52.dp)
                        .testTag("settings_save_and_test_button")
                ) {
                    Text("Simpan & Uji Koneksi", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        viewModel.logout()
                        Toast.makeText(context, "Keluar dari sesi belajar!", Toast.LENGTH_SHORT).show()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmosError),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("settings_logout_button")
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Reusable custom Colors for TextFields in Cosmos design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun textFieldCosmosColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CosmosTeal,
    unfocusedBorderColor = CosmosCardBorder,
    focusedLabelColor = CosmosTeal,
    unfocusedLabelColor = CosmosTextMuted,
    focusedTextColor = CosmosTextLight,
    unfocusedTextColor = CosmosTextLight,
    focusedPlaceholderColor = CosmosTextMuted,
    unfocusedPlaceholderColor = CosmosTextMuted
)

// Helper of state flow remember
@Composable
fun <T> rememberStateFlow(flow: StateFlow<T>): State<T> {
    return flow.collectAsState()
}

// Utility constructor
fun <T> mutableStateFlowOf(value: T): MutableState<T> = mutableStateOf(value)

@Composable
fun ThemeToggleButton(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    IconButton(
        onClick = { viewModel.toggleTheme() },
        modifier = modifier
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
            if (isDarkTheme) {
                // Moon shape: Draw yellow crescent
                val path = androidx.compose.ui.graphics.Path().apply {
                    val width = size.width
                    val height = size.height
                    moveTo(width * 0.35f, height * 0.15f)
                    cubicTo(
                        width * 0.85f, height * 0.2f,
                        width * 0.85f, height * 0.8f,
                        width * 0.35f, height * 0.85f
                    )
                    cubicTo(
                        width * 0.65f, height * 0.7f,
                        width * 0.65f, height * 0.3f,
                        width * 0.35f, height * 0.15f
                    )
                }
                drawPath(path, color = Color(0xFFF1C40F))
            } else {
                // Sun shape: Draw amber/yellow circle with rays
                val center = center
                val radius = size.minDimension / 3.5f
                drawCircle(
                    color = Color(0xFFE67E22),
                    radius = radius,
                    center = center
                )
                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4
                    val startX = (center.x + (radius + 2.dp.toPx()) * Math.cos(angle)).toFloat()
                    val startY = (center.y + (radius + 2.dp.toPx()) * Math.sin(angle)).toFloat()
                    val endX = (center.x + (radius + 5.dp.toPx()) * Math.cos(angle)).toFloat()
                    val endY = (center.y + (radius + 5.dp.toPx()) * Math.sin(angle)).toFloat()
                    drawLine(
                        color = Color(0xFFE67E22),
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileAccountDialog(
    session: com.example.database.UserSession?,
    attemptsCount: Int,
    baseUrl: String,
    baseUrlType: ApiBaseUrlType,
    onDismiss: () -> Unit,
    onRelogin: (String, String, String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    var editNama by remember { mutableStateOf(session?.nama ?: "") }
    var editKelas by remember { mutableStateOf(session?.kelas ?: "") }
    var editAbsen by remember { mutableStateOf(session?.absen ?: "") }
    var editError by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = CosmosSurface,
            border = BorderStroke(1.dp, CosmosCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isEditing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(CosmosPrimary, CosmosSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PROFIL BELAJAR",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = (-40).dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(CosmosBackground)
                            .border(4.dp, CosmosSurface, CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(CosmosTeal, CosmosSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val firstChar = session?.nama?.trim()?.firstOrNull()?.let { it.uppercaseChar().toString() } ?: "P"
                        Text(
                            text = firstChar,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(-30.dp))

                    Text(
                        text = session?.nama ?: "Pelajar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = CosmosTextLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CosmosTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, CosmosTeal, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = session?.kelas?.ifBlank { "Umum" } ?: "Umum",
                                color = CosmosTeal,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!session?.absen.isNullOrBlank() && session?.absen != "-") {
                            Box(
                                modifier = Modifier
                                    .background(CosmosSecondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CosmosSecondary, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "No. ${session?.absen}",
                                    color = CosmosSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = CosmosCardBorder)

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status Sesi", color = CosmosTextMuted, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CosmosSuccess))
                                Text("Aktif (Lokal)", color = CosmosSuccess, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Latihan Diselesaikan", color = CosmosTextMuted, style = MaterialTheme.typography.bodyMedium)
                            Text("$attemptsCount Sesi", color = CosmosTextLight, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Koneksi AI (${baseUrlType.displayName})", color = CosmosTextMuted, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (baseUrl.isNotBlank()) "Terkonfigurasi" else "Belum Diatur",
                                color = if (baseUrl.isNotBlank()) CosmosTeal else CosmosWarning,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { isEditing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmosTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CosmosTextLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Profil Belajar", fontWeight = FontWeight.Bold, color = CosmosTextLight)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        border = BorderStroke(1.dp, CosmosSecondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmosSecondary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Konfigurasi API", tint = CosmosSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Base URL & API Key", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = CosmosTextMuted, fontWeight = FontWeight.SemiBold)
                    }

                } else {
                    Text(
                        text = "Perbarui Profil Belajar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmosTextLight,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Masukkan Nama dan informasi opsional untuk menyimpan sesi belajar lokal Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmosTextMuted,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = editNama,
                        onValueChange = {
                            editNama = it
                            editError = ""
                        },
                        label = { Text("Nama Lengkap *") },
                        colors = textFieldCosmosColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editKelas,
                        onValueChange = { editKelas = it },
                        label = { Text("Kelas / Bidang (Opsional)") },
                        colors = textFieldCosmosColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editAbsen,
                        onValueChange = { editAbsen = it },
                        label = { Text("Nomor Absen (Opsional)") },
                        colors = textFieldCosmosColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    if (editError.isNotEmpty()) {
                        Text(
                            text = editError,
                            color = CosmosError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            border = BorderStroke(1.dp, CosmosCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmosTextMuted),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Kembali", color = CosmosTextLight)
                        }

                        Button(
                            onClick = {
                                val n = editNama.trim()
                                val k = editKelas.trim()
                                val a = editAbsen.trim()
                                if (n.isEmpty()) {
                                    editError = "Nama Lengkap wajib diisi!"
                                } else {
                                    onRelogin(n, k, a)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmosTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f).height(48.dp)
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

