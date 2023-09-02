package cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class Change3 (
    val changeSubject: String?,
    val day: String?,
    val hours: String?,
    val changeType: String?,
    val description: String?,
    val time: String?,
    val typeAbbrev: String?,
    val typeName: String?,
)