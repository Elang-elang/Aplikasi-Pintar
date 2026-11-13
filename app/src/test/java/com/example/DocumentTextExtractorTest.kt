package com.example

import com.example.util.DocumentExtractionResult
import com.example.util.DocumentTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentTextExtractorTest {

    private fun createZipBytes(entries: Map<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    @Test
    fun `rejects file at or above 5 MB limit`() {
        val exactFiveMb = ByteArray(5 * 1024 * 1024) { 'A'.code.toByte() }
        val result = DocumentTextExtractor.extractFromBytes(exactFiveMb, "besar.txt", "text/plain")
        assertTrue(result is DocumentExtractionResult.Error)
        val msg = (result as DocumentExtractionResult.Error).message
        assertTrue(msg.contains("5 MB"))

        val streamNull = DocumentTextExtractor.readBytesStrictlyUnderLimit(
            ByteArrayInputStream(exactFiveMb),
            DocumentTextExtractor.MAX_FILE_SIZE_BYTES
        )
        assertNull(streamNull)
    }

    @Test
    fun `extracts docx headings paragraphs and tables into T2T markdown`() {
        val wordXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p>
                  <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
                  <w:r><w:t>Kisi-Kisi Biologi</w:t></w:r>
                </w:p>
                <w:p>
                  <w:r><w:t>Membahas struktur sel dan fotosintesis.</w:t></w:r>
                </w:p>
                <w:tbl>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Topik</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Bobot</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Mitosis</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>30%</w:t></w:r></w:p></w:tc>
                  </w:tr>
                </w:tbl>
              </w:body>
            </w:document>
        """.trimIndent()

        val zipBytes = createZipBytes(mapOf("word/document.xml" to wordXml))
        val res = DocumentTextExtractor.extractFromBytes(zipBytes, "materi.docx")
        assertTrue(res is DocumentExtractionResult.Success)
        val doc = (res as DocumentExtractionResult.Success).document
        assertEquals("DOCX", doc.formatLabel)
        assertTrue(doc.t2tPayload.contains("# Kisi-Kisi Biologi"))
        assertTrue(doc.t2tPayload.contains("Membahas struktur sel dan fotosintesis."))
        assertTrue(doc.t2tPayload.contains("| Topik | Bobot |"))
        assertTrue(doc.t2tPayload.contains("| Mitosis | 30% |"))
    }

    @Test
    fun `extracts pptx slides in numeric order with notes`() {
        val slide1 = """
            <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
              <p:cSld><p:spTree><p:sp><p:txBody><a:p><a:r><a:t>Bab 1: Termodinamika</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld>
            </p:sld>
        """.trimIndent()
        val slide2 = """
            <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
              <p:cSld><p:spTree><p:sp><p:txBody><a:p><a:r><a:t>Bab 2: Hukum Kirchoff</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld>
            </p:sld>
        """.trimIndent()
        val slide10 = """
            <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
              <p:cSld><p:spTree><p:sp><p:txBody><a:p><a:r><a:t>Bab 10: Gelombang</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld>
            </p:sld>
        """.trimIndent()

        val zipBytes = createZipBytes(
            mapOf(
                "ppt/slides/slide10.xml" to slide10,
                "ppt/slides/slide2.xml" to slide2,
                "ppt/slides/slide1.xml" to slide1
            )
        )
        val res = DocumentTextExtractor.extractFromBytes(zipBytes, "fisika.pptx")
        assertTrue(res is DocumentExtractionResult.Success)
        val doc = (res as DocumentExtractionResult.Success).document
        assertEquals("PPTX", doc.formatLabel)
        assertEquals(3, doc.pageOrSectionCount)
        val idx1 = doc.extractedText.indexOf("[Slide 1]")
        val idx2 = doc.extractedText.indexOf("[Slide 2]")
        val idx10 = doc.extractedText.indexOf("[Slide 10]")
        assertTrue(idx1 in 0 until idx2 && idx2 < idx10)
    }

    @Test
    fun `extracts odt and html and rtf cleanly`() {
        val odtContent = """
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:body><office:text>
                <text:h>Sejarah Indonesia</text:h>
                <text:p>Proklamasi Kemerdekaan 1945</text:p>
              </office:text></office:body>
            </office:document-content>
        """.trimIndent()
        val odtBytes = createZipBytes(mapOf("content.xml" to odtContent))
        val odtRes = DocumentTextExtractor.extractFromBytes(odtBytes, "sejarah.odt") as DocumentExtractionResult.Success
        assertTrue(odtRes.document.extractedText.contains("# Sejarah Indonesia"))
        assertTrue(odtRes.document.extractedText.contains("Proklamasi Kemerdekaan 1945"))

        val htmlRes = DocumentTextExtractor.extractFromBytes(
            "<html><body><h1>Pancasila</h1><p>Sila ke-1 &amp; ke-2</p></body></html>".toByteArray(),
            "ppkn.html"
        ) as DocumentExtractionResult.Success
        assertTrue(htmlRes.document.extractedText.contains("Pancasila"))
        assertTrue(htmlRes.document.extractedText.contains("Sila ke-1 & ke-2"))

        val rtfRes = DocumentTextExtractor.extractFromBytes(
            """{\rtf1\ansi{\fonttbl\f0 Arial;}\b Matematika Dasar\par Integral dan Turunan}""".toByteArray(),
            "matematika.rtf"
        ) as DocumentExtractionResult.Success
        assertTrue(rtfRes.document.extractedText.contains("Matematika Dasar"))
        assertTrue(rtfRes.document.extractedText.contains("Integral dan Turunan"))
    }
}
