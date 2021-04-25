package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(entities = [SchoolInfo::class], version = 1, exportSchema = false)
abstract class SchoolsDatabase : RoomDatabase() {
    abstract fun schoolDAO(): SchoolDAO

    suspend fun replaceSchools(schools: List<SchoolInfo>){
        withTransaction {
            schoolDAO().nukeTable()
            schoolDAO().insertSchools(schools)
        }
    }
}