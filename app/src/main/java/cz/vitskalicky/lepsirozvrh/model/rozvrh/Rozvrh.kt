package cz.vitskalicky.lepsirozvrh.model.rozvrh

import androidx.room.TypeConverter
import com.fasterxml.jackson.annotation.JsonIgnore
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.database.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime

/** data class for rozvrh used by the app. The API uses [Rozvrh3] which needs to be converted into this.*/
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
    }

    object Converter{
        @TypeConverter
        fun toJsonString(value: Rozvrh):String = Json.encodeToString(value);
        @TypeConverter
        fun fromJsonString(value: String):Rozvrh = Json.decodeFromString<Rozvrh>(value);
    }

    enum class RelativeLessonState {
        CURRENT,
        NEXT
    }

    data class RelativeLesson(
        val block: RozvrhBlock,
        val lesson: RozvrhLesson,
        val state: RelativeLessonState,
        val targetDateTime: LocalDateTime
    )

    /**
     * Returns the current lesson if one is in progress, otherwise the next lesson in this
     * timetable. Empty timetable blocks and no-school event days are skipped.
     */
    @JsonIgnore
    fun getCurrentOrNextLesson(now: LocalDateTime = LocalDateTime.now()): RelativeLesson? {
        val candidates = days
            .filter { it.event == null }
            .flatMap { day ->
                val displayDate = displayDateForCurrentOrNextLesson(day.date, now)
                captions.indices.mapNotNull { captionIndex ->
                    val lesson = day.blocks.getOrNull(captionIndex)
                        ?.firstOrNull { it.takesPlace() }
                        ?: return@mapNotNull null
                    val caption = captions[captionIndex]
                    CandidateLesson(
                        block = RozvrhBlock(day, caption, day.blocks[captionIndex]),
                        lesson = lesson,
                        begin = displayDate.toLocalDateTime(caption.beginTime),
                        end = displayDate.toLocalDateTime(caption.endTime)
                    )
                }
            }
            .sortedBy { it.begin }

        candidates.firstOrNull { !now.isBefore(it.begin) && now.isBefore(it.end) }?.let {
            return RelativeLesson(it.block, it.lesson, RelativeLessonState.CURRENT, it.end)
        }

        candidates.firstOrNull { now.isBefore(it.begin) }?.let {
            return RelativeLesson(it.block, it.lesson, RelativeLessonState.NEXT, it.begin)
        }

        return null
    }

    private data class CandidateLesson(
        val block: RozvrhBlock,
        val lesson: RozvrhLesson,
        val begin: LocalDateTime,
        val end: LocalDateTime
    )

    private fun RozvrhLesson.takesPlace(): Boolean =
        changeType != RozvrhLesson.CANCELLED &&
            changeKind != LessonChangeType.CANCELLED &&
            changeKind != LessonChangeType.REMOVED

    private fun displayDateForCurrentOrNextLesson(dayDate: LocalDate, now: LocalDateTime): LocalDate {
        if (!permanent) return dayDate

        val today = now.toLocalDate()
        val dateThisWeek = today.withDayOfWeek(dayDate.dayOfWeek)
        return if (dateThisWeek.isBefore(today)) dateThisWeek.plusWeeks(1) else dateThisWeek
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

    /**
     * Returns lessons that should be displayed on a widget or an empty list if all the lessons are already over. If there is en event on the current day ([RozvrhDay.event] != null),
     * the list is `null` and the string contains name of the event. Otherwise the string is `null`.
     *
     * If this is not the current week, the pair is `null`.
     *
     * @param length how many lessons does the widget display - determines the length of the returned list.
     * @return a [Pair] of nullable list and nullable string or `null` if this is not the current week.
     * The first parameter is list of lessons which should be displayed or empty list if all the lessons
     * are already over or `null` if there is an event on that day. The second parameter is the description of current event or `null`
     * if there is no event on that day.
     */
    @JsonIgnore
    fun getWidgetDisplayBlocks(length: Int): Pair<List<RozvrhBlock>?,String?>?{
        if (Utils.getCurrentMonday() != monday){
            return null
        }
        return days.indexOfFirst { it.date == LocalDate.now() }.takeUnless { it == -1 }?.let { getWidgetDisplayBlocksForDay(it, length) } ?: Pair(emptyList(), null)
    }

    /**
     * Returns lessons that should be displayed on a widget or an empty list if all the lessons are already over. If there is en event on the day ([RozvrhDay.event] != null),
     * the list is `null` and the string contains name of the event. Otherwise the string is `null`.
     *
     * If this is not today, the pair is `null`.
     *
     * @param length how many lessons does the widget display - determines the length of the returned list.
     * @return a [Pair] of nullable list and nullable string or `null` if this is not today.
     * The first parameter is list of lessons which should be displayed or empty list if all the lessons
     * are already over or `null` if there is an event on that day. The second parameter is the description of current event or `null`
     * if there is no event on that day.
     */
    private fun getWidgetDisplayBlocksForDay(dayIndex: Int, length: Int): Pair<List<RozvrhBlock>?,String?>?{
        val nowDate = LocalDate.now()
        val nowTime = LocalTime.now()
        val day = days[dayIndex]

        if (nowDate != day.date){
            return null
        }
        if (day.event != null){
            return Pair(null, day.event)
        }

        //remove empty blocks at the end and beginning of the day
        val blocksToCheck = day.blocks.toMutableList()
        while (true){
            val item = blocksToCheck.lastOrNull()
            if (item?.isEmpty() == true){
                blocksToCheck.removeLast()
            }else{
                break
            }
        }
        var skip = 0;
        while (true){
            val item = blocksToCheck.getOrNull(skip)
            if (item?.isEmpty() == true){
                skip++
            }else{
                break
            }
        }

        var nowIndex = skip;
        while ( nowIndex < blocksToCheck.size && captions[nowIndex].endTime.minusMinutes(10).isBefore(nowTime)){
            nowIndex++
        }
        val ret = ArrayList<RozvrhBlock>()
        for (i in 0 until length){
            if (nowIndex + i < blocksToCheck.size){
                ret.add(RozvrhBlock(day, captions[nowIndex + i], blocksToCheck[nowIndex + i]))
            }
        }
        return Pair(ret, null)
    }

    /**
     * Return time when the notification and widget should be updated or `null` if this week is already over.
     */
    @JsonIgnore
    fun getUpdateDisplayedDataTime(): LocalDateTime?{
        val nowDate = LocalDate.now()
        val nowTime = LocalTime.now()

        var updateTime: LocalDateTime? = null;

        val futureDays: List<RozvrhDay> = days.filter { !it.date.isBefore(nowDate) }
        var index = 0;

        while (updateTime == null && index < futureDays.size) {

            val day: RozvrhDay = futureDays[index]

            var first = true

            for (i in day.blocks.indices) {
                val item: RozvrhBlock = RozvrhBlock(day, captions[i], day.blocks[i])
                val lesson: RozvrhLesson? = item.lessons.getOrNull(0)
                if (lesson != null || !first) {
                    if (nowTime.isBefore(item.caption.endTime.minusMinutes(10))) {
                        if (first && nowTime.isBefore(item.caption.beginTime.minusHours(3))) {
                            updateTime = day.date.toLocalDateTime(item.caption.beginTime.minusHours(3));
                        } else if (first && nowTime.isBefore(item.caption.beginTime.minusHours(1))) {
                            updateTime = day.date.toLocalDateTime(item.caption.beginTime.minusHours(1));
                        } else {
                            updateTime = day.date.toLocalDateTime(item.caption.endTime.minusMinutes(10));
                        }
                        break
                    }
                    first = false
                }
            }
            if (day.event != null) {
                updateTime = day.date.toLocalDateTime(LocalTime.MIDNIGHT)
                if (updateTime!!.isBefore(LocalDateTime.now())) {
                    updateTime = day.date.toLocalDateTime(captions.firstOrNull()?.beginTime?.minusHours(2)
                        ?: LocalTime.MIDNIGHT)
                }
                if (updateTime!!.isBefore(LocalDateTime.now())) {
                    updateTime = null
                }
            }
            index++;
        }

        return updateTime
    }
}
