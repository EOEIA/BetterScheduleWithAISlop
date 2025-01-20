package cz.vitskalicky.lepsirozvrh.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.vitskalicky.lepsirozvrh.model.*
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh

@Database(
    entities = [RozvrhRecord::class, Account::class],
    version = 2,
)
@TypeConverters(*[LocalDateConverters::class, LocalTimeConverters::class, DateTimeConverters::class, Rozvrh.Converter::class])
abstract class RozvrhDatabase : RoomDatabase() {
    abstract fun rozvrhDao(): RozvrhDao

    abstract fun accountDao(): AccountDao
}

object Migrations{
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // clear all cached data and create new table according to scheme (see app/schemas/2.json)
            database.execSQL("DROP TABLE IF EXISTS Rozvrh")
            database.execSQL("DROP TABLE IF EXISTS RozvrhCaption ")
            database.execSQL("DROP TABLE IF EXISTS RozvrhDay ")
            database.execSQL("DROP TABLE IF EXISTS RozvrhBlock ")
            database.execSQL("DROP TABLE IF EXISTS RozvrhLesson")
            database.execSQL("DROP TABLE IF EXISTS Account")
            database.execSQL("CREATE TABLE IF NOT EXISTS `Account` (`serverUrl` TEXT NOT NULL, `username` TEXT NOT NULL, `accessToken` TEXT NOT NULL, `refreshToken` TEXT NOT NULL, `accessExpires` TEXT NOT NULL, `schoolName` TEXT NOT NULL, `fullName` TEXT NOT NULL, `userType` TEXT NOT NULL, `userTypeText` TEXT NOT NULL, `semesterEnd` TEXT, `userUID` TEXT NOT NULL, `requireRefresh` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `class_id` TEXT NOT NULL, `class_abbrev` TEXT NOT NULL, `class_name` TEXT NOT NULL)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `Rozvrh` (`lastUpdate` TEXT NOT NULL, `data` TEXT NOT NULL, `account` INTEGER NOT NULL, `monday` TEXT NOT NULL, PRIMARY KEY(`account`, `monday`), FOREIGN KEY(`account`) REFERENCES `Account`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
        }
    }
}
