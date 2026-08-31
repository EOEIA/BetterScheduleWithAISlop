package cz.vitskalicky.lepsirozvrh.model.rozvrh

import kotlinx.serialization.Serializable

/**
 * Semantic classification of a lesson change *as reported by the Bakaláři API for a single
 * timetable fetch* — i.e. how this lesson differs from the school's normal schedule.
 *
 * It is derived from [cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3.Change3.changeType] by
 * `RozvrhConverter.classifyChange`. It intentionally only covers what a single response can tell
 * us. Notions that require comparing two separately fetched timetables (such as a change being
 * *restored*, a lesson being *moved*, or a *group* change) are NOT represented here — those belong
 * to a later timetable-diff layer.
 *
 * The legacy [RozvrhLesson.changeType] `Int` (NO_CHANGE / CHANGED / CANCELLED) is kept unchanged
 * for existing UI; this enum is an additive, finer-grained companion.
 */
@Serializable
enum class LessonChangeType {
    /** No change reported for this lesson. */
    NONE,

    /** Lesson was added to the timetable (Bakaláři `"Added"`). */
    ADDED,

    /** Lesson was removed from the timetable (Bakaláři `"Removed"`). */
    REMOVED,

    /** Lesson is cancelled / does not take place (Bakaláři `"Canceled"`, or the legacy `typeAbbrev` signal). */
    CANCELLED,

    /** Lesson is substituted — subject / teacher / time swap (Bakaláři `"Substitution"`). */
    SUBSTITUTION,

    /** Only the room changed (Bakaláři `"RoomChanged"`). */
    ROOM_CHANGED,

    /** A change is present but its type was not recognised. Treat as a generic change. */
    OTHER,
}
