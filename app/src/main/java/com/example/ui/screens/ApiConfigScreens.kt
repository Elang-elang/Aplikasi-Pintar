package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
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
            colors = CardDefaults.cardColors(containerColor = CosmosError.copy(alpha = 0.14f)),
            border = BorderStroke(1.5.dp, CosmosError),
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
                        contentDescription = "Peringatan Koneksi Gagal",
                        tint = CosmosError,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Koneksi API Gagal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmosError
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CosmosTextLight
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
                            contentDescription = "Tutup Pesan",
                            tint = CosmosTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDetail = !showDetail }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showDetail) "Sembunyikan Detail Error" else "Lihat Detail Error",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmosError
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showDetail) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Detail",
                            tint = CosmosError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AnimatedVisibility(visible = showDetail) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTextMuted,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .background(CosmosBackground.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
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
            fontWeight = FontWeight.Bold,
            color = CosmosTeal
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ApiBaseUrlType.entries.forEach { type ->
                val isSelected = selectedType == type
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CosmosTeal else CosmosCardBorder
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CosmosTeal.copy(alpha = 0.16f) else CosmosSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectType(type) }
                        .testTag("base_url_type_${type.key}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CosmosTeal else CosmosTextLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (type) {
                                ApiBaseUrlType.ANTROPIC -> "/v1/messages"
                                ApiBaseUrlType.CHAT_ID -> "/chat/completions"
                                ApiBaseUrlType.RESPONSE -> "/v1/responses"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = CosmosTextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Format aktif: ${selectedType.endpointHint}",
            style = MaterialTheme.typography.bodySmall,
            color = CosmosTextMuted
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Konfigurasi API & Base URL",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmosTextLight
                    )
                    Text(
                        text = "Sesi Belajar: ${session?.nama ?: "Pengguna"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmosTeal
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
                            contentDescription = "Keluar Akun",
                            tint = CosmosError
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Toggle Message Banner when connection test fails
            ApiFailureToggleBanner(
                message = failureToggleMessage,
                detail = failureDetail,
                onDismiss = { viewModel.dismissApiFailureMessage() },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Info banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                border = BorderStroke(1.dp, CosmosCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(CosmosPrimary, CosmosTeal))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "API Setup",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pengaturan Manual Diperlukan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmosTextLight
                        )
                        Text(
                            text = "Atur Base URL, Tipe Base URL, dan API Key Anda secara manual, lalu uji koneksi (maks. 5 percobaan, timeout 5 menit) sebelum masuk ke Lobby.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tipe Base URL Selector (antropic / chat_id / response)
            ApiBaseUrlTypeSelector(
                selectedType = selectedType,
                onSelectType = { newType ->
                    selectedType = newType
                    validationError = ""
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Base URL Input
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
                    text = "Endpoint tujuan: $resolvedEndpointPreview",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmosTeal,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 6.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // API Key Input
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    validationError = ""
                },
                label = { Text("API Key") },
                placeholder = { Text("Masukkan API Key Anda...") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "API Key") },
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.Done else Icons.Default.Lock,
                            contentDescription = if (isApiKeyVisible) "Sembunyikan API Key" else "Tampilkan API Key",
                            tint = CosmosTeal
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

            // Model Name Input
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Model AI (Opsional, default: ${selectedType.defaultModel})") },
                placeholder = { Text(selectedType.defaultModel) },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Model AI") },
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
                    color = CosmosError,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    containerColor = CosmosTeal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_and_test_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan & Uji Koneksi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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
        containerColor = CosmosBackground
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
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(CosmosTeal.copy(alpha = 0.14f))
                    .border(2.dp, CosmosTeal.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CosmosTeal,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Menguji Koneksi API...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CosmosTextLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tipe: ${baseUrlType.displayName} • Maksimal ${testState.maxAttempts} request",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmosTextMuted,
                textAlign = TextAlign.Center
            )

            Text(
                text = baseUrl,
                style = MaterialTheme.typography.labelSmall,
                color = CosmosTeal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timeout Badge (5 minutes)
            Box(
                modifier = Modifier
                    .background(CosmosPrimary.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                    .border(1.dp, CosmosPrimary, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Batas Timeout 5 Menit: $timeoutText",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CosmosTextLight
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Attempts status list (1..5)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                border = BorderStroke(1.dp, CosmosCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Status Percobaan (${testState.currentAttempt}/${testState.maxAttempts})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CosmosTextLight
                    )
                    HorizontalDivider(color = CosmosCardBorder)

                    testState.attempts.forEach { log ->
                        val statusColor = when (log.status) {
                            AttemptStatus.PENDING -> CosmosTextMuted
                            AttemptStatus.RUNNING -> CosmosTeal
                            AttemptStatus.SUCCESS -> CosmosSuccess
                            AttemptStatus.FAILED -> CosmosError
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmosBackground.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                when (log.status) {
                                    AttemptStatus.RUNNING -> CircularProgressIndicator(
                                        color = CosmosTeal,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AttemptStatus.SUCCESS -> Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Berhasil",
                                        tint = CosmosSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AttemptStatus.FAILED -> Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Gagal",
                                        tint = CosmosError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AttemptStatus.PENDING -> Text(
                                        text = "${log.attemptNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmosTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Request #${log.attemptNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmosTextLight
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
                border = BorderStroke(1.dp, CosmosCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmosTextMuted),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_api_test_button")
            ) {
                Text("Batalkan & Kembali", fontWeight = FontWeight.SemiBold, color = CosmosTextLight)
            }
        }
    }
}
