package com.example.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val id: Int = 1,
    val nama: String,
    val kelas: String,
    val absen: String,
    val isLogin: Boolean = false
)

@Entity(tableName = "saved_exams")
data class SavedExam(
    @PrimaryKey val id: Int = 1,
    val kisiKisi: String,
    val questionsJson: String, // Combined JSON containing "pilihan_ganda" and "esai"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_attempts")
data class ExamAttempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val studentName: String,
    val studentClass: String,
    val studentAbsen: String,
    val pgCorrectCount: Int,
    val pgTotalCount: Int,
    val pgScore: Int,
    val essayScore: Int,
    val finalScore: Int,
    val answersJson: String, // Raw quiz responses json
    val gradingJson: String, // Essay analysis feedbacks json
    val emailSentStatus: String = "PENDING", // "SUCCESS", "FAILED", "NOT_CONFIGURED"
    val questionsJson: String = "", // Fully serialized ExamQuestions list
    val kisiKisi: String = "" // Materials submitted
)

@Dao
interface ExamDao {
    @Query("SELECT * FROM user_sessions WHERE id = 1")
    suspend fun getUserSession(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSession(session: UserSession)

    @Query("DELETE FROM user_sessions")
    suspend fun clearUserSession()

    @Query("SELECT * FROM saved_exams WHERE id = 1")
    suspend fun getSavedExam(): SavedExam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExam(exam: SavedExam)

    @Query("DELETE FROM saved_exams")
    suspend fun clearSavedExam()

    @Query("SELECT * FROM exam_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<ExamAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttempt(attempt: ExamAttempt): Long

    @Query("SELECT * FROM exam_attempts WHERE id = :id LIMIT 1")
    suspend fun getAttemptById(id: Int): ExamAttempt?
}

@Database(entities = [UserSession::class, SavedExam::class, ExamAttempt::class], version = 2, exportSchema = false)
abstract class ExamDatabase : RoomDatabase() {
    abstract val examDao: ExamDao

    companion object {
        @Volatile
        private var INSTANCE: ExamDatabase? = null

        fun getDatabase(context: Context): ExamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamDatabase::class.java,
                    "exam_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
