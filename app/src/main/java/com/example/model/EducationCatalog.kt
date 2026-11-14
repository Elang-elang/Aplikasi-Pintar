package com.example.model

import com.example.viewmodel.StudyExplainMode

enum class SchoolLevel(
    val displayName: String,
    val shortLabel: String,
    val classes: List<String>,
    val subjects: List<String>
) {
    SD(
        displayName = "SD",
        shortLabel = "SD",
        classes = listOf("1", "2", "3", "4", "5", "6"),
        subjects = listOf(
            "Umum",
            "Matematika",
            "Bahasa Indonesia",
            "IPAS (IPA & IPS)",
            "Pendidikan Pancasila",
            "Bahasa Inggris",
            "Seni Budaya"
        )
    ),
    SMP(
        displayName = "SMP",
        shortLabel = "SMP",
        classes = listOf("7", "8", "9"),
        subjects = listOf(
            "Umum",
            "Matematika",
            "Bahasa Indonesia",
            "Bahasa Inggris",
            "IPA (Biologi & Fisika)",
            "IPS (Sejarah, Geografi, Ekonomi)",
            "Pendidikan Pancasila",
            "Informatika"
        )
    ),
    SMA(
        displayName = "SMA",
        shortLabel = "SMA",
        classes = listOf("10", "11", "12"),
        subjects = listOf(
            "Umum",
            "Matematika",
            "Bahasa Indonesia",
            "Bahasa Inggris",
            "Fisika",
            "Kimia",
            "Biologi",
            "Ekonomi",
            "Sejarah",
            "Geografi",
            "Sosiologi",
            "Informatika"
        )
    ),
    SMK(
        displayName = "SMK",
        shortLabel = "SMK",
        classes = listOf("10", "11", "12"),
        subjects = listOf(
            "Umum",
            "Teknik Komputer & Jaringan (TKJ)",
            "Rekayasa Perangkat Lunak (RPL)",
            "Multimedia / DKV",
            "Akuntansi & Keuangan Lembaga",
            "Manajemen Perkantoran",
            "Bisnis Daring & Pemasaran",
            "Teknik Kendaraan Ringan (Otomotif)",
            "Teknik Elektronika / Listrik",
            "Tata Boga / Perhotelan",
            "Matematika Kejuruan",
            "Bahasa Inggris Kejuruan"
        )
    );

    companion object {
        fun fromDisplayName(value: String?): SchoolLevel? {
            val clean = value?.trim()?.uppercase() ?: return null
            return entries.firstOrNull {
                it.displayName == clean ||
                    it.shortLabel == clean ||
                    it.name == clean ||
                    clean.startsWith("${it.shortLabel}/")
            }
        }
    }
}

data class RegistrationValidationResult(
    val namaError: String? = null,
    val sekolahError: String? = null,
    val kelasError: String? = null,
    val bidangError: String? = null
) {
    val isValid: Boolean
        get() = namaError == null && sekolahError == null && kelasError == null && bidangError == null
}

data class StudySuggestion(
    val title: String,
    val prompt: String,
    val mode: StudyExplainMode
)

object EducationCatalog {

    fun classesFor(level: SchoolLevel?): List<String> = level?.classes ?: emptyList()

    fun subjectsFor(level: SchoolLevel?): List<String> = level?.subjects ?: emptyList()

    fun validateRegistration(
        nama: String,
        sekolah: SchoolLevel?,
        kelas: String?,
        bidang: String?
    ): RegistrationValidationResult {
        val cleanName = nama.trim()
        val namaError = when {
            cleanName.isEmpty() -> "Nama lengkap wajib diisi."
            cleanName.length < 2 -> "Nama lengkap minimal 2 huruf."
            else -> null
        }

        val sekolahError = if (sekolah == null) {
            "Pilih jenjang sekolah terlebih dahulu."
        } else null

        val validClasses = classesFor(sekolah)
        val kelasError = when {
            sekolah == null -> "Pilih jenjang sekolah agar pilihan kelas tampil."
            kelas.isNullOrBlank() || kelas !in validClasses -> "Pilih kelas yang sesuai jenjang ${sekolah.displayName}."
            else -> null
        }

        val validSubjects = subjectsFor(sekolah)
        val bidangError = when {
            sekolah == null -> "Pilih jenjang sekolah terlebih dahulu."
            bidang.isNullOrBlank() || bidang !in validSubjects -> "Pilih bidang mata pelajaran atau Umum."
            else -> null
        }

        return RegistrationValidationResult(
            namaError = namaError,
            sekolahError = sekolahError,
            kelasError = kelasError,
            bidangError = bidangError
        )
    }

