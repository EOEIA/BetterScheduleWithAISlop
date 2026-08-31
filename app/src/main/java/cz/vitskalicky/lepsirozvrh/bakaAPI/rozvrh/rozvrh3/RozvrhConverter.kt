package cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3

import android.content.Context
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.model.rozvrh.*
import io.sentry.Sentry
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** Converts [Rozvrh3] (what API uses) into [Rozvrh] (what this app uses).*/
object RozvrhConverter {
    /**
     * backup text to day description in case it is empty, but it is holiday.
     */
    val dayTypes: Map<String, Int> = mapOf(
            "WorkDay" to R.string.day_type_workday,
            "Holiday" to R.string.day_type_holiday,
            "Celebration" to  R.string.day_type_celebration,
            "Weekend" to R.string.day_type_weekend,
            "DirectorDay" to R.string.day_type_director_day
    )

    /**
     * prevents from sending many reports to Sentry
     */
    var sendUnknownDayTypeReport = true

    /** Converts [Rozvrh3] (what API uses) into [Rozvrh] (what this app uses).
     *  - [rozvrh3]: the data to cenvert
     *  - [date]: monday of the rozvrh or `null` if permanent
     *  - [context]: android context (used for translated strings)
     * */
    @Throws(RozvrhConversionException::class)
    fun convert(rozvrh3: Rozvrh3, date: LocalDate?, context: Context): Rozvrh{
        @Suppress("NAME_SHADOWING")
        val rozvrh3 = remove0thCaptionIfUnnecessary(rozvrh3)

        val monday : LocalDate = date?.let { Utils.getWeekMonday(date) } ?: Rozvrh.PERM
        val cycle: RozvrhCycle? = if (date == null){
                null
            }else{
                if (rozvrh3.cycles.isEmpty()){
                    RozvrhCycle("","","")
                }else{
                    val c3 = rozvrh3.cycles[0]
                    RozvrhCycle(c3.id, c3.name, c3.abbrev)
                }
            }

        //caption3 id and corresponding RozvrhCaption
        val captionsUnsorted = ArrayList<Pair<String,RozvrhCaption>>()
        for (value in rozvrh3.hours.withIndex()) {
            val item = value.value
            val nev = RozvrhCaption(
                name = item.caption,
                beginTime = LocalTime.parse(item.beginTime),
                endTime = LocalTime.parse(item.endTime)
            )
            captionsUnsorted.add(Pair(item.id.toString(), nev))
        }

        //to be extra sure, we sort the caption ascending by begin time to make sure it has the right index
        captionsUnsorted.sortWith( compareBy { it.second.beginTime } )
        //here we have the RozvrhCaptions. Key is the hourId, first in the pair is the index and second is the caption
        val captionsMap = HashMap<String, Pair<Int,RozvrhCaption>>()
        captionsUnsorted.forEachIndexed { index, pair -> captionsMap[pair.first] = Pair(index, pair.second) }
        //and here they are sorted by beginTime
        val captions: List<RozvrhCaption> = captionsUnsorted.mapIndexed { _, pair -> pair.second }

        // save each type of objects into a map with their id as keys
        val hours = HashMap<String, Hour3>()
        for (item in rozvrh3.hours) {
            hours[item.id.toString()] = item
        }
        val classes = HashMap<String, Class3>()
        for (item in rozvrh3.classes) {
            classes[item.id] = item
        }
        val groups = HashMap<String, Group3>()
        for (item in rozvrh3.groups) {
            groups[item.id] = item
        }
        val subjects = HashMap<String, Subject3>()
        for (item in rozvrh3.subjects) {
            subjects[item.id] = item
        }
        val teachers = HashMap<String, Teacher3>()
        for (item in rozvrh3.teachers) {
            teachers[item.id] = item
        }
        val rooms = HashMap<String, Room3>()
        for (item in rozvrh3.rooms) {
            rooms[item.id] = item
        }
        val cycles = HashMap<String, Cycle3>()
        for (item in rozvrh3.cycles) {
            cycles[item.id] = item
        }

        val days = ArrayList<RozvrhDay>() //days for the filan Rozvrh

        for (item in rozvrh3.days) {

            // determine day date
            var dayDate : LocalDate = if (monday != Rozvrh.PERM) {
                DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ").parseLocalDate(item.date)
            }else{
                Rozvrh.PERM.plusDays(item.dayOfWeek - 1)
            }
            // determine if there is an event on that day (such as holiday)
            var event: String? = null
            if (monday != Rozvrh.PERM){ //events in permanent schedule are ignored to "fix" a bug in Bakaláři API which puts celebration events into permanent schedule. You cannot have holiday in permanent schedule.
                if (item.dayDescription.isNotBlank()){
                    event = item.dayDescription
                }else if (item.dayType.isNotBlank()){
                    val dayType: Int? = dayTypes[item.dayType]
                    if (dayType == null){
                        //report unknown day type
                        //prevent spam
                        if (sendUnknownDayTypeReport){
                            sendUnknownDayTypeReport = false
                            (context.applicationContext as? MainApplication)?.sendReport(java.lang.Exception("[NOT CRITICAL] Unknown day type: ${item.dayType}"));
                        }
                        event = null
                    }else{
                        if (dayType == R.string.day_type_workday){
                            event = null
                        }else{
                            event = context.getString(dayType)
                        }
                    }
                }
            }

            val lessons = Array<ArrayList<RozvrhLesson>>(captions.size) { ArrayList() }
            for (atom in item.atoms) {
                val captionIndex: Int = captionsMap[atom.hourId]?.first ?:
                    //report problem
                    throw RozvrhConversionException("Failed to parse Rozvrh3 to Rozvrh: Could not find a caption for an atom: searched for '${atom.hourId}' available caption ids: ${captionsMap.keys}")

                var subjectName = ""
                var subjectAbbrev = ""

                atom.subjectId?.let { subjects[it] }?.let{
                    subjectName = it.name ?: ""
                    subjectAbbrev = it.abbrev ?: ""
                }

                var teacherName = ""
                var teacherAbbrev = ""

                atom.teacherId?.let { teachers[it] }?.let {
                    teacherName = it.name ?: ""
                    teacherAbbrev = it.abbrev ?: ""
                }

                var roomName = ""
                var roomAbbrev = ""

                atom.roomId?.let { rooms[it] }?.let {
                    roomName = it.name ?: ""
                    roomAbbrev = it.abbrev ?: ""
                }

                val theme = atom.theme ?: ""

                var changeType: Int = RozvrhLesson.NO_CHANGE
                var chngDesc: String? = null
                if (atom.change != null) {
                    chngDesc = atom.change.description
                    changeType = RozvrhLesson.CHANGED
                    if (!atom.change.typeAbbrev.isNullOrBlank()) {
                        changeType = RozvrhLesson.CANCELLED
                        subjectAbbrev = atom.change.typeAbbrev
                        subjectName = atom.change.typeName ?: ""
                    }
                }
                val changeKind: LessonChangeType = classifyChange(atom.change)

                val lessonGroups = ArrayList<RozvrhGroup>()
                atom.groupIds.forEach {
                    groups[it]?.let{
                        lessonGroups.add(RozvrhGroup(it.id, it.name, it.abbrev))
                    }
                }

                val lessonCycles = ArrayList<RozvrhCycle>()
                atom.cycleIds.forEach {
                    cycles[it]?.let {
                        lessonCycles.add(RozvrhCycle(it.id, it.name, it.abbrev))
                    }
                }

                val homeworkIds = ArrayList<String>()
                atom.homeworkIds.map {
                    if (it.length > 3){
                        val id = it.substring(2, 4)
                        for (grp in atom.groupIds) {
                            if (grp == id) {
                                homeworkIds.add(it)
                                break
                            }
                        }
                    }
                }

                lessons[captionIndex].add(RozvrhLesson(
                        subjectName,
                        subjectAbbrev,
                        teacherName,
                        teacherAbbrev,
                        roomName,
                        roomAbbrev,
                        lessonGroups,
                        lessonCycles,
                        homeworkIds,
                        theme,
                        changeType,
                        chngDesc,
                        changeKind
                ))
            }

            // Sometimes Bakaláři reports "Holiday" event even though there are lessons. The correct behaviour is to
            // ignore the event and display the lessons
            if (!lessons.all { it.isEmpty() }){
                event = null;
            }

            days.add(RozvrhDay(dayDate, event, lessons.toList()))
        }
        
        return Rozvrh(monday, monday == Rozvrh.PERM, cycle,captions, days)
    }

