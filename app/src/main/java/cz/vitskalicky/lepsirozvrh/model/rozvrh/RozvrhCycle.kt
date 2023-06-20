package cz.vitskalicky.lepsirozvrh.model.rozvrh

import kotlinx.serialization.Serializable

@Serializable
data class RozvrhCycle(
    val id: String,
    val name: String,
    val abbrev: String
)