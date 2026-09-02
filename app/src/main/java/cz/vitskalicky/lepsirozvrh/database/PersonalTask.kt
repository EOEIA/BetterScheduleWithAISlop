package cz.vitskalicky.lepsirozvrh.database

import androidx.lifecycle.LiveData
import androidx.room.*
import org.joda.time.LocalDate
import org.joda.time.LocalTime

@Entity(tableName = "personal_task")
data class PersonalTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val subject: String = "",
    @ColumnInfo(name = "due_date") val dueDate: LocalDate? = null,
    @ColumnInfo(name = "due_time") val dueTime: LocalTime? = null,
    @ColumnInfo(name = "lesson_key") val lessonKey: String? = null,
    @ColumnInfo(name = "is_done") val isDone: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PersonalTaskDao {
    @Query("SELECT * FROM personal_task WHERE accountId = :accountId ORDER BY is_done ASC, created_at DESC")
    fun getAllForAccount(accountId: Long): LiveData<List<PersonalTask>>

    @Query("SELECT * FROM personal_task WHERE accountId = :accountId AND lesson_key = :lessonKey ORDER BY is_done ASC, created_at DESC")
    fun getForLesson(accountId: Long, lessonKey: String): LiveData<List<PersonalTask>>

    @Insert
    suspend fun insert(task: PersonalTask)

    @Query("UPDATE personal_task SET is_done = :isDone WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean)

    @Query("DELETE FROM personal_task WHERE id = :id")
    suspend fun delete(id: Long)
}
