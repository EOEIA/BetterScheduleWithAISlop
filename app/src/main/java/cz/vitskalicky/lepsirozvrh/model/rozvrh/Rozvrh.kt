package cz.vitskalicky.lepsirozvrh.model.rozvrh

import androidx.room.TypeConverter
import cz.vitskalicky.lepsirozvrh.database.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
@Serializable
data class Rozvrh(
    /**
     * Monday of the week. [Rozvrh.PERM] for permanent schedule.
     */
    @Serializable(LocalDateSerializer::class)
    val monday: LocalDate,
    val permanent: Boolean,
    val cycle: RozvrhCycle?,
    val captions: List<RozvrhCaption>,
    val days: List<RozvrhDay>
){
    companion object{
        val PERM: LocalDate = LocalDate.parse("0000-01-01").plusWeeks(1).withDayOfWeek(DateTimeConstants.MONDAY)

        @TypeConverter
        fun toJsonString(value: Rozvrh):String = Json.encodeToString(value);
        @TypeConverter
        fun fromJsonString(value: String):Rozvrh = Json.decodeFromString<Rozvrh>(value);
    }
}