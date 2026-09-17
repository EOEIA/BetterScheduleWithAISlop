package cz.vitskalicky.lepsirozvrh.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * The user ticking an assignment off locally.
 *
 * Bakaláři reports its own `Done` flag per homework, but there is no write endpoint we can rely on,
 * so a row here is an explicit local override of that flag - present means "the user decided",
 * absent means "whatever the API says".
 */
@Entity(tableName = "homework_done", primaryKeys = ["accountId", "homeworkId"])
data class HomeworkDone(
    val accountId: Long,
    val homeworkId: String,
    @ColumnInfo(name = "is_done") val isDone: Boolean
)

@Dao
interface HomeworkDoneDao {
    @Query("SELECT * FROM homework_done WHERE accountId = :accountId")
    fun getAllForAccount(accountId: Long): LiveData<List<HomeworkDone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: HomeworkDone)
}
