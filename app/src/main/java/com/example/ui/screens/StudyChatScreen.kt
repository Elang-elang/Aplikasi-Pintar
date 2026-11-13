package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.DocumentTextExtractor
import com.example.util.ExtractedDocument
import com.example.viewmodel.ExamViewModel
import com.example.viewmodel.StudyChatMessage
import com.example.viewmodel.StudyExplainMode
import com.example.viewmodel.UiState
import java.util.Locale

@Composable
fun StudyChatScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.studyMessages.collectAsState()
    val chatState by viewModel.studyChatState.collectAsState()
    val attachedDoc by viewModel.studyAttachedDocument.collectAsState()
    val docExtractionState by viewModel.studyDocExtractionState.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val baseUrlType by viewModel.baseUrlType.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(StudyExplainMode.EXPLAIN_SIMPLE) }

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.processStudyDocument(uri)
        }
    }

    LaunchedEffect(messages.size, chatState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = CosmosBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmosSurface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("study_chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Lobby",
                            tint = CosmosTextLight
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CosmosTeal, CosmosPrimary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tutor Belajar AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmosTextLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val activeModelLabel = modelName.ifBlank { baseUrlType.defaultModel }
                        Text(
                            text = "Penjelasan Teks & Dokumen (< 5 MB) • $activeModelLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmosTeal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (messages.isNotEmpty() || attachedDoc != null) {
                        IconButton(
                            onClick = {
                                viewModel.clearStudyChat()
                                Toast.makeText(context, "Percakapan dibersihkan", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("study_clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Mulai Obrolan Baru",
                                tint = CosmosTextMuted
                            )
                        }
                    }

                    ThemeToggleButton(viewModel = viewModel)
                }

                // Mode selector chips for learning explanations
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudyExplainMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) CosmosTeal.copy(alpha = 0.2f) else CosmosBackground,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CosmosTeal else CosmosCardBorder
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedMode = mode }
                                .testTag("study_mode_${mode.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CosmosTeal else CosmosTextMuted
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = CosmosCardBorder)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmosSurface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(color = CosmosCardBorder)

                // Document extraction error banner
                if (docExtractionState is UiState.Error) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .background(CosmosError.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(1.dp, CosmosError, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .testTag("study_doc_error_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error Dokumen",
                            tint = CosmosError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (docExtractionState as UiState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTextLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Document extracting progress bar
                if (docExtractionState is UiState.Loading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CosmosTeal,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mengubah dokumen menjadi teks T2T untuk AI...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTeal
                        )
                    }
                }

                // Pending attached document chip
                attachedDoc?.let { doc ->
                    PendingStudyDocChip(
                        doc = doc,
                        onRemove = { viewModel.clearStudyDocument() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Attach document (< 5 MB)
                    FilledTonalIconButton(
                        onClick = {
                            documentPickerLauncher.launch(DocumentTextExtractor.SUPPORTED_MIME_TYPES)
                        },
                        enabled = docExtractionState !is UiState.Loading && chatState !is UiState.Loading,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = CosmosTeal.copy(alpha = 0.16f),
                            contentColor = CosmosTeal
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("study_attach_doc_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Lampirkan Dokumen (< 5 MB)"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (attachedDoc != null) {
                                    "Tambahkan pertanyaan tentang dokumen..."
                                } else {
                                    "Tempel teks atau tanya materi belajar..."
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = textFieldCosmosColors(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 1,
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_chat_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = (inputText.isNotBlank() || attachedDoc != null) &&
                        chatState !is UiState.Loading &&
                        docExtractionState !is UiState.Loading

                    FilledIconButton(
                        onClick = {
                            if (canSend) {
                                val textToSend = inputText
                                inputText = ""
                                viewModel.sendStudyMessage(textToSend, selectedMode)
                            }
                        },
                        enabled = canSend,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = CosmosTeal,
                            contentColor = Color.White,
                            disabledContainerColor = CosmosTeal.copy(alpha = 0.25f),
                            disabledContentColor = Color.White.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("study_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Pesan Belajar"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty() && chatState !is UiState.Loading) {
                StudyWelcomeSection(
                    selectedMode = selectedMode,
                    onPickDocument = {
                        documentPickerLauncher.launch(DocumentTextExtractor.SUPPORTED_MIME_TYPES)
                    },
                    onQuickPromptClick = { prompt, mode ->
                        selectedMode = mode
                        inputText = prompt
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        StudyMessageBubble(
                            message = msg,
                            onCopyResponse = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Penjelasan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (chatState is UiState.Loading) {
                        item(key = "loading_bubble") {
                            StudyAiTypingBubble()
                        }
                    }

                    if (chatState is UiState.Error) {
                        item(key = "error_bubble") {
                            val errMsg = (chatState as UiState.Error).message
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CosmosError.copy(alpha = 0.14f)),
                                border = BorderStroke(1.dp, CosmosError),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("study_chat_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error Chat",
                                        tint = CosmosError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CosmosTextLight,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyWelcomeSection(
    selectedMode: StudyExplainMode,
    onPickDocument: () -> Unit,
    onQuickPromptClick: (String, StudyExplainMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CosmosTeal.copy(alpha = 0.15f))
                .border(1.5.dp, CosmosTeal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = CosmosTeal,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Penjelasan Teks & Dokumen AI",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CosmosTextLight
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Kirim uraian teks pelajaran atau unggah file dokumen (PDF, DOCX, PPTX, XLSX, TXT < 5 MB). AI akan menjelaskan materinya secara terstruktur untuk membantu Anda belajar.",
            style = MaterialTheme.typography.bodySmall,
            color = CosmosTextMuted,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onPickDocument,
            border = BorderStroke(1.2.dp, CosmosTeal),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmosTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("study_welcome_pick_doc_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unggah Dokumen Belajar (< 5 MB)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Contoh Permintaan Belajar (${selectedMode.label}):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CosmosTextMuted,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val quickSuggestions = listOf(
            Triple(
                "Jelaskan konsep fotosintesis tahap demi tahap agar mudah dipahami",
                "Penjelasan Konsep",
                StudyExplainMode.EXPLAIN_SIMPLE
            ),
            Triple(
                "Buatkan ringkasan poin-poin inti dari materi ini untuk persiapan ujian",
                "Ringkasan Materi",
                StudyExplainMode.SUMMARY
            ),
            Triple(
                "Berikan contoh soal dan pembahasan singkat dari topik Hukum Newton",
                "Contoh & Latihan",
                StudyExplainMode.EXAMPLES
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quickSuggestions.forEach { (promptText, label, mode) ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmosSurface),
                    border = BorderStroke(1.dp, CosmosCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickPromptClick(promptText, mode) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CosmosSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CosmosTeal
                            )
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmosTextLight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingStudyDocChip(
    doc: ExtractedDocument,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeLabel = remember(doc.fileSizeBytes) {
        if (doc.fileSizeBytes >= 1024 * 1024) {
            String.format(Locale.US, "%.2f MB", doc.fileSizeBytes / (1024.0 * 1024.0))
        } else {
            String.format(Locale.US, "%.1f KB", doc.fileSizeBytes / 1024.0)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CosmosTeal.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, CosmosTeal.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("study_attached_doc_chip"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(CosmosTeal)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Text(
                text = doc.formatLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.fileName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CosmosTextLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$sizeLabel • ${doc.extractedText.length} karakter T2T siap dijelaskan AI",
                style = MaterialTheme.typography.labelSmall,
                color = CosmosTeal
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(36.dp)
                .testTag("study_remove_doc_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus Lampiran",
                tint = CosmosError,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StudyMessageBubble(
    message: StudyChatMessage,
    onCopyResponse: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) CosmosPrimary.copy(alpha = 0.24f) else CosmosSurface
            ),
            border = BorderStroke(
                1.dp,
                if (message.isUser) CosmosPrimary.copy(alpha = 0.5f) else CosmosCardBorder
            ),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.modeLabel?.let { label ->
                            Box(
                                modifier = Modifier
                                    .background(CosmosSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmosSecondary
                                )
                            }
                        }
                        message.attachedDocName?.let { docName ->
                            Box(
                                modifier = Modifier
                                    .background(CosmosTeal.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Dokumen: $docName",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmosTeal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (message.modeLabel != null || message.attachedDocName != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = message.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmosTextLight
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tutor Belajar AI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmosTeal
                        )
                        TextButton(
                            onClick = { onCopyResponse(message.displayText) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "Salin",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmosSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            text = message.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmosTextLight,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyAiTypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CosmosSurface),
            border = BorderStroke(1.dp, CosmosCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = CosmosTeal,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AI sedang menyusun penjelasan belajar...",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmosTextMuted
                )
            }
        }
    }
}
