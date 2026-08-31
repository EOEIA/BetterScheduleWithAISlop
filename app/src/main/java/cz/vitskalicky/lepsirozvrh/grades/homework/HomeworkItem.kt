package cz.vitskalicky.lepsirozvrh.grades.homework

import org.joda.time.LocalDate
import org.joda.time.LocalTime

data class HomeworkItem(
    val subjectName: String,
    val subjectAbbrev: String,
    val description: String,
    val date: LocalDate?,
    val lessonBeginTime: LocalTime?
)
