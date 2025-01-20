package cz.vitskalicky.lepsirozvrh.schoolPicker

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SchoolDAO {
    @Insert
    suspend fun insertSchool(school: SchoolInfo)

    @Insert
    suspend fun insertSchools(schools: List<SchoolInfo>)

    @Query("DELETE FROM schools")
    suspend fun nukeTable()

    @Query("SELECT * FROM schools WHERE search_text MATCH :query")
    fun search(query: String): DataSource.Factory<Int, SchoolInfo>

    @Query("SELECT * FROM schools")
    fun queryAllSchools(): DataSource.Factory<Int, SchoolInfo>

    @Query("SELECT count(*) FROM schools")
    suspend fun countAllSchools(): Int
}