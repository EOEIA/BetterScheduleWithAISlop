package cz.vitskalicky.lepsirozvrh.model

import androidx.room.Entity
import androidx.room.ForeignKey
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import org.joda.time.DateTime
import org.joda.time.LocalDate

@Entity(primaryKeys = ["account","monday"], foreignKeys = [ForeignKey(
    entity = Account::class,
    parentColumns = ["id"],
    childColumns = ["account"],
    onDelete = ForeignKey.CASCADE,
    onUpdate = ForeignKey.CASCADE,
    deferred = true
)])
data class RozvrhRecord(
    val account: Int,
    val monday: LocalDate,
    val lastUpdate: DateTime,
    val data: Rozvrh
)