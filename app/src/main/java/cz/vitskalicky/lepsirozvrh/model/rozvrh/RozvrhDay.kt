package cz.vitskalicky.lepsirozvrh.model.rozvrh

import cz.vitskalicky.lepsirozvrh.database.LocalDateSerializer
import kotlinx.serialization.Serializable
import org.joda.time.LocalDate

@Serializable
data class RozvrhDay(
    /**
     * if permanent, then it is a date from the week of [Rozvrh.PERM].
     */
    @Serializable(LocalDateSerializer::class)
    val date: LocalDate,
    //val dayOfWeek: Int,
    /**
     * For completely free days such as holiday. If not `null`, then the whole day is free.
     */
    val event: String?,
    /** Outer list represents list of *blocks* (lessons in one caption), each inner list is a block with lessons.
     * shall be exactly the length of the captions list of the rozvrh.
     */
    val blocks: List<List<RozvrhLesson>>
)