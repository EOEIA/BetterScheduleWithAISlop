package cz.vitskalicky.lepsirozvrh.notification

import cz.vitskalicky.lepsirozvrh.model.rozvrh.LessonChangeType
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson

/**
 * Result of comparing two fetched versions of the same timetable week.
 * Contains only changes that are *new* relative to the previously cached version.
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
data class TimetableDiff(
    /** Lessons whose content or change-kind differs between the old and new fetch. */
    val changedLessons: List<ChangedLesson>,
    /** Days that gained a new or different no-school event. */
    val noSchoolEvents: List<NoSchoolEvent>
) {
    val isEmpty: Boolean get() = changedLessons.isEmpty() && noSchoolEvents.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty

    data class ChangedLesson(
        val dayIndex: Int,
        val captionIndex: Int,
        val lesson: RozvrhLesson,
        val changeKind: LessonChangeType
    )

    data class NoSchoolEvent(
        val dayIndex: Int,
        val event: String
    )

    companion object {
        /**
         * Computes the diff between [old] (previously cached) and [new] (freshly fetched).
         * Only changes that are new relative to [old] are returned; stable changes are silently
         * ignored so users are not re-alerted for the same substitution every fetch cycle.
         */
        fun between(old: Rozvrh, new: Rozvrh): TimetableDiff {
            val changedLessons = mutableListOf<ChangedLesson>()
            val noSchoolEvents = mutableListOf<NoSchoolEvent>()

            for ((dayIndex, newDay) in new.days.withIndex()) {
                val oldDay = old.days.getOrNull(dayIndex)

                // No-school event that is new or whose name changed
                val newEvent = newDay.event
                if (newEvent != null && newEvent != oldDay?.event) {
                    noSchoolEvents.add(NoSchoolEvent(dayIndex, newEvent))
                }

                // Per-slot lesson diff
                for ((captionIndex, newBlock) in newDay.blocks.withIndex()) {
                    val oldBlock = oldDay?.blocks?.getOrNull(captionIndex) ?: emptyList()
                    for ((lessonIndex, newLesson) in newBlock.withIndex()) {
                        val oldLesson = oldBlock.getOrNull(lessonIndex)
                        if (isSignificantChange(oldLesson, newLesson)) {
                            changedLessons.add(
                                ChangedLesson(dayIndex, captionIndex, newLesson, newLesson.changeKind)
                            )
                        }
                    }
                }
            }

            return TimetableDiff(changedLessons, noSchoolEvents)
        }

        /**
         * A change is "significant" — and thus worth alerting — when the change kind or the
         * displayed lesson identity (subject abbrev, teacher abbrev, room abbrev) changed relative
         * to the previously cached version.  Stable data (including stable substitutions already
         * shown in the old cache) returns false.
         */
        private fun isSignificantChange(old: RozvrhLesson?, new: RozvrhLesson): Boolean {
            if (old == null) return new.changeKind != LessonChangeType.NONE
            return old.changeKind != new.changeKind ||
                   old.subjectAbbrev != new.subjectAbbrev ||
                   old.teacherAbbrev != new.teacherAbbrev ||
                   old.roomAbbrev != new.roomAbbrev
        }
    }
}

/**
 * A compact fingerprint of the change-relevant fields of this Rozvrh.
 * Used to suppress duplicate notifications when the same changed timetable is
 * fetched multiple times without any further school-side update.
 */
fun Rozvrh.changeFingerprint(): Long {
    var h = 1L
    for (day in days) {
        h = 31 * h + (day.event?.hashCode()?.toLong() ?: 0L)
        for (block in day.blocks) {
            for (lesson in block) {
                h = 31 * h + lesson.subjectAbbrev.hashCode()
                h = 31 * h + lesson.teacherAbbrev.hashCode()
                h = 31 * h + lesson.roomAbbrev.hashCode()
                h = 31 * h + lesson.changeKind.ordinal
            }
        }
    }
    return h
}
