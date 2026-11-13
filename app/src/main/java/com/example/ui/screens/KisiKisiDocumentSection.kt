package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.DocumentTextExtractor
import com.example.util.ExtractedDocument
import com.example.viewmodel.ExamViewModel
import com.example.viewmodel.UiState
import java.util.Locale

@Composable
fun KisiKisiDocumentSection(
    viewModel: ExamViewModel,
    onInsertTextToEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val attachedDoc by viewModel.attachedDocument.collectAsState()
    val extractionState by viewModel.documentExtractionState.collectAsState()
    var showT2TPreview by remember { mutableStateOf(false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            showT2TPreview = false
            viewModel.processKisiKisiDocument(uri)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Atau Unggah Dokumen Kisi-Kisi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CosmosTextLight
            )
            Box(
                modifier = Modifier
                    .background(CosmosSecondary.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                    .border(1.dp, CosmosSecondary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Maks < 5 MB",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmosSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CosmosSurface),
            border = BorderStroke(1.dp, CosmosCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Mendukung PDF, DOCX, PPTX, XLSX, ODT, ODP, TXT, MD, CSV, HTML, RTF. Dokumen otomatis diekstrak secara lokal menjadi teks terstruktur untuk model AI (T2T).",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmosTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        documentPickerLauncher.launch(DocumentTextExtractor.SUPPORTED_MIME_TYPES)
                    },
                    enabled = extractionState !is UiState.Loading,
                    border = BorderStroke(1.2.dp, CosmosTeal),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmosTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_document_button")
                ) {
                    if (extractionState is UiState.Loading) {
                        CircularProgressIndicator(
                            color = CosmosTeal,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Mengekstrak Dokumen ke Teks...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Pilih File Dokumen",
                            tint = CosmosTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (attachedDoc == null) "Pilih File Dokumen (< 5 MB)" else "Ganti File Dokumen (< 5 MB)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Error feedback
                if (extractionState is UiState.Error) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmosError.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(1.dp, CosmosError, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .testTag("document_error_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error Dokumen",
                            tint = CosmosError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (extractionState as UiState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmosTextLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Attached document summary & T2T preview
                attachedDoc?.let { doc ->
                    Spacer(modifier = Modifier.height(12.dp))
                    AttachedDocumentCard(
                        doc = doc,
                        showPreview = showT2TPreview,
                        onTogglePreview = { showT2TPreview = !showT2TPreview },
                        onCopyToEditor = { onInsertTextToEditor(doc.extractedText) },
                        onRemove = {
                            showT2TPreview = false
                            viewModel.clearAttachedDocument()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachedDocumentCard(
    doc: ExtractedDocument,
    showPreview: Boolean,
    onTogglePreview: () -> Unit,
    onCopyToEditor: () -> Unit,
    onRemove: () -> Unit
) {
    val sizeLabel = remember(doc.fileSizeBytes) {
        if (doc.fileSizeBytes >= 1024 * 1024) {
            String.format(Locale.US, "%.2f MB", doc.fileSizeBytes / (1024.0 * 1024.0))
        } else {
            String.format(Locale.US, "%.1f KB", doc.fileSizeBytes / 1024.0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmosTeal.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, CosmosTeal.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("attached_document_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmosTeal)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = doc.formatLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.fileName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CosmosTextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sizeLabel • ${doc.extractedText.length} karakter T2T",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmosTeal
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("remove_document_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Dokumen",
                    tint = CosmosError,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onTogglePreview,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = if (showPreview) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CosmosTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showPreview) "Sembunyikan Teks T2T" else "Lihat Teks T2T",
                    fontSize = 12.sp,
                    color = CosmosTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = onCopyToEditor,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = CosmosSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Salin ke Kolom Teks",
                    fontSize = 12.sp,
                    color = CosmosSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        AnimatedVisibility(visible = showPreview) {
            val previewText = remember(doc.t2tPayload) {
                if (doc.t2tPayload.length > 1500) {
                    doc.t2tPayload.take(1500) + "\n\n[...Preview dipotong. Total ${doc.t2tPayload.length} karakter siap dikirim ke AI...]"
                } else {
                    doc.t2tPayload
                }
            }
            Text(
                text = previewText,
                style = MaterialTheme.typography.bodySmall,
                color = CosmosTextMuted,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(CosmosBackground.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        }
    }
}
