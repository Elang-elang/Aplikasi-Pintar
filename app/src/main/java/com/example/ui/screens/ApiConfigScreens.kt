package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.ApiBaseUrlType
import com.example.api.GeminiApiClient
import com.example.model.EducationCatalog
import com.example.ui.components.AcademicPulseLoader
import com.example.ui.theme.*
import com.example.viewmodel.AttemptStatus
import com.example.viewmodel.ExamViewModel

@Composable
fun ApiFailureToggleBanner(
    message: String?,
    detail: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetail by remember(message) { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AcademicError.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, AcademicError),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_failure_toggle_banner")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Koneksi Gagal",
                        tint = AcademicError,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Koneksi Tidak Berhasil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AcademicError
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dismiss_failure_banner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = AcademicTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDetail = !showDetail }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showDetail) "Sembunyikan Detail" else "Lihat Detail",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AcademicError
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showDetail) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Detail Error",
                            tint = AcademicError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AnimatedVisibility(visible = showDetail) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicTextMuted,
                            modifier = Modifier
                                .padding(top = 6.dp)
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

@Composable
fun ApiBaseUrlTypeSelector(
    selectedType: ApiBaseUrlType,
    onSelectType: (ApiBaseUrlType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Tipe Base URL",
            style = MaterialTheme.typography.titleSmall,
            color = AcademicTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ApiBaseUrlType.entries.forEach { type ->
                val isSelected = selectedType == type
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) AcademicBlueTint else AcademicSurface,
                    label = "type_bg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) AcademicBlue else AcademicBorder,
                    label = "type_border"
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = borderColor
                    ),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectType(type) }
                        .testTag("base_url_type_${type.key}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AcademicBlue else AcademicTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = type.badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = AcademicTextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = selectedType.endpointHint,
            style = MaterialTheme.typography.bodySmall,
            color = AcademicTextMuted
        )
    }
}

