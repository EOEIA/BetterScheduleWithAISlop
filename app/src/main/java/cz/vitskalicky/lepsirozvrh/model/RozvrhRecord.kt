package cz.vitskalicky.lepsirozvrh.model

import androidx.room.*
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import org.joda.time.DateTime
import org.joda.time.LocalDate

@Entity(foreignKeys = [ForeignKey(
    entity = Account::class,
    parentColumns = ["id"],
    childColumns = ["account"],
    onDelete = ForeignKey.CASCADE,
    onUpdate = ForeignKey.CASCADE,
    deferred = true
)], tableName = "Rozvrh")
data class RozvrhRecord(
    @PrimaryKey
    @Embedded
    val key: Key,
    val lastUpdate: DateTime,
    val data: Rozvrh
){
    data class Key(
        val account: Long,
        val monday: LocalDate
    )
}