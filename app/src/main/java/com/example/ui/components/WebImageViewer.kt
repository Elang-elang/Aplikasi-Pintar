package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import java.net.URI

@Composable
fun MarkdownWebImageCard(
    url: String,
    alt: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var retryToken by remember(url) { mutableIntStateOf(0) }
    var showFullscreen by remember { mutableStateOf(false) }
    var isLoaded by remember(url, retryToken) { mutableStateOf(false) }

    val context = LocalContext.current
    val request = remember(url, retryToken, context) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .setParameter("retry_token", retryToken, memoryCacheKey = null)
            .build()
    }

    val hostLabel = remember(url) {
        try {
            URI(url).host?.removePrefix("www.") ?: "Web"
        } catch (_: Exception) {
            "Web"
        }
    }

    if (showFullscreen) {
        FullscreenImageDialog(
            url = url,
            alt = alt,
            onDismiss = { showFullscreen = false }
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurfaceSubtle),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("markdown_web_image_card")
            .then(
                if (isLoaded) {
                    Modifier.clickable { showFullscreen = true }
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = alt.ifBlank { "Gambar ilustrasi materi" },
                contentScale = ContentScale.Fit,
                onSuccess = { isLoaded = true },
                onError = { isLoaded = false },
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 110.dp else 160.dp)
                            .background(AcademicSurfaceSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = AcademicBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Memuat gambar...",
                                style = MaterialTheme.typography.labelSmall,
                                color = AcademicTextMuted
                            )
                        }
                    }
                },
                error = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Gambar gagal dimuat",
                            tint = AcademicTextMuted,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = alt.ifBlank { "Gambar tidak dapat dimuat" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = AcademicTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sumber: $hostLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = AcademicTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { retryToken++ },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = AcademicBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Muat Ulang",
                                color = AcademicBlue,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (compact) 80.dp else 120.dp, max = if (compact) 180.dp else 280.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            if (isLoaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AcademicSurface)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alt.ifBlank { "Ilustrasi ($hostLabel)" },
                        style = MaterialTheme.typography.labelSmall,
                        color = AcademicTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(AcademicBlueTint, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Perbesar gambar",
                            tint = AcademicBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Perbesar",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AcademicBlue,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenImageDialog(
    url: String,
    alt: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF20B1120))
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("fullscreen_image_dialog")
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = alt.ifBlank { "Tampilan gambar penuh" },
                contentScale = ContentScale.Fit,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            offset = if (newScale > 1f) {
                                offset + pan
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.4f
                                }
                            }
                        )
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            // Top action bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = if (scale > 1.05f) "Zoom ${(scale * 100).toInt()}% • Ketuk 2x reset" else "Cubit untuk zoom • Ketuk 2x perbesar",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .testTag("fullscreen_image_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Tampilan Penuh",
                        tint = Color.White
                    )
                }
            }

            if (alt.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = alt,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
