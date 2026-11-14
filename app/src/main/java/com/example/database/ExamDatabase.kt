package com.example.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val id: Int = 1,
    val nama: String,
    val sekolah: String,
    val kelas: String,
    val bidang: String,
    val isLogin: Boolean = false
)

@Entity(tableName = "saved_exams")
data class SavedExam(
    @PrimaryKey val id: Int = 1,
    val kisiKisi: String,
    val questionsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_attempts")
data class ExamAttempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val studentName: String,
    val studentClass: String,
    val studentAbsen: String = "",
    val pgCorrectCount: Int,
    val pgTotalCount: Int,
    val pgScore: Int,
    val essayScore: Int,
    val finalScore: Int,
    val answersJson: String,
    val gradingJson: String,
    val emailSentStatus: String = "LOCAL",
    val questionsJson: String = "",
    val kisiKisi: String = ""
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

@Database(entities = [UserSession::class, SavedExam::class, ExamAttempt::class], version = 3, exportSchema = false)
abstract class ExamDatabase : RoomDatabase() {
    abstract val examDao: ExamDao

    companion object {
        @Volatile
        private var INSTANCE: ExamDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild user_sessions with structured sekolah, kelas, bidang (without absen)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_sessions_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        nama TEXT NOT NULL,
                        sekolah TEXT NOT NULL,
                        kelas TEXT NOT NULL,
                        bidang TEXT NOT NULL,
                        isLogin INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS user_sessions")
                db.execSQL("ALTER TABLE user_sessions_new RENAME TO user_sessions")
            }
        }

        fun getDatabase(context: Context): ExamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamDatabase::class.java,
                    "exam_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
