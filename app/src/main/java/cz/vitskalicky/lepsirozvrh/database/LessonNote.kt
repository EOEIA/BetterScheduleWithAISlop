package cz.vitskalicky.lepsirozvrh.database

import androidx.lifecycle.LiveData
import androidx.room.*
import org.joda.time.LocalDate
import org.joda.time.LocalTime

@Entity(tableName = "lesson_note", primaryKeys = ["accountId", "lessonKey"])
data class LessonNote(
    val accountId: Long,
    val lessonKey: String,
    val text: String
)

@Dao
interface LessonNoteDao {
    @Query("SELECT * FROM lesson_note WHERE accountId = :accountId")
    fun getAllForAccount(accountId: Long): LiveData<List<LessonNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: LessonNote)

    @Query("DELETE FROM lesson_note WHERE accountId = :accountId AND lessonKey = :lessonKey")
    suspend fun delete(accountId: Long, lessonKey: String)
}

fun lessonNoteKey(date: LocalDate, beginTime: LocalTime): String =
    "${LocalDateConverters.fromLocalDate(date)}|${beginTime.millisOfDay}"
