package cz.vitskalicky.lepsirozvrh.bakaAPI.homework

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Shape inferred from other Bakaláři API v3 clients; unknown fields are ignored and missing
 * ones default to blank/empty so a schema mismatch degrades to "no description" instead of crashing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HomeworksResponse @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    // single-property data class + a List default value is a known jackson-module-kotlin trap:
    // without forcing PROPERTIES creator mode it gets misdetected as a delegating creator, falls
    // back to bean-style deserialization, and crashes trying to .add() onto the immutable emptyList()
    @JsonProperty("Homeworks") val Homeworks: List<Homework> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Homework(
    // the real API returns this field as "ID" (all caps) - kept exact since Jackson's
    // case-insensitive property matching does not reliably apply to Kotlin constructor params
    val ID: String = "",
    val Subject: HomeworkSubject = HomeworkSubject(),
    val DateEnd: String = "",
    val Content: String = "",
    val Done: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HomeworkSubject(
    val Id: String = "",
    val Abbrev: String = "",
    val Name: String = ""
)