@Composable
fun ApiConfigScreen(
    viewModel: ExamViewModel,
    onStartTest: () -> Unit,
    onLogout: () -> Unit
) {
    val savedBaseUrl by viewModel.baseUrl.collectAsState()
    val savedBaseUrlType by viewModel.baseUrlType.collectAsState()
    val savedApiKey by viewModel.customApiKey.collectAsState()
    val savedModelName by viewModel.modelName.collectAsState()
    val session by viewModel.userSession.collectAsState()
    val failureToggleMessage by viewModel.apiConfigFailureToggleMessage.collectAsState()
    val failureDetail by viewModel.apiConfigFailureDetail.collectAsState()

    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var selectedType by remember(savedBaseUrlType) { mutableStateOf(savedBaseUrlType) }
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var modelName by remember(savedModelName) { mutableStateOf(savedModelName) }
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

    val resolvedEndpointPreview = remember(baseUrl, selectedType) {
        try {
            if (baseUrl.isBlank()) null else GeminiApiClient.resolveEndpointUrl(baseUrl, selectedType)
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
            Surface(
                color = AcademicSurface,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengaturan Koneksi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AcademicTextPrimary
                            )
                            val badge = EducationCatalog.formatProfileBadge(
                                session?.sekolah,
                                session?.kelas,
                                session?.bidang
                            )
                            Text(
                                text = listOfNotNull(session?.nama, badge.takeIf { it.isNotBlank() }).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = AcademicTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ThemeToggleButton(viewModel = viewModel)
                            IconButton(
                                onClick = {
                                    viewModel.logout()
                                    onLogout()
                                },
                                modifier = Modifier.testTag("api_config_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Keluar Profil",
                                    tint = AcademicError
                                )
                            }
                        }
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
                text = "Hubungkan ke layanan model pilihan Anda (domain publik maupun https://localhost atau 127.0.0.1).",
                style = MaterialTheme.typography.bodyMedium,
                color = AcademicTextMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            ApiBaseUrlTypeSelector(
                selectedType = selectedType,
                onSelectType = { newType ->
                    selectedType = newType
                    validationError = ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    validationError = ""
                },
                label = { Text("Base URL") },
                placeholder = { Text(selectedType.placeholderUrl) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Base URL") },
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("base_url_input")
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

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    validationError = ""
                },
                label = { Text("API Key") },
                placeholder = { Text("Masukkan kunci API Anda") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "API Key") },
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.Done else Icons.Default.Lock,
                            contentDescription = if (isApiKeyVisible) "Sembunyikan API Key" else "Tampilkan API Key",
                            tint = AcademicBlue
                        )
                    }
                },
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Nama Model (Opsional)") },
                placeholder = { Text(selectedType.defaultModel) },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Nama Model") },
                colors = textFieldCosmosColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("model_name_input")
            )

            if (validationError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError,
                    color = AcademicError,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                            val effectiveModel = modelName.trim().ifBlank { selectedType.defaultModel }
                            viewModel.startConnectionTest(
                                baseUrl = trimmedUrl,
                                baseUrlType = selectedType,
                                apiKey = trimmedKey,
                                model = effectiveModel
                            )
                            onStartTest()
                        }
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
                    .testTag("save_and_test_button")
            ) {
                Text(
                    text = "Simpan & Uji Koneksi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ApiConnectionTestScreen(
    viewModel: ExamViewModel,
    onTestSuccess: () -> Unit,
    onTestFailed: () -> Unit,
    onCancel: () -> Unit
) {
    val testState by viewModel.apiTestState.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val baseUrlType by viewModel.baseUrlType.collectAsState()

    BackHandler(enabled = true) {
        viewModel.cancelConnectionTest()
        onCancel()
    }

    LaunchedEffect(testState.result) {
        when (testState.result) {
            true -> {
                viewModel.resetApiTestResult()
                onTestSuccess()
            }
            false -> {
                viewModel.resetApiTestResult()
                onTestFailed()
            }
            null -> Unit
        }
    }

    val remMin = testState.remainingSeconds / 60
    val remSec = testState.remainingSeconds % 60
    val timeoutText = String.format("%02d:%02d", remMin, remSec)

    Scaffold(
        containerColor = AcademicBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AcademicPulseLoader(size = 96.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Menguji Koneksi...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AcademicTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${baseUrlType.displayName} • Sisa waktu $timeoutText",
                style = MaterialTheme.typography.bodyMedium,
                color = AcademicTextMuted,
                textAlign = TextAlign.Center
            )

            Text(
                text = baseUrl,
                style = MaterialTheme.typography.labelSmall,
                color = AcademicBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                border = BorderStroke(1.dp, AcademicBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Percobaan (${testState.currentAttempt}/${testState.maxAttempts})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AcademicTextPrimary
                    )
                    HorizontalDivider(color = AcademicBorder)

                    testState.attempts.forEach { log ->
                        val statusColor = when (log.status) {
                            AttemptStatus.PENDING -> AcademicTextMuted
                            AttemptStatus.RUNNING -> AcademicBlue
                            AttemptStatus.SUCCESS -> AcademicSuccess
                            AttemptStatus.FAILED -> AcademicError
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AcademicSurfaceSubtle, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                when (log.status) {
                                    AttemptStatus.RUNNING -> CircularProgressIndicator(
                                        color = AcademicBlue,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AttemptStatus.SUCCESS -> Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Berhasil",
                                        tint = AcademicSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AttemptStatus.FAILED -> Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Gagal",
                                        tint = AcademicError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AttemptStatus.PENDING -> Text(
                                        text = "${log.attemptNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AcademicTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permintaan #${log.attemptNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AcademicTextPrimary
                                )
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    viewModel.cancelConnectionTest()
                    onCancel()
                },
                border = BorderStroke(1.dp, AcademicBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AcademicTextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_api_test_button")
            ) {
                Text("Batalkan", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
