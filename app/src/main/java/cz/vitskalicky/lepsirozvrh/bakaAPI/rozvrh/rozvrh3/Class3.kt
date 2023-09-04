package cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class Class3 (
    val id: String,
    val abbrev: String,
    val name: String,
)