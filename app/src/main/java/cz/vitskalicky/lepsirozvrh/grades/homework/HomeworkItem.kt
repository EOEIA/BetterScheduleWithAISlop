package cz.vitskalicky.lepsirozvrh.grades.homework

import org.joda.time.LocalDate
import org.joda.time.LocalTime

data class HomeworkItem(
    /** Bakaláři's homework id - also the key the local done-override is stored under. */
    val id: String,
    val subjectName: String,
    val subjectAbbrev: String,
    /** `null` when the assignment text could not be fetched/matched; show a placeholder instead. */
    val description: String?,
    val date: LocalDate?,
    val lessonBeginTime: LocalTime?,
    /** What Bakaláři reports; a local override in `homework_done` wins over this. */
    val isDone: Boolean = false
)
