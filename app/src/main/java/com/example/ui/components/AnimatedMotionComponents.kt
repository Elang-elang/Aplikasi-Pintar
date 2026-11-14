package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcademicBackground
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueTint
import com.example.ui.theme.AcademicBorder
import com.example.ui.theme.AcademicSurface
import com.example.ui.theme.AcademicTextMuted
import com.example.ui.theme.AcademicTextPrimary
import kotlinx.coroutines.delay

@Composable
fun AcademicBookEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    containerColor: Color = AcademicBlue,
    strokeColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.56f)) {
            val w = this.size.width
            val h = this.size.height
            val strokePx = w * 0.085f

            val bookPath = Path().apply {
                // Left page
                moveTo(w * 0.5f, h * 0.24f)
                cubicTo(w * 0.36f, h * 0.16f, w * 0.16f, h * 0.16f, w * 0.06f, h * 0.24f)
                lineTo(w * 0.06f, h * 0.80f)
                cubicTo(w * 0.18f, h * 0.72f, w * 0.38f, h * 0.72f, w * 0.5f, h * 0.82f)
                // Right page
                cubicTo(w * 0.62f, h * 0.72f, w * 0.82f, h * 0.72f, w * 0.94f, h * 0.80f)
                lineTo(w * 0.94f, h * 0.24f)
                cubicTo(w * 0.84f, h * 0.16f, w * 0.64f, h * 0.16f, w * 0.5f, h * 0.24f)
                close()
            }
            drawPath(
                path = bookPath,
                color = strokeColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Center spine
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.5f, h * 0.24f),
                end = Offset(w * 0.5f, h * 0.82f),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            // Subtle page lines
            drawLine(
                color = strokeColor.copy(alpha = 0.85f),
                start = Offset(w * 0.18f, h * 0.42f),
                end = Offset(w * 0.38f, h * 0.40f),
                strokeWidth = strokePx * 0.7f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor.copy(alpha = 0.85f),
                start = Offset(w * 0.62f, h * 0.40f),
                end = Offset(w * 0.82f, h * 0.42f),
                strokeWidth = strokePx * 0.7f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor.copy(alpha = 0.75f),
                start = Offset(w * 0.62f, h * 0.56f),
                end = Offset(w * 0.80f, h * 0.58f),
                strokeWidth = strokePx * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AnimatedSplashScreen(
    isSessionLoaded: Boolean,
    onSplashFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.8f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(18f) }
    val containerAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 300f))
        titleAlpha.animateTo(1f, tween(340))
        titleOffsetY.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(isSessionLoaded) {
        if (isSessionLoaded) {
            delay(850)
            containerAlpha.animateTo(0f, tween(260))
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(containerAlpha.value)
            .background(AcademicBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            AcademicBookEmblem(
                size = 84.dp,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = titleOffsetY.value.dp)
                    .alpha(titleAlpha.value)
            ) {
                Text(
                    text = "Pembelajaran dengan AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AcademicTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pendamping belajar sesuai jenjang sekolah",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AcademicTextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                AcademicDotsWave(dotSize = 7.dp)
            }
        }
    }
}

@Composable
fun AcademicDotsWave(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    dotColor: Color = AcademicBlue
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_wave")
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1Offset, dot2Offset, dot3Offset).forEach { offsetY ->
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(dotSize)
                    .background(dotColor, CircleShape)
            )
        }
    }
}

@Composable
fun StudyTypingIndicator(
    label: String = "Menyusun penjelasan materi...",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AcademicSurface),
            border = BorderStroke(1.dp, AcademicBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AcademicDotsWave(dotSize = 7.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicTextMuted
                )
            }
        }
    }
}

@Composable
fun AcademicPulseLoader(
    modifier: Modifier = Modifier,
    size: Dp = 92.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "api_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring"
    )

    val primaryColor = AcademicBlue
    val tintColor = AcademicBlueTint

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = this.size.minDimension / 2f
            val ringRadius = maxRadius * (0.45f + 0.55f * pulseProgress)
            val ringAlpha = (1f - pulseProgress) * 0.45f
            drawCircle(
                color = primaryColor.copy(alpha = ringAlpha),
                radius = ringRadius,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = tintColor,
                radius = maxRadius * 0.58f
            )
        }
        AcademicBookEmblem(size = size * 0.44f)
    }
}

@Composable
fun AcademicBookLoadingAnimation(
    title: String,
    subtitle: String,
    tips: List<String>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "book_loader")
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "line_sweep"
    )

    var tipIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(tips) {
        if (tips.size > 1) {
            while (true) {
                delay(3200)
                tipIndex = (tipIndex + 1) % tips.size
            }
        }
    }

    val blue = AcademicBlue
    val tint = AcademicBlueTint

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(104.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val center = Offset(w / 2f, h / 2f)

                drawCircle(
                    color = tint,
                    radius = w * 0.46f
                )
                drawArc(
                    color = blue,
                    startAngle = sweep * 360f - 90f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(w * 0.06f, h * 0.06f),
                    size = Size(w * 0.88f, h * 0.88f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Stylized crisp notebook page inside circle
                val pageW = w * 0.38f
                val pageH = h * 0.46f
                val pageLeft = center.x - pageW / 2f
                val pageTop = center.y - pageH / 2f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(pageLeft, pageTop),
                    size = Size(pageW, pageH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawRoundRect(
                    color = blue,
                    topLeft = Offset(pageLeft, pageTop),
                    size = Size(pageW, pageH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Animated writing lines
                val lineProgress1 = (sweep * 3f).coerceIn(0.15f, 1f)
                val lineProgress2 = ((sweep - 0.25f) * 3f).coerceIn(0.1f, 1f)
                val lineProgress3 = ((sweep - 0.5f) * 3f).coerceIn(0.1f, 1f)

                val lx = pageLeft + pageW * 0.2f
                val maxLen = pageW * 0.6f
                drawLine(
                    color = blue,
                    start = Offset(lx, pageTop + pageH * 0.30f),
                    end = Offset(lx + maxLen * lineProgress1, pageTop + pageH * 0.30f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = blue.copy(alpha = 0.7f),
                    start = Offset(lx, pageTop + pageH * 0.50f),
                    end = Offset(lx + maxLen * lineProgress2, pageTop + pageH * 0.50f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = blue.copy(alpha = 0.55f),
                    start = Offset(lx, pageTop + pageH * 0.70f),
                    end = Offset(lx + maxLen * 0.8f * lineProgress3, pageTop + pageH * 0.70f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AcademicTextPrimary,
            textAlign = TextAlign.Center
        )

        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AcademicTextMuted,
                textAlign = TextAlign.Center
            )
        }

        if (tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(26.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = AcademicSurface),
                border = BorderStroke(1.dp, AcademicBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = tips[tipIndex % tips.size],
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically { it / 4 }) togetherWith fadeOut(tween(200))
                    },
                    label = "study_tip_cycle"
                ) { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = AcademicTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }
}
