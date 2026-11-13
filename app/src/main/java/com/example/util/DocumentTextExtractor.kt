package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class ExtractedDocument(
    val fileName: String,
    val fileSizeBytes: Long,
    val formatLabel: String,
    val extractedText: String,
    val t2tPayload: String,
    val pageOrSectionCount: Int
)

sealed interface DocumentExtractionResult {
    data class Success(val document: ExtractedDocument) : DocumentExtractionResult
    data class Error(val message: String) : DocumentExtractionResult
}

object DocumentTextExtractor {
    const val MAX_FILE_SIZE_BYTES: Long = 5L * 1024L * 1024L // Strictly under 5 MB
    const val MAX_EXTRACTED_CHARS: Int = 50_000

    val SUPPORTED_MIME_TYPES = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/msword",
        "application/vnd.ms-powerpoint",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.presentation",
        "application/vnd.oasis.opendocument.spreadsheet",
        "application/rtf",
        "text/*",
        "application/json",
        "application/xml",
        "application/octet-stream"
    )

    @Volatile
    private var pdfBoxInitialized = false

    private fun ensurePdfBoxInitialized(context: Context) {
        if (!pdfBoxInitialized) {
            synchronized(this) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxInitialized = true
                }
            }
        }
    }

    suspend fun extractFromUri(context: Context, uri: Uri): DocumentExtractionResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var displayName = "dokumen"
            var reportedSize = -1L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
                        reportedSize = cursor.getLong(sizeIdx)
                    }
                }
            }

            if (reportedSize >= MAX_FILE_SIZE_BYTES) {
                val mb = String.format(Locale.US, "%.2f", reportedSize / (1024.0 * 1024.0))
                return@withContext DocumentExtractionResult.Error(
                    "Ukuran file \"$displayName\" ($mb MB) melebihi batas. File harus di bawah 5 MB."
                )
            }

            val stream = contentResolver.openInputStream(uri)
                ?: return@withContext DocumentExtractionResult.Error(
                    "Tidak dapat membaca isi file \"$displayName\"."
                )

            val bytes = stream.use { input ->
                readBytesStrictlyUnderLimit(input, MAX_FILE_SIZE_BYTES)
            } ?: return@withContext DocumentExtractionResult.Error(
                "Ukuran file \"$displayName\" mencapai atau melebihi 5 MB. Batas maksimal adalah di bawah 5 MB."
            )

            if (bytes.isEmpty()) {
                return@withContext DocumentExtractionResult.Error(
                    "File \"$displayName\" kosong (0 byte)."
                )
            }

            val mimeType = contentResolver.getType(uri)
            extractFromBytes(
                bytes = bytes,
                fileName = displayName,
                mimeType = mimeType,
                pdfExtractor = { pdfBytes -> extractPdfWithPdfBox(context, pdfBytes) }
            )
        } catch (e: Exception) {
            DocumentExtractionResult.Error(
                "Gagal memproses dokumen: ${e.message ?: "Kesalahan tidak diketahui"}"
            )
        }
    }

    /**
     * Reads stream up to [exclusiveMaxBytes] - 1. Returns null if stream reaches or exceeds [exclusiveMaxBytes].
     */
    internal fun readBytesStrictlyUnderLimit(input: InputStream, exclusiveMaxBytes: Long): ByteArray? {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total >= exclusiveMaxBytes) {
                return null
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    internal fun extractFromBytes(
        bytes: ByteArray,
        fileName: String,
        mimeType: String? = null,
        pdfExtractor: ((ByteArray) -> Pair<String, Int>)? = null
    ): DocumentExtractionResult {
        if (bytes.size.toLong() >= MAX_FILE_SIZE_BYTES) {
            val mb = String.format(Locale.US, "%.2f", bytes.size / (1024.0 * 1024.0))
            return DocumentExtractionResult.Error(
                "Ukuran file \"$fileName\" ($mb MB) melebihi batas. File harus di bawah 5 MB."
            )
        }
        if (bytes.isEmpty()) {
            return DocumentExtractionResult.Error("File \"$fileName\" kosong.")
        }

        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val isPdfSignature = bytes.size >= 5 &&
            bytes[0] == '%'.code.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()
        val isZipSignature = bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()

        val format = detectFormat(ext, mimeType, isPdfSignature, isZipSignature, bytes)
            ?: return DocumentExtractionResult.Error(
                "Format file \"$fileName\" belum didukung. Gunakan PDF, DOCX, PPTX, XLSX, ODT, ODP, TXT, MD, CSV, HTML, atau RTF."
            )

        val (rawText, sectionCount) = try {
            when (format) {
                "PDF" -> {
                    if (pdfExtractor != null) {
                        pdfExtractor(bytes)
                    } else {
                        return DocumentExtractionResult.Error("Ekstraktor PDF tidak tersedia di konteks ini.")
                    }
                }
                "DOCX" -> extractDocx(bytes)
                "PPTX" -> extractPptx(bytes)
                "XLSX" -> extractXlsx(bytes)
                "ODT", "ODP", "ODS" -> extractOpenDocument(bytes)
                "DOC" -> extractLegacyDocText(bytes)
                "HTML" -> Pair(stripHtml(String(bytes, StandardCharsets.UTF_8)), 1)
                "RTF" -> Pair(stripRtf(String(bytes, StandardCharsets.UTF_8)), 1)
                else -> Pair(String(bytes, StandardCharsets.UTF_8), 1)
            }
        } catch (e: Exception) {
            return DocumentExtractionResult.Error(
                "Gagal mengekstrak teks dari $fileName ($format): ${e.message ?: "File rusak atau terenkripsi"}"
            )
        }

        val normalized = normalizeTextForT2T(rawText)
        if (normalized.isBlank()) {
            val extraHint = if (format == "PDF") {
                " Kemungkinan PDF hanya berisi gambar hasil scan tanpa lapisan teks (OCR diperlukan untuk model AI berbasis teks)."
            } else {
                ""
            }
            return DocumentExtractionResult.Error(
                "Tidak ditemukan teks yang dapat dibaca pada \"$fileName\".$extraHint"
            )
        }

        val finalContent = if (normalized.length > MAX_EXTRACTED_CHARS) {
            normalized.take(MAX_EXTRACTED_CHARS) +
                "\n\n[...Sebagian isi dokumen dipotong karena melebihi batas $MAX_EXTRACTED_CHARS karakter untuk input T2T AI...]"
        } else {
            normalized
        }

        val t2tPayload = buildString {
            append("[DOKUMEN KISI-KISI: ").append(fileName)
            append(" | Format: ").append(format)
            if (sectionCount > 1) {
                append(" | Bagian/Halaman: ").append(sectionCount)
            }
            append("]\n")
            append(finalContent)
        }

        return DocumentExtractionResult.Success(
            ExtractedDocument(
                fileName = fileName,
                fileSizeBytes = bytes.size.toLong(),
                formatLabel = format,
                extractedText = finalContent,
                t2tPayload = t2tPayload,
                pageOrSectionCount = sectionCount.coerceAtLeast(1)
            )
        )
    }

    private fun detectFormat(
        ext: String,
        mimeType: String?,
        isPdfSignature: Boolean,
        isZipSignature: Boolean,
        bytes: ByteArray
    ): String? {
        return when {
            ext == "pdf" || mimeType == "application/pdf" || isPdfSignature -> "PDF"
            ext == "docx" || mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "DOCX"
            ext == "pptx" || mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "PPTX"
            ext == "xlsx" || mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "XLSX"
            ext == "odt" || mimeType == "application/vnd.oasis.opendocument.text" -> "ODT"
            ext == "odp" || mimeType == "application/vnd.oasis.opendocument.presentation" -> "ODP"
            ext == "ods" || mimeType == "application/vnd.oasis.opendocument.spreadsheet" -> "ODS"
            ext == "doc" || mimeType == "application/msword" -> "DOC"
            ext == "html" || ext == "htm" || mimeType == "text/html" -> "HTML"
            ext == "rtf" || mimeType == "application/rtf" || mimeType == "text/rtf" -> "RTF"
            ext == "txt" || mimeType == "text/plain" -> "TXT"
            ext == "md" || ext == "markdown" || mimeType == "text/markdown" -> "MD"
            ext == "csv" || mimeType == "text/csv" || mimeType == "text/comma-separated-values" -> "CSV"
            ext == "json" || mimeType == "application/json" -> "JSON"
            ext == "xml" || mimeType == "text/xml" || mimeType == "application/xml" -> "XML"
            isZipSignature -> detectOfficeFormatFromZipEntries(bytes)
            mimeType?.startsWith("text/") == true -> "TXT"
            else -> null
        }
    }

    private fun detectOfficeFormatFromZipEntries(bytes: ByteArray): String? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == "word/document.xml" -> return "DOCX"
                    name.startsWith("ppt/slides/slide") -> return "PPTX"
                    name.startsWith("xl/worksheets/sheet") -> return "XLSX"
                    name == "content.xml" -> return "ODT"
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    private fun extractPdfWithPdfBox(context: Context, bytes: ByteArray): Pair<String, Int> {
        ensurePdfBoxInitialized(context)
        PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            if (document.isEncrypted) {
                throw IllegalStateException("PDF terenkripsi / dilindungi kata sandi")
            }
            val totalPages = document.numberOfPages
            val stripper = PDFTextStripper()
            val sb = StringBuilder()
            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                val pageText = stripper.getText(document).trim()
                if (pageText.isNotBlank()) {
                    if (totalPages > 1) {
                        sb.append("[Halaman ").append(page).append("]\n")
                    }
                    sb.append(pageText).append("\n\n")
                }
            }
            return Pair(sb.toString(), totalPages)
        }
    }

    private fun newDomDocument(xmlBytes: ByteArray): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            try {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Exception) {
            }
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
    }

    private fun readZipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(4096)
                    while (true) {
                        val r = zis.read(buf)
                        if (r == -1) break
                        out.write(buf, 0, r)
                    }
                    entries[entry.name] = out.toByteArray()
                }
                entry = zis.nextEntry
            }
        }
        return entries
    }

    private fun extractDocx(bytes: ByteArray): Pair<String, Int> {
        val entries = readZipEntries(bytes)
        val docXml = entries["word/document.xml"]
            ?: throw IllegalStateException("Struktur DOCX tidak valid (word/document.xml tidak ditemukan)")
        val doc = newDomDocument(docXml)
        val sb = StringBuilder()
        var sectionCount = 0

        val bodyNodes = doc.getElementsByTagNameNS("*", "body")
        val root: Node = if (bodyNodes.length > 0) bodyNodes.item(0) else doc.documentElement
        val children = root.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType != Node.ELEMENT_NODE) continue
            val el = child as Element
            when (el.localName) {
                "p" -> {
                    val line = extractDocxParagraph(el)
                    if (line.isNotBlank()) {
                        if (line.startsWith("#")) sectionCount++
                        sb.append(line).append("\n")
                    }
                }
                "tbl" -> {
                    val tableText = extractDocxTable(el)
                    if (tableText.isNotBlank()) {
                        sb.append(tableText).append("\n")
                    }
                }
            }
        }
        return Pair(sb.toString(), sectionCount.coerceAtLeast(1))
    }

    private fun getDomAttribute(el: Element, name: String): String {
        val attrs = el.attributes
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            if (attr.localName == name || attr.nodeName == name || attr.nodeName.endsWith(":$name")) {
                return attr.nodeValue ?: ""
            }
        }
        return ""
    }

    private fun extractDocxParagraph(pElement: Element): String {
        var headingPrefix = ""
        val pStyleNodes = pElement.getElementsByTagNameNS("*", "pStyle")
        if (pStyleNodes.length > 0) {
            val styleEl = pStyleNodes.item(0) as? Element
            val styleAttr = if (styleEl != null) getDomAttribute(styleEl, "val") else ""
            if (styleAttr.contains("Heading1", ignoreCase = true) || styleAttr == "1") headingPrefix = "# "
            else if (styleAttr.contains("Heading2", ignoreCase = true) || styleAttr == "2") headingPrefix = "## "
            else if (styleAttr.contains("Heading", ignoreCase = true)) headingPrefix = "### "
        }

        val numPrNodes = pElement.getElementsByTagNameNS("*", "numPr")
        val bulletPrefix = if (numPrNodes.length > 0 && headingPrefix.isEmpty()) "- " else ""

        val textSb = StringBuilder()
        collectDocxRunText(pElement, textSb)
        val clean = textSb.toString().trim()
        return if (clean.isEmpty()) "" else "$headingPrefix$bulletPrefix$clean"
    }

    private fun collectDocxRunText(node: Node, sb: StringBuilder) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val el = node as Element
            when (el.localName) {
                "t" -> sb.append(el.textContent ?: "")
                "tab" -> sb.append("\t")
                "br", "cr" -> sb.append("\n")
                else -> {
                    val children = el.childNodes
                    for (i in 0 until children.length) {
                        collectDocxRunText(children.item(i), sb)
                    }
                }
            }
        }
    }

    private fun extractDocxTable(tblElement: Element): String {
        val sb = StringBuilder()
        val rows = tblElement.getElementsByTagNameNS("*", "tr")
        for (r in 0 until rows.length) {
            val rowEl = rows.item(r) as? Element ?: continue
            val cells = rowEl.getElementsByTagNameNS("*", "tc")
            val rowValues = mutableListOf<String>()
            for (c in 0 until cells.length) {
                val cellEl = cells.item(c) as? Element ?: continue
                val cellParagraphs = cellEl.getElementsByTagNameNS("*", "p")
                val cellParts = mutableListOf<String>()
                for (p in 0 until cellParagraphs.length) {
                    val pText = extractDocxParagraph(cellParagraphs.item(p) as Element)
                    if (pText.isNotBlank()) cellParts.add(pText)
                }
                rowValues.add(cellParts.joinToString(" ").replace("\n", " "))
            }
            if (rowValues.any { it.isNotBlank() }) {
                sb.append("| ").append(rowValues.joinToString(" | ")).append(" |\n")
            }
        }
        return sb.toString()
    }

    private fun extractPptx(bytes: ByteArray): Pair<String, Int> {
        val entries = readZipEntries(bytes)
        val slideRegex = Regex("^ppt/slides/slide(\\d+)\\.xml$")
        val slides = entries.keys
            .mapNotNull { key ->
                val match = slideRegex.matchEntire(key)
                match?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { num -> num to key }
            }
            .sortedBy { it.first }

        if (slides.isEmpty()) {
            throw IllegalStateException("Struktur PPTX tidak memiliki slide yang dapat dibaca")
        }

        val sb = StringBuilder()
        for ((slideNum, entryKey) in slides) {
            val slideBytes = entries[entryKey] ?: continue
            val doc = newDomDocument(slideBytes)
            val slideText = extractDrawingMlParagraphs(doc.documentElement)

            val notesKey = "ppt/notesSlides/notesSlide$slideNum.xml"
            val notesText = entries[notesKey]?.let { notesBytes ->
                extractDrawingMlParagraphs(newDomDocument(notesBytes).documentElement)
            }?.takeIf { it.isNotBlank() }

            if (slideText.isNotBlank() || notesText != null) {
                sb.append("[Slide ").append(slideNum).append("]\n")
                if (slideText.isNotBlank()) {
                    sb.append(slideText).append("\n")
                }
                if (notesText != null) {
                    sb.append("[Catatan Slide ").append(slideNum).append("]: ").append(notesText).append("\n")
                }
                sb.append("\n")
            }
        }
        return Pair(sb.toString(), slides.size)
    }

    private fun extractDrawingMlParagraphs(root: Element): String {
        val sb = StringBuilder()
        val paragraphs = root.getElementsByTagNameNS("*", "p")
        for (i in 0 until paragraphs.length) {
            val pEl = paragraphs.item(i) as? Element ?: continue
            val textNodes = pEl.getElementsByTagNameNS("*", "t")
            val lineSb = StringBuilder()
            for (t in 0 until textNodes.length) {
                lineSb.append(textNodes.item(t).textContent ?: "")
            }
            val line = lineSb.toString().trim()
            if (line.isNotBlank()) {
                sb.append(line).append("\n")
            }
        }
        return sb.toString().trim()
    }

    private fun extractXlsx(bytes: ByteArray): Pair<String, Int> {
        val entries = readZipEntries(bytes)
        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { ssBytes ->
            val ssDoc = newDomDocument(ssBytes)
            val siNodes = ssDoc.getElementsByTagNameNS("*", "si")
            for (i in 0 until siNodes.length) {
                val siEl = siNodes.item(i) as? Element ?: continue
                val tNodes = siEl.getElementsByTagNameNS("*", "t")
                val valSb = StringBuilder()
                for (t in 0 until tNodes.length) {
                    valSb.append(tNodes.item(t).textContent ?: "")
                }
                sharedStrings.add(valSb.toString())
            }
        }

        val sheetRegex = Regex("^xl/worksheets/sheet(\\d+)\\.xml$")
        val sheets = entries.keys
            .mapNotNull { key ->
                sheetRegex.matchEntire(key)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { it to key }
            }
            .sortedBy { it.first }

        val sb = StringBuilder()
        for ((sheetNum, key) in sheets) {
            val sheetBytes = entries[key] ?: continue
            val sheetDoc = newDomDocument(sheetBytes)
            val rows = sheetDoc.getElementsByTagNameNS("*", "row")
            val sheetSb = StringBuilder()
            for (r in 0 until rows.length) {
                val rowEl = rows.item(r) as? Element ?: continue
                val cells = rowEl.getElementsByTagNameNS("*", "c")
                val rowCells = mutableListOf<String>()
                for (c in 0 until cells.length) {
                    val cellEl = cells.item(c) as? Element ?: continue
                    val type = cellEl.getAttribute("t")
                    val vNodes = cellEl.getElementsByTagNameNS("*", "v")
                    val rawVal = if (vNodes.length > 0) vNodes.item(0).textContent?.trim() ?: "" else ""
                    val cellValue = when (type) {
                        "s" -> rawVal.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                        "inlineStr" -> {
                            val tNodes = cellEl.getElementsByTagNameNS("*", "t")
                            if (tNodes.length > 0) tNodes.item(0).textContent ?: "" else ""
                        }
                        else -> rawVal
                    }
                    if (cellValue.isNotBlank()) rowCells.add(cellValue.trim())
                }
                if (rowCells.isNotEmpty()) {
                    sheetSb.append("| ").append(rowCells.joinToString(" | ")).append(" |\n")
                }
            }
            if (sheetSb.isNotBlank()) {
                sb.append("[Sheet ").append(sheetNum).append("]\n").append(sheetSb).append("\n")
            }
        }
        return Pair(sb.toString(), sheets.size.coerceAtLeast(1))
    }

    private fun extractOpenDocument(bytes: ByteArray): Pair<String, Int> {
        val entries = readZipEntries(bytes)
        val contentXml = entries["content.xml"]
            ?: throw IllegalStateException("Struktur OpenDocument tidak valid (content.xml tidak ditemukan)")
        val doc = newDomDocument(contentXml)
        val sb = StringBuilder()
        var sectionCount = 0

        fun walk(node: Node) {
            if (node.nodeType == Node.ELEMENT_NODE) {
                val el = node as Element
                if (el.localName == "h" || el.localName == "p") {
                    val text = el.textContent?.trim() ?: ""
                    if (text.isNotBlank()) {
                        if (el.localName == "h") {
                            sectionCount++
                            sb.append("# ").append(text).append("\n")
                        } else {
                            sb.append(text).append("\n")
                        }
                    }
                    return
                }
            }
            val children = node.childNodes
            for (i in 0 until children.length) {
                walk(children.item(i))
            }
        }

        walk(doc.documentElement)
        return Pair(sb.toString(), sectionCount.coerceAtLeast(1))
    }

    private fun extractLegacyDocText(bytes: ByteArray): Pair<String, Int> {
        // Best-effort extraction of printable UTF-16LE and ASCII runs from legacy binary .doc
        val asciiSb = StringBuilder()
        val current = StringBuilder()
        for (b in bytes) {
            val ch = b.toInt() and 0xFF
            if (ch in 32..126 || ch == 10 || ch == 13 || ch == 9) {
                current.append(ch.toChar())
            } else {
                if (current.length >= 6) {
                    asciiSb.append(current).append("\n")
                }
                current.clear()
            }
        }
        if (current.length >= 6) asciiSb.append(current)
        return Pair(asciiSb.toString(), 1)
    }

    internal fun stripHtml(rawHtml: String): String {
        var text = rawHtml
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?i)<(br|p|div|li|tr|h[1-6])[^>]*>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
        text = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        return text
    }

    internal fun stripRtf(rawRtf: String): String {
        val cleaned = rawRtf
            .replace(Regex("(?s)\\{\\\\(fonttbl|colortbl|stylesheet|info|pict|object).*?\\}"), " ")
            .replace("\\par", "\n")
            .replace("\\line", "\n")
            .replace("\\tab", "\t")
            .replace(Regex("\\\\'[0-9a-fA-F]{2}"), "")
            .replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"), "")
            .replace("{", "")
            .replace("}", "")
        return cleaned
    }

    internal fun normalizeTextForT2T(raw: String): String {
        return raw
            .replace("\uFEFF", "")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), " ")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
