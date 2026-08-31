package cz.vitskalicky.lepsirozvrh

import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3.Change3
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3.RozvrhConverter
import cz.vitskalicky.lepsirozvrh.model.rozvrh.LessonChangeType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [RozvrhConverter.classifyChange] — the mapping from the Bakaláři API's per-lesson
 * change metadata to the semantic [LessonChangeType]. Pure JVM test (no Android dependencies).
 *
 * The recognised `changeType` strings mirror real API v3 payloads embedded in `DebugUtils`
 * (`"Added"`, `"Removed"`, `"Substitution"`) plus the documented `"Canceled"` / `"RoomChanged"`.
 */
class RozvrhChangeClassificationTest {

    private fun change(changeType: String?, typeAbbrev: String? = null) = Change3(
        changeSubject = null,
        day = null,
        hours = null,
        changeType = changeType,
        description = null,
        time = null,
        typeAbbrev = typeAbbrev,
        typeName = null,
    )

    @Test
    fun nullChangeIsNone() {
        assertEquals(LessonChangeType.NONE, RozvrhConverter.classifyChange(null))
    }

    @Test
    fun added() {
        assertEquals(LessonChangeType.ADDED, RozvrhConverter.classifyChange(change("Added")))
    }

    @Test
    fun removed() {
        assertEquals(LessonChangeType.REMOVED, RozvrhConverter.classifyChange(change("Removed")))
    }

    @Test
    fun substitution() {
        assertEquals(LessonChangeType.SUBSTITUTION, RozvrhConverter.classifyChange(change("Substitution")))
    }

    @Test
    fun canceledAmericanSpelling() {
        assertEquals(LessonChangeType.CANCELLED, RozvrhConverter.classifyChange(change("Canceled")))
    }

    @Test
    fun cancelledBritishSpelling() {
        assertEquals(LessonChangeType.CANCELLED, RozvrhConverter.classifyChange(change("Cancelled")))
    }

    @Test
    fun roomChanged() {
        assertEquals(LessonChangeType.ROOM_CHANGED, RozvrhConverter.classifyChange(change("RoomChanged")))
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertEquals(LessonChangeType.ADDED, RozvrhConverter.classifyChange(change("added")))
        assertEquals(LessonChangeType.SUBSTITUTION, RozvrhConverter.classifyChange(change("  SUBSTITUTION  ")))
    }

    /** Legacy cancellation signal: no recognised changeType, but a typeAbbrev present. */
    @Test
    fun typeAbbrevFallbackMeansCancelled() {
        assertEquals(LessonChangeType.CANCELLED, RozvrhConverter.classifyChange(change(changeType = null, typeAbbrev = "Od")))
    }

    @Test
    fun unrecognisedTypeIsOther() {
        assertEquals(LessonChangeType.OTHER, RozvrhConverter.classifyChange(change("SomethingBrandNew")))
    }

    @Test
    fun changePresentButBlankTypeIsOther() {
        assertEquals(LessonChangeType.OTHER, RozvrhConverter.classifyChange(change("")))
    }
}
