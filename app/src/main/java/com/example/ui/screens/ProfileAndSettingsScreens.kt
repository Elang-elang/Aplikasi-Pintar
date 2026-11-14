package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.api.ApiBaseUrlType
import com.example.api.GeminiApiClient
import com.example.database.UserSession
import com.example.model.EducationCatalog
import com.example.model.SchoolLevel
import com.example.ui.components.AcademicProfileFormFields
import com.example.ui.theme.*
import com.example.viewmodel.ExamViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SettingsScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onStartTest: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    BackHandler(enabled = true) {
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
                    containerColor = AcademicError,
                    contentColor = Color.White
                )
            }
        },
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
                            text = "Pengaturan Koneksi",
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

            ApiFailureToggleBanner(
                message = failureToggleMessage,
                detail = failureDetail,
                onDismiss = { viewModel.dismissApiFailureMessage() },
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Text(
                text = "Ubah Base URL (mendukung domain publik maupun https://localhost / 127.0.0.1), API Key, atau model yang digunakan.",
                style = MaterialTheme.typography.bodySmall,
                color = AcademicTextMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

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
                    text = "Tujuan: $resolvedEndpointPreview",
                    style = MaterialTheme.typography.labelSmall,
                    color = AcademicBlue,
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
                label = { Text("API Key") },
                placeholder = { Text("Masukkan API Key...") },
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.Done else Icons.Default.Lock,
                            contentDescription = if (isApiKeyVisible) "Sembunyikan" else "Tampilkan",
                            tint = AcademicBlue
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
                label = { Text("Model (Default: ${baseUrlType.defaultModel})") },
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
                    color = AcademicError,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val trimmedUrl = GeminiApiClient.normalizeBaseUrlInput(baseUrl)
                        val trimmedKey = apiKey.trim()
                        when {
                            trimmedUrl.isBlank() -> {
                                validationError = "Base URL wajib diisi."
                            }
                            !GeminiApiClient.isValidBaseUrlInput(trimmedUrl) -> {
                                validationError = "Base URL harus diawali http:// atau https:// (atau localhost / 127.0.0.1)."
                            }
                            trimmedKey.isBlank() -> {
                                validationError = "API Key wajib diisi."
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
                    colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(52.dp)
                        .testTag("settings_save_and_test_button")
                ) {
                    Text("Simpan & Uji", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.logout()
                        Toast.makeText(context, "Keluar dari profil belajar", Toast.LENGTH_SHORT).show()
                        onLogout()
                    },
                    border = BorderStroke(1.dp, AcademicError),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AcademicError),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("settings_logout_button")
                ) {
                    Text("Keluar", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun textFieldCosmosColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AcademicBlue,
    unfocusedBorderColor = AcademicBorder,
    focusedLabelColor = AcademicBlue,
    unfocusedLabelColor = AcademicTextMuted,
    focusedTextColor = AcademicTextPrimary,
    unfocusedTextColor = AcademicTextPrimary,
    focusedPlaceholderColor = AcademicTextMuted,
    unfocusedPlaceholderColor = AcademicTextMuted,
    focusedContainerColor = AcademicSurface,
    unfocusedContainerColor = AcademicSurface
)

@Composable
fun ThemeToggleButton(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val desc = if (isDarkTheme) "Beralih ke mode terang" else "Beralih ke mode gelap"
    FilledTonalIconButton(
        onClick = { viewModel.toggleTheme() },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = AcademicSurfaceSubtle,
            contentColor = AcademicTextPrimary
        ),
        modifier = modifier
            .size(48.dp)
            .testTag("theme_toggle_button")
            .semantics { contentDescription = desc }
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            if (isDarkTheme) {
                val path = Path().apply {
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
                drawPath(path, color = Color(0xFFFBBF24))
            } else {
                val center = center
                val radius = size.minDimension / 3.6f
                drawCircle(
                    color = Color(0xFF1D4ED8),
                    radius = radius,
                    center = center
                )
                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4
                    val startX = (center.x + (radius + 2.dp.toPx()) * cos(angle)).toFloat()
                    val startY = (center.y + (radius + 2.dp.toPx()) * sin(angle)).toFloat()
                    val endX = (center.x + (radius + 4.5.dp.toPx()) * cos(angle)).toFloat()
                    val endY = (center.y + (radius + 4.5.dp.toPx()) * sin(angle)).toFloat()
                    drawLine(
                        color = Color(0xFF1D4ED8),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileAccountDialog(
    session: UserSession?,
    attemptsCount: Int,
    baseUrl: String,
    baseUrlType: ApiBaseUrlType,
    onDismiss: () -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    var editNama by remember(session) { mutableStateOf(session?.nama ?: "") }
    var editSekolah by remember(session) { mutableStateOf(SchoolLevel.fromDisplayName(session?.sekolah)) }
    var editKelas by remember(session) { mutableStateOf(session?.kelas) }
    var editBidang by remember(session) { mutableStateOf(session?.bidang) }
    var showErrors by remember { mutableStateOf(false) }

    val validation = remember(editNama, editSekolah, editKelas, editBidang) {
        EducationCatalog.validateRegistration(editNama, editSekolah, editKelas, editBidang)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = AcademicSurface,
            border = BorderStroke(1.dp, AcademicBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isEditing) {
                    val firstChar = session?.nama?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(AcademicBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstChar,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = session?.nama ?: "Pelajar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AcademicTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val badge = EducationCatalog.formatProfileBadge(
                        session?.sekolah,
                        session?.kelas,
                        session?.bidang
                    )
                    if (badge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(AcademicBlueTint, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = badge,
                                color = AcademicBlue,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = AcademicBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Latihan Diselesaikan", color = AcademicTextMuted, style = MaterialTheme.typography.bodyMedium)
                            Text("$attemptsCount Sesi", color = AcademicTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Koneksi (${baseUrlType.displayName})", color = AcademicTextMuted, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (baseUrl.isNotBlank()) "Aktif" else "Belum Diatur",
                                color = if (baseUrl.isNotBlank()) AcademicSuccess else AcademicWarning,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = { isEditing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Profil Belajar", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        border = BorderStroke(1.dp, AcademicBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AcademicTextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_open_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan API", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = AcademicTextMuted)
                    }
                } else {
                    Text(
                        text = "Perbarui Profil Belajar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AcademicTextPrimary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 14.dp)
                    )

                    AcademicProfileFormFields(
                        nama = editNama,
                        onNamaChange = { editNama = it },
                        selectedSekolah = editSekolah,
                        onSekolahSelect = { newLevel ->
                            if (editSekolah != newLevel) {
                                editSekolah = newLevel
                                if (editKelas !in newLevel.classes) editKelas = null
                                if (editBidang !in newLevel.subjects) editBidang = "Umum"
                            }
                        },
                        selectedKelas = editKelas,
                        onKelasSelect = { editKelas = it },
                        selectedBidang = editBidang,
                        onBidangSelect = { editBidang = it },
                        validation = validation,
                        showValidationErrors = showErrors
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            border = BorderStroke(1.dp, AcademicBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AcademicTextPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                if (validation.isValid && editSekolah != null && editKelas != null && editBidang != null) {
                                    onSaveProfile(editNama.trim(), editSekolah!!.displayName, editKelas!!, editBidang!!)
                                } else {
                                    showErrors = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("save_profile_button")
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
