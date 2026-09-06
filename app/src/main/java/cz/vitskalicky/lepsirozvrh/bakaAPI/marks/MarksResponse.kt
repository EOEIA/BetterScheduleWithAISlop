package cz.vitskalicky.lepsirozvrh.bakaAPI.marks

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MarksResponse @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    // single-property data class + a List default value is a known jackson-module-kotlin trap:
    // without forcing PROPERTIES creator mode it gets misdetected as a delegating creator, falls
    // back to bean-style deserialization, and crashes trying to .add() onto the immutable emptyList()
    @JsonProperty("Subjects") val Subjects: List<MarkSubject> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MarkSubject(
    val Subject: SubjectRef = SubjectRef(),
    val Marks: List<Mark> = emptyList(),
    val AverageText: String = "",
    val TemporaryMark: String = "",
    val SubjectNote: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubjectRef(
    val Id: String = "",
    val Abbrev: String = "",
    val Name: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mark(
    val Id: String = "",
    val SubjectId: String = "",
    val Caption: String = "",
    val MarkText: String = "",
    val Weight: Int = 1,
    val Date: String = "",
    val IsNew: Boolean = false,
    val PointsText: String? = null
)
