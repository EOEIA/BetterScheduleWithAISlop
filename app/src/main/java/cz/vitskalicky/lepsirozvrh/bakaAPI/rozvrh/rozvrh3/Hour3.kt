package cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class Hour3 (
    val id: Int,
    val caption: String,
    val beginTime: String,
    val endTime: String,
)