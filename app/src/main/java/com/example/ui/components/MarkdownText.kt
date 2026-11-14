package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun RichMarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = AcademicTextPrimary,
    baseTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    selectable: Boolean = false,
    compact: Boolean = false
) {
    val blocks = remember(markdown) { MarkdownParser.parseBlocks(markdown) }
    val linkColor = AcademicBlue
    val inlineCodeBg = AcademicSurfaceSubtle
    val inlineCodeColor = if (LocalThemeIsDark.current) Color(0xFF93C5FD) else Color(0xFF0F4C81)

    val content: @Composable () -> Unit = {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val headingStyle = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            3 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = rememberMarkdownAnnotatedString(
                                text = block.text,
                                linkColor = linkColor,
                                inlineCodeBg = inlineCodeBg,
                                inlineCodeColor = inlineCodeColor
                            ),
                            style = headingStyle,
                            color = textColor,
                            modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp)
                        )
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = rememberMarkdownAnnotatedString(
                                text = block.text,
                                linkColor = linkColor,
                                inlineCodeBg = inlineCodeBg,
                                inlineCodeColor = inlineCodeColor
                            ),
                            style = baseTextStyle.copy(
                                lineHeight = if (compact) 20.sp else 22.sp
                            ),
                            color = textColor
                        )
                    }

                    is MarkdownBlock.BulletListItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (block.indentLevel * 14).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = if (block.indentLevel == 0) "•" else "◦",
                                style = baseTextStyle,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBlue,
                                modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                            )
                            Text(
                                text = rememberMarkdownAnnotatedString(
                                    text = block.text,
                                    linkColor = linkColor,
                                    inlineCodeBg = inlineCodeBg,
                                    inlineCodeColor = inlineCodeColor
                                ),
                                style = baseTextStyle.copy(lineHeight = if (compact) 20.sp else 22.sp),
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is MarkdownBlock.OrderedListItem -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${block.number}.",
                                style = baseTextStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = AcademicBlue,
                                modifier = Modifier
                                    .widthIn(min = 22.dp)
                                    .padding(end = 6.dp)
                            )
                            Text(
                                text = rememberMarkdownAnnotatedString(
                                    text = block.text,
                                    linkColor = linkColor,
                                    inlineCodeBg = inlineCodeBg,
                                    inlineCodeColor = inlineCodeColor
                                ),
                                style = baseTextStyle.copy(lineHeight = if (compact) 20.sp else 22.sp),
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is MarkdownBlock.BlockQuote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AcademicBlueTint.copy(alpha = 0.45f))
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(AcademicBlue)
                            )
                            Text(
                                text = rememberMarkdownAnnotatedString(
                                    text = block.text,
                                    linkColor = linkColor,
                                    inlineCodeBg = inlineCodeBg,
                                    inlineCodeColor = inlineCodeColor
                                ),
                                style = baseTextStyle.copy(
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 21.sp
                                ),
                                color = textColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    is MarkdownBlock.CodeBlock -> {
                        MarkdownCodeBlockCard(
                            language = block.language,
                            code = block.code
                        )
                    }

                    is MarkdownBlock.Table -> {
                        MarkdownTableCard(
                            headers = block.headers,
                            rows = block.rows,
                            linkColor = linkColor,
                            inlineCodeBg = inlineCodeBg,
                            inlineCodeColor = inlineCodeColor
                        )
                    }

                    is MarkdownBlock.Image -> {
                        MarkdownWebImageCard(
                            url = block.url,
                            alt = block.alt,
                            compact = compact
                        )
                    }

                    is MarkdownBlock.HorizontalRule -> {
                        HorizontalDivider(
                            color = AcademicBorder,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (selectable) {
        SelectionContainer {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun rememberMarkdownAnnotatedString(
    text: String,
    linkColor: Color,
    inlineCodeBg: Color,
    inlineCodeColor: Color
): AnnotatedString {
    return remember(text, linkColor, inlineCodeBg, inlineCodeColor) {
        buildMarkdownAnnotatedString(
            text = text,
            linkColor = linkColor,
            inlineCodeBg = inlineCodeBg,
            inlineCodeColor = inlineCodeColor
        )
    }
}

fun buildMarkdownAnnotatedString(
    text: String,
    linkColor: Color,
    inlineCodeBg: Color,
    inlineCodeColor: Color
): AnnotatedString {
    val tokens = MarkdownParser.parseInline(text)
    return buildAnnotatedString {
        tokens.forEach { token ->
            when (token) {
                is InlineMarkdownToken.Text -> {
                    val decoration = if (token.strikethrough) TextDecoration.LineThrough else null
                    val style = SpanStyle(
                        fontWeight = if (token.bold) FontWeight.Bold else null,
                        fontStyle = if (token.italic) FontStyle.Italic else null,
                        textDecoration = decoration
                    )
                    if (token.bold || token.italic || token.strikethrough) {
                        withStyle(style) {
                            append(token.content)
                        }
                    } else {
                        append(token.content)
                    }
                }

                is InlineMarkdownToken.InlineCode -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = inlineCodeBg,
                            color = inlineCodeColor,
                            fontSize = 13.sp
                        )
                    ) {
                        append(" ${token.code} ")
                    }
                }

                is InlineMarkdownToken.Link -> {
                    withLink(
                        LinkAnnotation.Url(
                            url = token.url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        )
                    ) {
                        append(token.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownCodeBlockCard(
    language: String?,
    code: String
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val isDark = LocalThemeIsDark.current
    val codeBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val headerBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = codeBg),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language?.lowercase()?.ifBlank { "kode" } ?: "kode",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = AcademicTextMuted
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        Toast.makeText(context, "Kode disalin", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = AcademicTextPrimary
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableCard(
    headers: List<String>,
    rows: List<List<String>>,
    linkColor: Color,
    inlineCodeBg: Color,
    inlineCodeColor: Color
) {
    val colCount = headers.size.coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AcademicSurface),
        border = BorderStroke(1.dp, AcademicBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .background(AcademicBlueTint)
                    .border(0.5.dp, AcademicBorder)
            ) {
                for (c in 0 until colCount) {
                    Text(
                        text = rememberMarkdownAnnotatedString(
                            text = headers.getOrElse(c) { "" },
                            linkColor = linkColor,
                            inlineCodeBg = inlineCodeBg,
                            inlineCodeColor = inlineCodeColor
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AcademicTextPrimary,
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 220.dp)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            // Table Body
            rows.forEachIndexed { rIdx, row ->
                val rowBg = if (rIdx % 2 == 1) AcademicSurfaceSubtle.copy(alpha = 0.55f) else AcademicSurface
                HorizontalDivider(color = AcademicBorder, thickness = 0.5.dp)
                Row(modifier = Modifier.background(rowBg)) {
                    for (c in 0 until colCount) {
                        Text(
                            text = rememberMarkdownAnnotatedString(
                                text = row.getOrElse(c) { "" },
                                linkColor = linkColor,
                                inlineCodeBg = inlineCodeBg,
                                inlineCodeColor = inlineCodeColor
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AcademicTextPrimary,
                            modifier = Modifier
                                .widthIn(min = 100.dp, max = 220.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
