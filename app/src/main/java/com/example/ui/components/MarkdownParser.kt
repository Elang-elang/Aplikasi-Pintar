package com.example.ui.components

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletListItem(val indentLevel: Int, val text: String) : MarkdownBlock
    data class OrderedListItem(val number: String, val text: String) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class Image(val alt: String, val url: String) : MarkdownBlock
    data object HorizontalRule : MarkdownBlock
}

sealed interface InlineMarkdownToken {
    data class Text(
        val content: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strikethrough: Boolean = false
    ) : InlineMarkdownToken

    data class InlineCode(val code: String) : InlineMarkdownToken
    data class Link(val label: String, val url: String) : InlineMarkdownToken
}

object MarkdownParser {

    private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.+?)\s*#*\s*$""")
    private val HR_REGEX = Regex("""^\s*([-*_])\s*\1\s*\1(\s*\1)*\s*$""")
    private val BULLET_REGEX = Regex("""^(\s*)([-*+•])\s+(.+)$""")
    private val ORDERED_REGEX = Regex("""^(\s*)(\d+)[.)]\s+(.+)$""")
    private val TABLE_SEPARATOR_REGEX = Regex("""^\s*\|?(\s*:?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?\s*$""")
    private val IMAGE_SYNTAX_REGEX = Regex("""!\[([^\]]*)]\((https?://[^\s)]+)\)""", RegexOption.IGNORE_CASE)

    fun parseBlocks(markdown: String): List<MarkdownBlock> {
        if (markdown.isBlank()) return emptyList()

        val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        fun flushParagraph(buffer: MutableList<String>) {
            if (buffer.isEmpty()) return
            val joined = buffer.joinToString("\n").trim()
            if (joined.isNotEmpty()) {
                splitTextWithEmbeddedImages(joined, blocks)
            }
            buffer.clear()
        }

        val paragraphBuffer = mutableListOf<String>()

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Blank line closes open paragraph
            if (trimmed.isEmpty()) {
                flushParagraph(paragraphBuffer)
                i++
                continue
            }

            // 2. Fenced code block (``` or ~~~)
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                flushParagraph(paragraphBuffer)
                val fence = if (trimmed.startsWith("```")) "```" else "~~~"
                val lang = trimmed.removePrefix(fence).trim().takeIf { it.isNotEmpty() }
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith(fence)) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // consume closing fence
                blocks.add(MarkdownBlock.CodeBlock(language = lang, code = codeLines.joinToString("\n")))
                continue
            }

            // 3. Heading
            val headingMatch = HEADING_REGEX.matchEntire(trimmed)
            if (headingMatch != null) {
                flushParagraph(paragraphBuffer)
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2].trim()
                blocks.add(MarkdownBlock.Heading(level = level, text = text))
                i++
                continue
            }

            // 4. Horizontal rule
            if (HR_REGEX.matches(trimmed)) {
                flushParagraph(paragraphBuffer)
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // 5. Markdown table (header + separator row + optional data rows)
            if (i + 1 < lines.size && line.contains("|") && TABLE_SEPARATOR_REGEX.matches(lines[i + 1].trim())) {
                flushParagraph(paragraphBuffer)
                val headers = parseTableRow(line)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (rowLine.isEmpty() || !rowLine.contains("|") || rowLine.startsWith("```")) {
                        break
                    }
                    val parsedRow = parseTableRow(rowLine)
                    if (parsedRow.isNotEmpty()) {
                        val normalizedRow = List(headers.size) { colIdx ->
                            parsedRow.getOrElse(colIdx) { "" }
                        }
                        rows.add(normalizedRow)
                    }
                    i++
                }
                if (headers.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Table(headers = headers, rows = rows))
                }
                continue
            }

            // 6. Blockquote
            if (trimmed.startsWith(">")) {
                flushParagraph(paragraphBuffer)
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trimStart())
                    i++
                }
                blocks.add(MarkdownBlock.BlockQuote(text = quoteLines.joinToString("\n").trim()))
                continue
            }

            // 7. Bullet list item
            val bulletMatch = BULLET_REGEX.matchEntire(line)
            if (bulletMatch != null) {
                flushParagraph(paragraphBuffer)
                val indent = (bulletMatch.groupValues[1].length / 2).coerceIn(0, 3)
                val itemText = bulletMatch.groupValues[3].trim()
                blocks.add(MarkdownBlock.BulletListItem(indentLevel = indent, text = itemText))
                i++
                continue
            }

            // 8. Ordered list item
            val orderedMatch = ORDERED_REGEX.matchEntire(line)
            if (orderedMatch != null) {
                flushParagraph(paragraphBuffer)
                val number = orderedMatch.groupValues[2]
                val itemText = orderedMatch.groupValues[3].trim()
                blocks.add(MarkdownBlock.OrderedListItem(number = number, text = itemText))
                i++
                continue
            }

            // 9. Standalone web image line
            val fullImageMatch = IMAGE_SYNTAX_REGEX.matchEntire(trimmed)
            if (fullImageMatch != null) {
                flushParagraph(paragraphBuffer)
                blocks.add(
                    MarkdownBlock.Image(
                        alt = fullImageMatch.groupValues[1].trim(),
                        url = fullImageMatch.groupValues[2].trim()
                    )
                )
                i++
                continue
            }

            // 10. Regular paragraph text line
            paragraphBuffer.add(line)
            i++
        }

        flushParagraph(paragraphBuffer)
        return blocks
    }

    private fun splitTextWithEmbeddedImages(text: String, out: MutableList<MarkdownBlock>) {
        val matches = IMAGE_SYNTAX_REGEX.findAll(text).toList()
        if (matches.isEmpty()) {
            out.add(MarkdownBlock.Paragraph(text))
            return
        }

        var cursor = 0
        for (match in matches) {
            if (match.range.first > cursor) {
                val before = text.substring(cursor, match.range.first).trim()
                if (before.isNotEmpty()) {
                    out.add(MarkdownBlock.Paragraph(before))
                }
            }
            out.add(
                MarkdownBlock.Image(
                    alt = match.groupValues[1].trim(),
                    url = match.groupValues[2].trim()
                )
            )
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            val after = text.substring(cursor).trim()
            if (after.isNotEmpty()) {
                out.add(MarkdownBlock.Paragraph(after))
            }
        }
    }

    private fun parseTableRow(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) trimmed = trimmed.drop(1)
        if (trimmed.endsWith("|")) trimmed = trimmed.dropLast(1)
        return trimmed.split("|").map { it.trim() }
    }

    fun parseInline(text: String): List<InlineMarkdownToken> {
        if (text.isEmpty()) return emptyList()
        val tokens = mutableListOf<InlineMarkdownToken>()
        var idx = 0
        val plainBuffer = StringBuilder()

        fun flushPlain() {
            if (plainBuffer.isNotEmpty()) {
                tokens.add(InlineMarkdownToken.Text(plainBuffer.toString()))
                plainBuffer.clear()
            }
        }

        while (idx < text.length) {
            // 1. Inline code `...`
            if (text[idx] == '`') {
                val endIdx = text.indexOf('`', idx + 1)
                if (endIdx != -1) {
                    flushPlain()
                    tokens.add(InlineMarkdownToken.InlineCode(text.substring(idx + 1, endIdx)))
                    idx = endIdx + 1
                    continue
                }
            }

            // 2. Link [label](https://...)
            if (text[idx] == '[') {
                val closeBracket = text.indexOf(']', idx + 1)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val label = text.substring(idx + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen).trim()
                        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                            flushPlain()
                            tokens.add(InlineMarkdownToken.Link(label = label.ifBlank { url }, url = url))
                            idx = closeParen + 1
                            continue
                        }
                    }
                }
            }

            // 3. Bold + Italic ***...*** or ___...___
            if (text.startsWith("***", idx) || text.startsWith("___", idx)) {
                val marker = text.substring(idx, idx + 3)
                val endIdx = text.indexOf(marker, idx + 3)
                if (endIdx != -1) {
                    flushPlain()
                    tokens.add(
                        InlineMarkdownToken.Text(
                            content = text.substring(idx + 3, endIdx),
                            bold = true,
                            italic = true
                        )
                    )
                    idx = endIdx + 3
                    continue
                }
            }

            // 4. Bold **...** or __...__
            if (text.startsWith("**", idx) || text.startsWith("__", idx)) {
                val marker = text.substring(idx, idx + 2)
                val endIdx = text.indexOf(marker, idx + 2)
                if (endIdx != -1 && endIdx > idx + 2) {
                    flushPlain()
                    tokens.add(
                        InlineMarkdownToken.Text(
                            content = text.substring(idx + 2, endIdx),
                            bold = true
                        )
                    )
                    idx = endIdx + 2
                    continue
                }
            }

            // 5. Strikethrough ~~...~~
            if (text.startsWith("~~", idx)) {
                val endIdx = text.indexOf("~~", idx + 2)
                if (endIdx != -1 && endIdx > idx + 2) {
                    flushPlain()
                    tokens.add(
                        InlineMarkdownToken.Text(
                            content = text.substring(idx + 2, endIdx),
                            strikethrough = true
                        )
                    )
                    idx = endIdx + 2
                    continue
                }
            }

            // 6. Italic *...* (single asterisk)
            if (text[idx] == '*' && !text.startsWith("**", idx)) {
                val endIdx = text.indexOf('*', idx + 1)
                if (endIdx != -1 && endIdx > idx + 1 && text[idx + 1] != ' ') {
                    flushPlain()
                    tokens.add(
                        InlineMarkdownToken.Text(
                            content = text.substring(idx + 1, endIdx),
                            italic = true
                        )
                    )
                    idx = endIdx + 1
                    continue
                }
            }

            // 7. Italic _..._ only at word boundaries so snake_case is preserved
            if (text[idx] == '_' && !text.startsWith("__", idx)) {
                val prevChar = if (idx > 0) text[idx - 1] else ' '
                val isStartBoundary = !prevChar.isLetterOrDigit()
                if (isStartBoundary) {
                    val endIdx = text.indexOf('_', idx + 1)
                    if (endIdx != -1 && endIdx > idx + 1) {
                        val afterChar = if (endIdx + 1 < text.length) text[endIdx + 1] else ' '
                        if (!afterChar.isLetterOrDigit() && text[idx + 1] != ' ') {
                            flushPlain()
                            tokens.add(
                                InlineMarkdownToken.Text(
                                    content = text.substring(idx + 1, endIdx),
                                    italic = true
                                )
                            )
                            idx = endIdx + 1
                            continue
                        }
                    }
                }
            }

            plainBuffer.append(text[idx])
            idx++
        }

        flushPlain()
        return tokens
    }
}
