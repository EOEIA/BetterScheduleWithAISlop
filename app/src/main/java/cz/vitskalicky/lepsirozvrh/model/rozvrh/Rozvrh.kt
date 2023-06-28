package cz.vitskalicky.lepsirozvrh.model.rozvrh

import androidx.room.TypeConverter
import com.fasterxml.jackson.annotation.JsonIgnore
import cz.vitskalicky.lepsirozvrh.database.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.joda.time.LocalTime

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

    /**
     * returns the lesson block, which should be highlighted to the user as next or current lesson, or null
     * if the school is over or this is not the week.
     *
     * @param forNotification If true, the first lesson won't be highlighted up until one hour before its start
     */
    @JsonIgnore
    fun getHighlightBlock(forNotification: Boolean): RozvrhBlock? {
        val indexes = getHighlightBlockIndexes(forNotification) ?: return null;
        return getAsRozvrhBlock(indexes.first, indexes.second)
    }


    /**
     * returns day index (first in pair) and block index (second in pair) of the lesson block, which should be highlighted to the user as next or current lesson, or null
     * if the school is over or this is not the week.
     *
     * @param forNotification If true, the first lesson won't be highlighted up until one hour before its start
     */
    @JsonIgnore
    fun getHighlightBlockIndexes(forNotification: Boolean): Pair<Int,Int>? {
        val dayIndex = days.indexOfFirst { it.date == LocalDate.now() }
        val day: RozvrhDay = if (dayIndex == -1) return null else days[dayIndex]

        val nowTime = LocalTime.now()

        var first = true
        //remove empty blocks at the end of the day
        val blocksToCheck = day.blocks.toMutableList()
        if (blocksToCheck.isEmpty() || day.event != null){
            return null
        }
        while (true){
            val item = blocksToCheck.lastOrNull()
            if (item?.isEmpty() == true){
                blocksToCheck.removeLast()
            }else{
                break
            }
        }

        for (i in blocksToCheck.indices) {
            val item: List<RozvrhLesson> = blocksToCheck[i]
            val lesson: RozvrhLesson? = item.getOrNull(0)
            if (lesson != null || !first) {
                if (forNotification && first && nowTime.isBefore(captions[i].beginTime.minusHours(1))) { //do not highlight
                    return null
                }
                if (nowTime.isBefore(captions[i].endTime.minusMinutes(10))) {
                    return Pair(dayIndex,i)
                }
                first = false
            }
        }

        return null
    }

    /** Return `null` if out of bounds */
    @JsonIgnore
    fun getAsRozvrhBlock(dayIndex: Int, blockIndex: Int): RozvrhBlock?{
        val day = days.getOrNull(dayIndex) ?: return null
        val caption = captions.getOrNull(blockIndex) ?: return null
        return RozvrhBlock(day, caption, day.blocks.getOrNull(blockIndex) ?: return null)
    }
}