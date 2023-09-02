package cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.rozvrh3

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)

data class Atom3 (
    val hourId: String,
    val groupIds: List<String>,
    val subjectId: String?,
    val teacherId: String?,
    val roomId: String?,
    val cycleIds: List<String>,
    val change: Change3?,
    val homeworkIds: List<String>,
    val theme: String? = null,
)