package cz.vitskalicky.lepsirozvrh.grades

import java.util.UUID

/**
 * Local-only (no persistence) "what-if" edits a user has made for one subject.
 * Never sent to the server.
 */
data class PredictionState(
    /** Per-mark overrides: markId → overridden values. Null fields mean "keep original". */
    val overrides: Map<String, MarkOverride> = emptyMap(),
    /** Hypothetical grades added by the user (not in the real fetch). */
    val added: List<HypotheticalMark> = emptyList(),
    /** Ids of real marks excluded from the prediction. */
    val excluded: Set<String> = emptySet()
) {
    val isEmpty: Boolean get() = overrides.isEmpty() && added.isEmpty() && excluded.isEmpty()
    val isActive: Boolean get() = !isEmpty

    fun withOverride(markId: String, override: MarkOverride): PredictionState =
        copy(overrides = overrides + (markId to override))

    fun withoutOverride(markId: String): PredictionState =
        copy(overrides = overrides - markId)

    fun withExcluded(markId: String): PredictionState =
        copy(excluded = excluded + markId)

    fun withIncluded(markId: String): PredictionState =
        copy(excluded = excluded - markId)

    fun withAdded(mark: HypotheticalMark): PredictionState =
        copy(added = added + mark)

    fun withRemovedAdded(hypoId: String): PredictionState =
        copy(added = added.filter { it.id != hypoId })

    fun withUpdatedAdded(hypoId: String, markText: String, weight: Int, caption: String): PredictionState =
        copy(added = added.map {
            if (it.id == hypoId) it.copy(markText = markText, weight = weight, caption = caption) else it
        })
}

data class MarkOverride(
    val markText: String? = null,
    val weight: Int? = null
)

data class HypotheticalMark(
    val id: String = "hypo-${UUID.randomUUID()}",
    val markText: String,
    val weight: Int,
    val caption: String = ""
)