    fun formatProfileBadge(sekolah: String?, kelas: String?, bidang: String?): String {
        val level = SchoolLevel.fromDisplayName(sekolah)
        val levelLabel = level?.shortLabel ?: sekolah?.takeIf { it.isNotBlank() } ?: "Pelajar"
        val classPart = kelas?.takeIf { it.isNotBlank() }?.let { "$it $levelLabel" } ?: levelLabel
        val subjectPart = bidang?.takeIf { it.isNotBlank() }
        return listOfNotNull(classPart, subjectPart).joinToString(" • ")
    }

    fun tutorDepthGuidance(sekolah: String?, kelas: String?, bidang: String?): String {
        val level = SchoolLevel.fromDisplayName(sekolah)
        val levelNote = when (level) {
            SchoolLevel.SD -> "Gunakan bahasa Indonesia yang sederhana, hangat, dan ramah anak SD kelas ${kelas ?: "1-6"}. Berikan contoh nyata dari aktivitas sehari-hari."
            SchoolLevel.SMP -> "Gunakan bahasa komunikatif setingkat SMP kelas ${kelas ?: "7-9"}. Jelaskan konsep secara runtut dengan langkah penyelesaian bertahap."
            SchoolLevel.SMA -> "Gunakan bahasa akademis yang jelas dan terstruktur setingkat SMA kelas ${kelas ?: "10-12"}. Sertakan analisis konsep, rumus, atau argumen pendukung yang akurat."
            SchoolLevel.SMK -> "Gunakan pendekatan vokasional dan terapan setingkat SMK kelas ${kelas ?: "10-12"}. Hubungkan teori dengan praktik kerja nyata, studi kasus kejuruan, dan langkah teknis yang jelas."
            null -> "Gunakan bahasa Indonesia yang runtut, jelas, dan mudah dipahami."
        }
        val subjectNote = if (bidang.isNullOrBlank() || bidang.equals("Umum", ignoreCase = true)) {
            "Pengguna memilih bidang Umum (semua mata pelajaran), sesuaikan topik dengan pertanyaan atau dokumen yang dilampirkan."
        } else {
            "Bidang pelajaran pengguna saat ini adalah $bidang; prioritaskan sudut pandang dan contoh yang relevan dengan $bidang bila sesuai konteks."
        }
        return "$levelNote $subjectNote"
    }

