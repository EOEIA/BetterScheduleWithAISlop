package cz.vitskalicky.lepsirozvrh.database

import androidx.room.*
import cz.vitskalicky.lepsirozvrh.model.*
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh

@Database(entities = [RozvrhRecord::class, Account::class], version = 2)
@TypeConverters(*[LocalDateConverters::class, LocalTimeConverters::class, DateTimeConverters::class, Rozvrh.Converter::class])
abstract class RozvrhDatabase : RoomDatabase() {
    abstract fun rozvrhDao(): RozvrhDao

    abstract fun accountDao(): AccountDao
}
