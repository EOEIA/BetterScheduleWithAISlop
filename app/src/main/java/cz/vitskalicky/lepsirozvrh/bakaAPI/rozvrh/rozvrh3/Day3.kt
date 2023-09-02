package cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class Day3 (
    val atoms: List<Atom3>,
    val dayOfWeek: Int,
    val date: String,
    val dayDescription: String,
    val dayType: String,
)