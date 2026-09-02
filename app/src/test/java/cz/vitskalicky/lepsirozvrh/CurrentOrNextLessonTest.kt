package cz.vitskalicky.lepsirozvrh

import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhDay
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import cz.vitskalicky.lepsirozvrh.model.rozvrh.LessonChangeType
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentOrNextLessonTest {
    private val date = LocalDate.parse("2026-08-28")

    @Test
    fun returnsCurrentLessonWhenLessonIsInProgress() {
        val rozvrh = rozvrh(
            listOf(
                listOf(lesson("Math")),
                listOf(lesson("English"))
            )
        )

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(8, 15)))

        assertEquals(Rozvrh.RelativeLessonState.CURRENT, result?.state)
        assertEquals("Math", result?.lesson?.subjectName)
    }

    @Test
    fun returnsNextLessonBeforeLessonStarts() {
        val rozvrh = rozvrh(
            listOf(
                emptyList(),
                listOf(lesson("English"))
            )
        )

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(8, 15)))

        assertEquals(Rozvrh.RelativeLessonState.NEXT, result?.state)
        assertEquals("English", result?.lesson?.subjectName)
    }

    @Test
    fun returnsNullAfterLastLessonEnds() {
        val rozvrh = rozvrh(listOf(listOf(lesson("Math"))))

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(15, 0)))

        assertNull(result)
    }

    @Test
    fun returnsNextLessonOnFutureDay() {
        val rozvrh = rozvrh(
            listOf(
                RozvrhDay(date, null, listOf(listOf(lesson("Math")))),
                RozvrhDay(date.plusDays(3), null, listOf(listOf(lesson("Physics"))))
            )
        )

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(15, 0)))

        assertEquals(Rozvrh.RelativeLessonState.NEXT, result?.state)
        assertEquals("Physics", result?.lesson?.subjectName)
        assertEquals(date.plusDays(3).toLocalDateTime(LocalTime(8, 0)), result?.targetDateTime)
    }

    @Test
    fun skipsNoSchoolDaysWhenLookingForNextLesson() {
        val rozvrh = rozvrh(
            listOf(
                RozvrhDay(date, "Holiday", listOf(emptyList())),
                RozvrhDay(date.plusDays(1), null, listOf(listOf(lesson("Physics"))))
            )
        )

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(8, 0)))

        assertEquals("Physics", result?.lesson?.subjectName)
    }

    @Test
    fun skipsCancelledCurrentLesson() {
        val rozvrh = rozvrh(
            listOf(
                listOf(lesson("Math", changeKind = LessonChangeType.CANCELLED)),
                listOf(lesson("English"))
            )
        )

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(8, 15)))

        assertEquals(Rozvrh.RelativeLessonState.NEXT, result?.state)
        assertEquals("English", result?.lesson?.subjectName)
    }

    @Test
    fun skipsCancelledNextLesson() {
        val rozvrh = rozvrh(
            listOf(
                emptyList(),
                listOf(lesson("English", changeType = RozvrhLesson.CANCELLED)),
                listOf(lesson("Physics"))
            )
        )

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(8, 15)))

        assertEquals(Rozvrh.RelativeLessonState.NEXT, result?.state)
        assertEquals("Physics", result?.lesson?.subjectName)
    }

    @Test
    fun permanentScheduleWrapsPastDaysToNextWeek() {
        val rozvrh = rozvrh(
            days = listOf(
                RozvrhDay(Rozvrh.PERM.withDayOfWeek(1), null, listOf(listOf(lesson("Math"))))
            ),
            permanent = true
        )

        val result = rozvrh.getCurrentOrNextLesson(date.toLocalDateTime(LocalTime(15, 0)))

        assertEquals(Rozvrh.RelativeLessonState.NEXT, result?.state)
        assertEquals(date.plusDays(3).toLocalDateTime(LocalTime(8, 0)), result?.targetDateTime)
    }

    private fun rozvrh(blocks: List<List<RozvrhLesson>>) = Rozvrh(
        monday = date.withDayOfWeek(1),
        permanent = false,
        cycle = null,
        captions = listOf(
            RozvrhCaption("", LocalTime(8, 0), LocalTime(8, 45)),
            RozvrhCaption("", LocalTime(9, 0), LocalTime(9, 45)),
            RozvrhCaption("", LocalTime(10, 0), LocalTime(10, 45))
        ).take(blocks.size),
        days = listOf(RozvrhDay(date, null, blocks))
    )

    private fun rozvrh(days: List<RozvrhDay>, permanent: Boolean = false) = Rozvrh(
        monday = if (permanent) Rozvrh.PERM else date.withDayOfWeek(1),
        permanent = permanent,
        cycle = null,
        captions = listOf(
            RozvrhCaption("", LocalTime(8, 0), LocalTime(8, 45))
        ),
        days = days
    )

    private fun lesson(
        subject: String,
        changeType: Int = RozvrhLesson.NO_CHANGE,
        changeKind: LessonChangeType = LessonChangeType.NONE
    ) = RozvrhLesson(
        subjectName = subject,
        subjectAbbrev = subject.take(2),
        teacherName = "Teacher",
        teacherAbbrev = "T",
        roomName = "101",
        roomAbbrev = "101",
        groups = emptyList(),
        cycles = emptyList(),
        homeworkIds = emptyList(),
        theme = "",
        changeType = changeType,
        changeDescription = null,
        changeKind = changeKind
    )
}
