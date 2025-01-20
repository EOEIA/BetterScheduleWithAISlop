package cz.vitskalicky.lepsirozvrh.model.rozvrh

import kotlinx.serialization.Serializable

@Serializable
data class RozvrhGroup(
    val id: String,
    val name: String,
    val abbrev: String
)