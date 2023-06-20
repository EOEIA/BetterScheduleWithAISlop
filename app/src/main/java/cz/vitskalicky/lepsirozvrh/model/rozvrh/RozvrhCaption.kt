package cz.vitskalicky.lepsirozvrh.model.rozvrh

import cz.vitskalicky.lepsirozvrh.model.LocalTimeSerializer
import kotlinx.serialization.Serializable
import org.joda.time.LocalTime

@Serializable
data class RozvrhCaption(
    val name: String,
    @Serializable(LocalTimeSerializer::class)
    val beginTime: LocalTime,
    @Serializable(LocalTimeSerializer::class)
    val endTime: LocalTime,
)