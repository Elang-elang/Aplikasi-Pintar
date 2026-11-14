package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EducationCatalog
import com.example.model.StudySuggestion
import com.example.ui.components.AcademicBookEmblem
import com.example.ui.components.StudyTypingIndicator
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
    val session by viewModel.userSession.collectAsState()
    val messages by viewModel.studyMessages.collectAsState()
    val chatState by viewModel.studyChatState.collectAsState()
    val attachedDoc by viewModel.studyAttachedDocument.collectAsState()
    val docExtractionState by viewModel.studyDocExtractionState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(StudyExplainMode.EXPLAIN_SIMPLE) }

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val profileBadge = remember(session) {
        EducationCatalog.formatProfileBadge(session?.sekolah, session?.kelas, session?.bidang)
    }
    val starterSuggestions = remember(session) {
        EducationCatalog.starterSuggestions(session?.sekolah, session?.kelas, session?.bidang)
    }

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
        containerColor = AcademicBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AcademicSurface)
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
                            contentDescription = "Kembali",
                            tint = AcademicTextPrimary
                        )
                    }

                    AcademicBookEmblem(size = 36.dp)

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tutor Belajar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AcademicTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profileBadge.isNotBlank()) {
                            Text(
                                text = profileBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = AcademicBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                                contentDescription = "Mulai Sesi Baru",
                                tint = AcademicTextMuted
                            )
                        }
                    }

                    ThemeToggleButton(viewModel = viewModel)
                }

                // Mode selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudyExplainMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) AcademicBlueTint else AcademicBackground,
                            label = "mode_chip_bg"
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = chipBg,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AcademicBlue else AcademicBorder
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
                                    color = if (isSelected) AcademicBlue else AcademicTextMuted
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = AcademicBorder)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AcademicSurface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(color = AcademicBorder)

                if (docExtractionState is UiState.Error) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .background(AcademicError.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .border(1.dp, AcademicError, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .testTag("study_doc_error_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error Dokumen",
                            tint = AcademicError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (docExtractionState as UiState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (docExtractionState is UiState.Loading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = AcademicBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Membaca dokumen...",
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicBlue
                        )
                    }
                }

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
                    FilledTonalIconButton(
                        onClick = {
                            documentPickerLauncher.launch(DocumentTextExtractor.SUPPORTED_MIME_TYPES)
                        },
                        enabled = docExtractionState !is UiState.Loading && chatState !is UiState.Loading,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = AcademicBlueTint,
                            contentColor = AcademicBlue
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
                                    "Tulis pertanyaan tentang dokumen..."
                                } else {
                                    "Tulis materi atau pertanyaan belajar..."
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
                            containerColor = AcademicBlue,
                            contentColor = Color.White,
                            disabledContainerColor = AcademicBlue.copy(alpha = 0.25f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("study_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Pertanyaan"
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
                    profileBadge = profileBadge,
                    suggestions = starterSuggestions,
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
                                Toast.makeText(context, "Penjelasan disalin", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (chatState is UiState.Loading) {
                        item(key = "loading_bubble") {
                            StudyTypingIndicator(label = "Menyusun penjelasan...")
                        }
                    }

                    if (chatState is UiState.Error) {
                        item(key = "error_bubble") {
                            val errMsg = (chatState as UiState.Error).message
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = AcademicError.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, AcademicError),
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
                                        contentDescription = "Peringatan",
                                        tint = AcademicError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AcademicTextPrimary,
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
    profileBadge: String,
    suggestions: List<StudySuggestion>,
    onPickDocument: () -> Unit,
    onQuickPromptClick: (String, StudyExplainMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AcademicBookEmblem(size = 60.dp)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Penjelasan Teks & Dokumen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AcademicTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tulis pertanyaan atau lampirkan file PDF, DOCX, PPTX, maupun XLSX (< 5 MB) untuk dijelaskan langkah demi langkah.",
            style = MaterialTheme.typography.bodySmall,
            color = AcademicTextMuted,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onPickDocument,
            border = BorderStroke(1.dp, AcademicBlue),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AcademicBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("study_welcome_pick_doc_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lampirkan Dokumen (< 5 MB)", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(22.dp))

        val suggestionHeader = if (profileBadge.isNotBlank()) {
            "Saran Belajar ($profileBadge)"
        } else {
            "Saran Materi"
        }

        Text(
            text = suggestionHeader,
            style = MaterialTheme.typography.labelLarge,
            color = AcademicTextMuted,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                    border = BorderStroke(1.dp, AcademicBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickPromptClick(item.prompt, item.mode) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AcademicBlue
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
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
            .background(AcademicBlueTint, RoundedCornerShape(12.dp))
            .border(1.dp, AcademicBlue.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("study_attached_doc_chip"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AcademicBlue)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Text(
                text = doc.formatLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.fileName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AcademicTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$sizeLabel • ${doc.extractedText.length} karakter",
                style = MaterialTheme.typography.labelSmall,
                color = AcademicBlue
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
                tint = AcademicError,
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
                containerColor = if (message.isUser) AcademicBlueTint else AcademicSurface
            ),
            border = BorderStroke(
                1.dp,
                if (message.isUser) AcademicBlue.copy(alpha = 0.35f) else AcademicBorder
            ),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (message.isUser) {
                    if (message.attachedDocName != null) {
                        Box(
                            modifier = Modifier
                                .background(AcademicBlue.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = message.attachedDocName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AcademicBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = message.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AcademicTextPrimary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Penjelasan Materi",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AcademicBlue
                        )
                        TextButton(
                            onClick = { onCopyResponse(message.displayText) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "Salin",
                                style = MaterialTheme.typography.labelSmall,
                                color = AcademicBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            text = message.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AcademicTextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
