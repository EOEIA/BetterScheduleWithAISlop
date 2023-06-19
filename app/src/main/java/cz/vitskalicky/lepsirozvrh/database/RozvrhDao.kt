package cz.vitskalicky.lepsirozvrh.database

import androidx.lifecycle.LiveData
import androidx.room.*
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.model.relations.RozvrhRelated
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import org.joda.time.DateTime
import org.joda.time.LocalDate

@Dao
abstract class RozvrhDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRozvrh(vararg rozvrhs: Rozvrh)

    @Delete
    abstract suspend fun deleteRozvrh(vararg rozvrhs: Rozvrh)

    @Update
    abstract suspend fun updateRozvrh(vararg rozvrhs: Rozvrh)

    @Query("SELECT * FROM rozvrh WHERE id = :monday")
    abstract fun loadRozvrhLive(monday: LocalDate): LiveData<Rozvrh>

    @Transaction
    @Query("SELECT * FROM rozvrh WHERE id = :monday")
    abstract fun loadRozvrhRelatedLive(monday: LocalDate): LiveData<RozvrhRelated>

    @Query("SELECT * FROM rozvrh WHERE id = :monday")
    abstract suspend fun loadRozvrh(monday: LocalDate): Rozvrh?

    @Transaction
    @Query("SELECT * FROM rozvrh WHERE id = :monday")
    abstract suspend fun loadRozvrhRelated(monday: LocalDate): RozvrhRelated?

    @Query("SELECT lastUpdate < :expireTime FROM rozvrh WHERE id= :monday")
    abstract suspend fun isExpired(monday: LocalDate, expireTime: DateTime): Boolean?

    /** Works line `touch` command in linux. Sets rozvrh's lastUpdate to the given time (DateTime.now() is default).
     * Used for prolonging validity of certain rozvrh.
     * */
    @Query("UPDATE rozvrh SET lastUpdate = :updateTime WHERE id = :monday")
    abstract suspend fun setLastUpdate(monday: LocalDate, updateTime: DateTime = DateTime.now())

    /** Sets last update time of given rozvrh to now */
    suspend fun resetExpiration(monday: LocalDate) = setLastUpdate(monday)

    @Query("DELETE FROM Rozvrh WHERE permanent = 0 AND id < :start OR id > :end")
    abstract suspend fun deleteOutside(start: LocalDate, end: LocalDate)

    suspend fun deleteUnnecessary(){
        deleteOutside(Utils.getCurrentMonday().minusWeeks(2), Utils.getCurrentMonday().plusWeeks(2))
    }

    @Query("UPDATE rozvrh SET lastUpdate = \"1980-10-12T00:00:00.042Z\" WHERE id != :monday")
    abstract fun invalidateAllOther(monday: LocalDate)
}