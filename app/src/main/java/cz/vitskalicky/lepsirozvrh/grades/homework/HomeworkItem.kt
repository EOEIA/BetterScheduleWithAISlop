package cz.vitskalicky.lepsirozvrh.grades.homework

import org.joda.time.LocalDate
import org.joda.time.LocalTime

data class HomeworkItem(
    val subjectName: String,
    val subjectAbbrev: String,
    /** `null` when the assignment text could not be fetched/matched; show a placeholder instead. */
    val description: String?,
    val date: LocalDate?,
    val lessonBeginTime: LocalTime?
)
