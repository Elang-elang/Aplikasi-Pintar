package com.example

import com.example.model.EducationCatalog
import com.example.model.SchoolLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationCatalogTest {

    @Test
    fun `school levels strictly contain SD SMP SMA SMK`() {
        assertEquals(
            listOf("SD", "SMP", "SMA", "SMK"),
            SchoolLevel.entries.map { it.displayName }
        )
    }

    @Test
    fun `classes depend strictly on selected school level`() {
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), EducationCatalog.classesFor(SchoolLevel.SD))
        assertEquals(listOf("7", "8", "9"), EducationCatalog.classesFor(SchoolLevel.SMP))
        assertEquals(listOf("10", "11", "12"), EducationCatalog.classesFor(SchoolLevel.SMA))
        assertEquals(listOf("10", "11", "12"), EducationCatalog.classesFor(SchoolLevel.SMK))
        assertTrue(EducationCatalog.classesFor(null).isEmpty())
    }

    @Test
    fun `subjects always start with Umum and SMK uses vocational majors`() {
        SchoolLevel.entries.forEach { level ->
            val subjects = EducationCatalog.subjectsFor(level)
            assertEquals("Umum", subjects.first())
            assertTrue(subjects.size > 3)
        }
        assertTrue(EducationCatalog.subjectsFor(SchoolLevel.SMA).contains("Fisika"))
        assertTrue(EducationCatalog.subjectsFor(SchoolLevel.SMK).contains("Rekayasa Perangkat Lunak (RPL)"))
        assertTrue(EducationCatalog.subjectsFor(SchoolLevel.SMK).contains("Teknik Komputer & Jaringan (TKJ)"))
        assertFalse(EducationCatalog.subjectsFor(SchoolLevel.SMK).contains("Sosiologi"))
    }

    @Test
    fun `fromDisplayName supports both short and legacy names`() {
        assertEquals(SchoolLevel.SD, SchoolLevel.fromDisplayName("SD"))
        assertEquals(SchoolLevel.SD, SchoolLevel.fromDisplayName("SD/SEDERAJAT"))
        assertEquals(SchoolLevel.SMP, SchoolLevel.fromDisplayName("SMP/SEDERAJAT"))
        assertEquals(SchoolLevel.SMA, SchoolLevel.fromDisplayName("SMA"))
        assertEquals(SchoolLevel.SMK, SchoolLevel.fromDisplayName("SMK"))
    }

    @Test
    fun `registration validation rejects empty fields and mismatched class`() {
        val missingAll = EducationCatalog.validateRegistration("", null, null, null)
        assertFalse(missingAll.isValid)
        assertNotNull(missingAll.namaError)
        assertNotNull(missingAll.sekolahError)
        assertNotNull(missingAll.kelasError)
        assertNotNull(missingAll.bidangError)

        val shortName = EducationCatalog.validateRegistration("A", SchoolLevel.SMP, "8", "Umum")
        assertFalse(shortName.isValid)
        assertNotNull(shortName.namaError)
        assertNull(shortName.sekolahError)
        assertNull(shortName.kelasError)
        assertNull(shortName.bidangError)

        val mismatchedClass = EducationCatalog.validateRegistration("Budi", SchoolLevel.SMP, "10", "Umum")
        assertFalse(mismatchedClass.isValid)
        assertNotNull(mismatchedClass.kelasError)

        val validSmk = EducationCatalog.validateRegistration(
            "Rina",
            SchoolLevel.SMK,
            "11",
            "Rekayasa Perangkat Lunak (RPL)"
        )
        assertTrue(validSmk.isValid)
    }

    @Test
    fun `tutor depth guidance and starter suggestions adapt to SD SMP SMA SMK`() {
        val sdGuidance = EducationCatalog.tutorDepthGuidance("SD", "4", "Matematika")
        assertTrue(sdGuidance.contains("SD"))
        assertTrue(sdGuidance.contains("Matematika"))

        val smkGuidance = EducationCatalog.tutorDepthGuidance("SMK", "12", "Teknik Komputer & Jaringan (TKJ)")
        assertTrue(smkGuidance.contains("SMK"))
        assertTrue(smkGuidance.contains("vokasional"))

        val smaSuggestions = EducationCatalog.starterSuggestions("SMA", "11", "Fisika")
        assertEquals(3, smaSuggestions.size)
        assertTrue(smaSuggestions.first().prompt.contains("Fisika"))
    }
}
