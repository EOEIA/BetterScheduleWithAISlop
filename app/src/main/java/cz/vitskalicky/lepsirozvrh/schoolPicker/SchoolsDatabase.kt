package cz.vitskalicky.lepsirozvrh.schoolPicker

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SchoolInfo::class], version = 2)
abstract class SchoolsDatabase : RoomDatabase() {
    abstract fun schoolDAO(): SchoolDAO

    suspend fun replaceSchools(schools: List<SchoolInfo>){
        withTransaction {
            schoolDAO().nukeTable()
            schoolDAO().insertSchools(schools)
        }
    }
}

object Migrations{
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // clear all cached data and create new table according to scheme (see app/schemas/2.json)
            database.execSQL("DROP TABLE IF EXISTS schools")
            database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `schools` USING FTS4(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `isManual` INTEGER NOT NULL, `search_text` TEXT)")
        }
    }
}