    /**
     * Classifies the change reported for a single lesson (atom) into a [LessonChangeType].
     *
     * Grounded in real Bakaláři API v3 payloads (see the demo data in `DebugUtils`), whose
     * `changeType` values include `"Added"`, `"Removed"` and `"Substitution"`; `"Canceled"` and
     * `"RoomChanged"` are the other documented values. Matching is case-insensitive to be
     * defensive against server-side casing differences.
     *
     * When the API does not provide a recognisable `changeType` but the legacy cancellation signal
     * ([Change3.typeAbbrev]) is present, the change is treated as [LessonChangeType.CANCELLED] so
     * that this stays consistent with how the rest of the app already detects cancelled lessons
     * (see the `typeAbbrev` branch in [convert]).
     *
     * Pure and Android-free so it can be unit-tested on the JVM.
     */
    fun classifyChange(change: Change3?): LessonChangeType {
        if (change == null) return LessonChangeType.NONE
        return when (change.changeType?.trim()?.lowercase()) {
            "added" -> LessonChangeType.ADDED
            "removed" -> LessonChangeType.REMOVED
            "canceled", "cancelled" -> LessonChangeType.CANCELLED
            "substitution" -> LessonChangeType.SUBSTITUTION
            "roomchanged" -> LessonChangeType.ROOM_CHANGED
            else -> if (!change.typeAbbrev.isNullOrBlank()) LessonChangeType.CANCELLED else LessonChangeType.OTHER
        }
    }

    /**
     * Romeves 0th caption if present and if unnecessary and return a modified copy.
     */
    fun remove0thCaptionIfUnnecessary(rozvrh3: Rozvrh3): Rozvrh3{
        val zeroCaptions = rozvrh3.hours.filter { it.caption.trim() == "0" }
        if (zeroCaptions.size != 1 ){
            return rozvrh3
        }
        val zeroCaption = zeroCaptions[0]

        var isEmpty: Boolean = true;
        rozvrh3.days.forEach {
            isEmpty = isEmpty && it.atoms.none { it.hourId == zeroCaption.id.toString() }
        }
        if (!isEmpty){
            return rozvrh3
        }
        return rozvrh3.copy(hours = rozvrh3.hours.toMutableList().apply { removeAll{ it.id == zeroCaption.id }})
    }

    class RozvrhConversionException(message: String): RuntimeException(message)
}