package cz.vitskalicky.lepsirozvrh.database

import androidx.lifecycle.LiveData
import androidx.room.*
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import org.joda.time.DateTime
import org.joda.time.LocalDate

@Dao
abstract class RozvrhDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRozvrh(vararg rozvrhs: RozvrhRecord)

    @Delete
    abstract suspend fun deleteRozvrh(vararg rozvrhs: RozvrhRecord)

    @Update
    abstract suspend fun updateRozvrh(vararg rozvrhs: RozvrhRecord)

    @Query("SELECT * FROM rozvrh WHERE monday = :monday AND account = :account")
    abstract fun loadRozvrhLive(account: Int, monday: LocalDate): LiveData<RozvrhRecord?>

    fun loadRozvrhLive(key: RozvrhRecord.Key): LiveData<RozvrhRecord?> = loadRozvrhLive(key.account, key.monday)

    @Query("SELECT * FROM rozvrh WHERE monday = :monday AND account = :account")
    abstract suspend fun loadRozvrh(account: Int, monday: LocalDate): RozvrhRecord?
    suspend fun loadRozvrh(key: RozvrhRecord.Key): RozvrhRecord? = loadRozvrh(key.account, key.monday)

    @Query("SELECT lastUpdate < :expireTime FROM rozvrh WHERE monday = :monday AND account = :account")
    abstract suspend fun isExpired(account: Int, monday: LocalDate, expireTime: DateTime): Boolean?
    suspend fun isExpired(key: RozvrhRecord.Key, expireTime: DateTime): Boolean? = isExpired(key.account, key.monday, expireTime)

    /** Works line `touch` command in linux. Sets rozvrh's lastUpdate to the given time (DateTime.now() is default).
     * Used for prolonging validity of certain rozvrh.
     * */
    @Query("UPDATE rozvrh SET lastUpdate = :updateTime WHERE monday = :monday AND account = :account")
    abstract suspend fun setLastUpdate(account: Int, monday: LocalDate, updateTime: DateTime)
    suspend fun setLastUpdate(key: RozvrhRecord.Key, updateTime: DateTime) = setLastUpdate(key.account, key.monday, updateTime)

    /** Sets last update time of given rozvrh to now */
    suspend fun resetExpiration(account: Int, monday: LocalDate) = setLastUpdate(account, monday, DateTime.now())
    suspend fun resetExpiration(key: RozvrhRecord.Key) = setLastUpdate(key, DateTime.now())

    @Query("DELETE FROM Rozvrh WHERE permanent = 0 AND monday < :start OR monday > :end")
    abstract suspend fun deleteOutside(start: LocalDate, end: LocalDate) //todo do we need to specify account here?

    suspend fun deleteUnnecessary(){
        deleteOutside(Utils.getCurrentMonday().minusWeeks(2), Utils.getCurrentMonday().plusWeeks(2))
    }

    @Query("UPDATE rozvrh SET lastUpdate = \"1980-10-12T00:00:00.042Z\" WHERE monday != :monday AND account = :account")
    abstract fun invalidateAllOther(account: Int, monday: LocalDate)
    fun invalidateAllOther(key:RozvrhRecord.Key) = invalidateAllOther(key.account, key.monday)

    @Query("SELECT * FROM Rozvrh WHERE monday = :monday ORDER BY account")
    abstract fun getAllRozvrhsOfWeekLive(monday: LocalDate): LiveData<List<RozvrhRecord>>
}