    fun starterSuggestions(sekolah: String?, kelas: String?, bidang: String?): List<StudySuggestion> {
        val level = SchoolLevel.fromDisplayName(sekolah)
        val activeBidang = bidang?.trim().orEmpty()
        val classNum = kelas?.trim().orEmpty().ifBlank {
            when (level) {
                SchoolLevel.SD -> "4"
                SchoolLevel.SMP -> "8"
                SchoolLevel.SMA, SchoolLevel.SMK -> "11"
                null -> ""
            }
        }

        if (activeBidang.isNotEmpty() && !activeBidang.equals("Umum", ignoreCase = true)) {
            return listOf(
                StudySuggestion(
                    title = "Konsep Inti $activeBidang",
                    prompt = "Jelaskan konsep dasar $activeBidang untuk kelas $classNum ${level?.shortLabel ?: ""} dengan langkah yang mudah dipahami.",
                    mode = StudyExplainMode.EXPLAIN_SIMPLE
                ),
                StudySuggestion(
                    title = "Ringkasan Bab Penting",
                    prompt = "Buatkan ringkasan materi utama $activeBidang kelas $classNum ${level?.shortLabel ?: ""} beserta istilah pentingnya.",
                    mode = StudyExplainMode.SUMMARY
                ),
                StudySuggestion(
                    title = "Contoh & Latihan Singkat",
                    prompt = "Berikan 3 contoh soal $activeBidang kelas $classNum ${level?.shortLabel ?: ""} lengkap dengan cara mengerjakannya.",
                    mode = StudyExplainMode.EXAMPLES
                )
            )
        }

        return when (level) {
            SchoolLevel.SD -> listOf(
                StudySuggestion(
                    title = "Operasi Hitung & Pecahan",
                    prompt = "Jelaskan cara mudah membandingkan dan menjumlahkan pecahan untuk kelas $classNum SD dengan contoh sehari-hari.",
                    mode = StudyExplainMode.EXPLAIN_SIMPLE
                ),
                StudySuggestion(
                    title = "Ringkasan IPAS: Daur Air",
                    prompt = "Buatkan ringkasan sederhana tentang tahapan daur air dan manfaatnya bagi makhluk hidup.",
                    mode = StudyExplainMode.SUMMARY
                ),
                StudySuggestion(
                    title = "Latihan Kalimat Utama",
                    prompt = "Berikan 3 contoh paragraf singkat kelas $classNum SD lalu ajari cara menemukan ide pokoknya.",
                    mode = StudyExplainMode.EXAMPLES
                )
            )
            SchoolLevel.SMP -> listOf(
                StudySuggestion(
                    title = "Konsep Sistem Pencernaan",
                    prompt = "Jelaskan urutan proses pencernaan makanan beserta enzim utamanya untuk kelas $classNum SMP.",
                    mode = StudyExplainMode.EXPLAIN_SIMPLE
                ),
                StudySuggestion(
                    title = "Ringkasan Teks Eksplanasi",
                    prompt = "Buatkan ringkasan struktur dan ciri kebahasaan teks eksplanasi untuk belajar kelas $classNum SMP.",
                    mode = StudyExplainMode.SUMMARY
                ),
                StudySuggestion(
                    title = "Contoh Aljabar & Persamaan",
                    prompt = "Berikan contoh penyelesaian persamaan linear satu variabel langkah demi langkah untuk kelas $classNum SMP.",
                    mode = StudyExplainMode.EXAMPLES
                )
            )
            SchoolLevel.SMK -> listOf(
                StudySuggestion(
                    title = "Konsep Praktik Kejuruan",
                    prompt = "Jelaskan alur kerja praktik dasar dan standar keselamatan kerja (K3LH) untuk siswa kelas $classNum SMK.",
                    mode = StudyExplainMode.EXPLAIN_SIMPLE
                ),
                StudySuggestion(
                    title = "Ringkasan Persiapan UKK",
                    prompt = "Buatkan ringkasan poin penting persiapan Uji Kompetensi Keahlian (UKK) untuk kelas $classNum SMK.",
                    mode = StudyExplainMode.SUMMARY
                ),
                StudySuggestion(
                    title = "Studi Kasus Lapangan",
                    prompt = "Berikan contoh studi kasus pemecahan masalah kejuruan beserta langkah penyelesaiannya untuk kelas $classNum SMK.",
                    mode = StudyExplainMode.EXAMPLES
                )
            )
            SchoolLevel.SMA, null -> listOf(
                StudySuggestion(
                    title = "Analisis Hukum Newton",
                    prompt = "Jelaskan perbedaan Hukum I, II, dan III Newton beserta penerapannya pada benda di bidang datar untuk kelas $classNum SMA.",
                    mode = StudyExplainMode.EXPLAIN_SIMPLE
                ),
                StudySuggestion(
                    title = "Ringkasan Ekonomi Makro",
                    prompt = "Buatkan ringkasan terstruktur tentang inflasi, kebijakan moneter, dan dampaknya terhadap daya beli.",
                    mode = StudyExplainMode.SUMMARY
                ),
                StudySuggestion(
                    title = "Latihan Soal Kritis",
                    prompt = "Berikan contoh soal pemahaman konsep dan pembahasan ringkas sesuai materi kelas $classNum SMA.",
                    mode = StudyExplainMode.EXAMPLES
                )
            )
        }
    }
}
