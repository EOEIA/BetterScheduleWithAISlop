package cz.vitskalicky.lepsirozvrh.notification

import cz.vitskalicky.lepsirozvrh.model.rozvrh.LessonChangeType
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhDay
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableDiffTest {

    private val monday = LocalDate.parse("2026-08-25")
    private val caption = RozvrhCaption("1", LocalTime(8, 0), LocalTime(8, 45))
    private val captions = listOf(caption)

    // --- helpers ---

    private fun lesson(
        subject: String = "MA",
        teacher: String = "Ko",
        room: String = "A101",
        changeKind: LessonChangeType = LessonChangeType.NONE
    ) = RozvrhLesson(
        subjectName = subject, subjectAbbrev = subject,
        teacherName = teacher, teacherAbbrev = teacher,
        roomName = room, roomAbbrev = room,
        groups = emptyList(), cycles = emptyList(), homeworkIds = emptyList(),
        theme = "", changeType = if (changeKind == LessonChangeType.NONE) 0 else 1,
        changeDescription = null, changeKind = changeKind
    )

    private fun rozvrh(
        days: List<RozvrhDay>,
        permanent: Boolean = false
    ) = Rozvrh(
        monday = if (permanent) Rozvrh.PERM else monday,
        permanent = permanent,
        cycle = null,
        captions = captions,
        days = days
    )

    private fun day(event: String? = null, vararg blocks: List<RozvrhLesson>) =
        RozvrhDay(monday, event, blocks.toList())

    // --- tests ---

    @Test
    fun emptyDiffWhenNothingChanged() {
        val r = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson())))))
        val diff = TimetableDiff.between(r, r)
        assertTrue(diff.isEmpty)
    }

    @Test
    fun changeKindNoneToSubstitutionIsDetected() {
        val old = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.NONE))))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.SUBSTITUTION))))))
        val diff = TimetableDiff.between(old, new)
        assertFalse(diff.isEmpty)
        assertEquals(1, diff.changedLessons.size)
        assertEquals(LessonChangeType.SUBSTITUTION, diff.changedLessons[0].changeKind)
    }

    @Test
    fun stableSubstitutionIsNotRealerted() {
        val r = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.SUBSTITUTION))))))
        val diff = TimetableDiff.between(r, r)
        assertTrue("Unchanged substitution should not re-alert", diff.isEmpty)
    }

    @Test
    fun cancelledLessonIsDetected() {
        val old = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson())))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.CANCELLED))))))
        val diff = TimetableDiff.between(old, new)
        assertEquals(1, diff.changedLessons.size)
        assertEquals(LessonChangeType.CANCELLED, diff.changedLessons[0].changeKind)
    }

    @Test
    fun subjectChangeIsDetected() {
        val old = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(subject = "MA"))))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(subject = "FY", changeKind = LessonChangeType.SUBSTITUTION))))))
        val diff = TimetableDiff.between(old, new)
        assertFalse(diff.isEmpty)
    }

    @Test
    fun teacherChangeIsDetected() {
        val old = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(teacher = "Ko"))))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(teacher = "No", changeKind = LessonChangeType.SUBSTITUTION))))))
        val diff = TimetableDiff.between(old, new)
        assertFalse(diff.isEmpty)
    }

    @Test
    fun roomChangeIsDetected() {
        val old = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(room = "A101"))))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(room = "B202", changeKind = LessonChangeType.ROOM_CHANGED))))))
        val diff = TimetableDiff.between(old, new)
        assertFalse(diff.isEmpty)
    }

    @Test
    fun newSlotWithChangeIsDetected() {
        val old = rozvrh(listOf(day(blocks = arrayOf(emptyList()))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.ADDED))))))
        val diff = TimetableDiff.between(old, new)
        assertFalse(diff.isEmpty)
        assertEquals(1, diff.changedLessons.size)
    }

    @Test
    fun newSlotWithNoneChangeKindIsNotAlerted() {
        val old = rozvrh(listOf(day(blocks = arrayOf(emptyList()))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.NONE))))))
        val diff = TimetableDiff.between(old, new)
        assertTrue("Regular lesson appearing without changeKind should not alert", diff.isEmpty)
    }

    @Test
    fun newNoSchoolEventIsDetected() {
        val old = rozvrh(listOf(day(event = null, blocks = arrayOf(listOf(lesson())))))
        val new = rozvrh(listOf(day(event = "Holiday", blocks = arrayOf(emptyList()))))
        val diff = TimetableDiff.between(old, new)
        assertFalse(diff.isEmpty)
        assertEquals(1, diff.noSchoolEvents.size)
        assertEquals("Holiday", diff.noSchoolEvents[0].event)
    }

    @Test
    fun unchangedNoSchoolEventIsNotRealerted() {
        val r = rozvrh(listOf(day(event = "Holiday", blocks = arrayOf(emptyList()))))
        val diff = TimetableDiff.between(r, r)
        assertTrue(diff.isEmpty)
    }

    @Test
    fun changedEventNameIsDetected() {
        val old = rozvrh(listOf(day(event = "Holiday A", blocks = arrayOf(emptyList()))))
        val new = rozvrh(listOf(day(event = "Holiday B", blocks = arrayOf(emptyList()))))
        val diff = TimetableDiff.between(old, new)
        assertFalse(diff.isEmpty)
        assertEquals("Holiday B", diff.noSchoolEvents[0].event)
    }

    @Test
    fun multipleDaysAndSlotsAreDiffedCorrectly() {
        val oldLesson = lesson(changeKind = LessonChangeType.NONE)
        val newLesson = lesson(changeKind = LessonChangeType.CANCELLED)
        val capts = listOf(caption, caption)
        val old = Rozvrh(monday, false, null, capts, listOf(
            RozvrhDay(monday, null, listOf(listOf(oldLesson), listOf(oldLesson))),
            RozvrhDay(monday.plusDays(1), null, listOf(listOf(oldLesson), listOf(oldLesson)))
        ))
        val new = Rozvrh(monday, false, null, capts, listOf(
            RozvrhDay(monday, null, listOf(listOf(oldLesson), listOf(newLesson))),  // slot [0][1] changed
            RozvrhDay(monday.plusDays(1), null, listOf(listOf(oldLesson), listOf(oldLesson)))  // no change
        ))
        val diff = TimetableDiff.between(old, new)
        assertEquals(1, diff.changedLessons.size)
        assertEquals(0, diff.changedLessons[0].dayIndex)
        assertEquals(1, diff.changedLessons[0].captionIndex)
    }

    @Test
    fun fingerprintIsStableForUnchangedRozvrh() {
        val r = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.SUBSTITUTION))))))
        assertEquals(r.changeFingerprint(), r.changeFingerprint())
    }

    @Test
    fun fingerprintDiffersAfterChange() {
        val old = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.NONE))))))
        val new = rozvrh(listOf(day(blocks = arrayOf(listOf(lesson(changeKind = LessonChangeType.CANCELLED))))))
        assertTrue(old.changeFingerprint() != new.changeFingerprint())
    }
}
