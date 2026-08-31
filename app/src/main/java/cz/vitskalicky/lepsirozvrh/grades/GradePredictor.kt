package cz.vitskalicky.lepsirozvrh.grades

import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.Mark

object GradePredictor {

    /** Parses Czech school grade text to a numeric value, or null if non-numeric (e.g. "OM"). */
    fun parseGrade(markText: String): Double? {
        val trimmed = markText.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.endsWith('+')) {
            val base = trimmed.dropLast(1).toDoubleOrNull() ?: return null
            return base - 0.5
        }
        if (trimmed.endsWith('-')) {
            val base = trimmed.dropLast(1).toDoubleOrNull() ?: return null
            return base + 0.5
        }
        return trimmed.toDoubleOrNull()
    }

    /** Weighted average, or null for empty or zero-weight list. */
    fun weightedAverage(grades: List<Pair<Double, Int>>): Double? {
        if (grades.isEmpty()) return null
        val totalWeight = grades.sumOf { it.second }
        if (totalWeight == 0) return null
        return grades.sumOf { it.first * it.second } / totalWeight
    }

    /**
     * Returns the Czech grade band (1–5) for a given weighted average.
     * Boundaries: ≤1.49→1, ≤2.49→2, ≤3.49→3, ≤4.49→4, else→5.
     */
    fun roundedGrade(avg: Double): Int = when {
        avg <= 1.49 -> 1
        avg <= 2.49 -> 2
        avg <= 3.49 -> 3
        avg <= 4.49 -> 4
        else -> 5
    }

    private val bandMaxAvg = mapOf(1 to 1.49, 2 to 2.49, 3 to 3.49, 4 to 4.49)

    enum class GradeOrigin { REAL, EDITED, ADDED, EXCLUDED }

    data class EffectiveGrade(
        val id: String,
        val markText: String,
        val weight: Int,
        val caption: String,
        val date: String,
        val origin: GradeOrigin,
        val numericValue: Double?
    )

    /** What grade to add (at a given weight) to cross a grade band threshold. */
    data class Suggestion(
        /** Upper bound of the target band (e.g. 1.49 for grade 1). */
        val targetMaxAvg: Double,
        /** Target grade number. */
        val targetRoundedGrade: Int,
        /** Grade needed to reach that threshold. */
        val neededGrade: Double,
        val addedWeight: Int
    )

    data class PredictionResult(
        val realAverage: Double?,
        val predictedAverage: Double?,
        val realRoundedGrade: Int?,
        val predictedRoundedGrade: Int?,
        /** Suggestions to improve by one grade band, computed from REAL marks. */
        val realImproveSuggestions: List<Suggestion>,
        /** Suggestions to reach grade 1 (empty if already grade 1 or same target as improve). */
        val realGrade1Suggestions: List<Suggestion>
    ) {
        /** Positive = improved (lower avg = better); null if either avg is absent. */
        val delta: Double? get() =
            if (realAverage != null && predictedAverage != null) realAverage - predictedAverage else null
        val isImproved: Boolean get() = (delta ?: 0.0) > 0.001
    }

    /** Computes the effective grade list from real marks + local prediction edits. */
    fun effectiveGrades(realMarks: List<Mark>, state: PredictionState): List<EffectiveGrade> {
        val result = mutableListOf<EffectiveGrade>()
        for (mark in realMarks) {
            if (mark.Id in state.excluded) {
                result += EffectiveGrade(mark.Id, mark.MarkText, mark.Weight, mark.Caption, mark.Date, GradeOrigin.EXCLUDED, null)
                continue
            }
            val override = state.overrides[mark.Id]
            val effectiveText = override?.markText ?: mark.MarkText
            val effectiveWeight = override?.weight ?: mark.Weight
            val origin = if (override != null) GradeOrigin.EDITED else GradeOrigin.REAL
            result += EffectiveGrade(mark.Id, effectiveText, effectiveWeight, mark.Caption, mark.Date, origin, parseGrade(effectiveText))
        }
        for (hypo in state.added) {
            result += EffectiveGrade(hypo.id, hypo.markText, hypo.weight, hypo.caption, "", GradeOrigin.ADDED, parseGrade(hypo.markText))
        }
        return result
    }

    fun predict(realMarks: List<Mark>, state: PredictionState): PredictionResult {
        val realPairs = realMarks.mapNotNull { m -> parseGrade(m.MarkText)?.let { it to m.Weight } }
        val realAverage = weightedAverage(realPairs)
        val realRoundedGrade = realAverage?.let { roundedGrade(it) }

        val effective = effectiveGrades(realMarks, state)
        val activePairs = effective
            .filter { it.origin != GradeOrigin.EXCLUDED }
            .mapNotNull { g -> g.numericValue?.let { it to g.weight } }
        val predictedAverage = weightedAverage(activePairs)
        val predictedRoundedGrade = predictedAverage?.let { roundedGrade(it) }

        val realSum = realPairs.sumOf { it.first * it.second }
        val realWeight = realPairs.sumOf { it.second }
        val (improveSuggs, grade1Suggs) = if (realAverage != null) {
            buildSuggestions(realSum, realWeight, realAverage)
        } else {
            Pair(emptyList(), emptyList())
        }

        return PredictionResult(realAverage, predictedAverage, realRoundedGrade, predictedRoundedGrade, improveSuggs, grade1Suggs)
    }

    /**
     * Grade needed at [addedWeight] to bring the average to exactly [targetAvg].
     * Returns null if the required grade is outside [1.0, 5.0] or addedWeight ≤ 0.
     */
    fun neededGrade(currentSum: Double, currentWeight: Int, targetAvg: Double, addedWeight: Int): Double? {
        if (addedWeight <= 0) return null
        val needed = (targetAvg * (currentWeight + addedWeight) - currentSum) / addedWeight
        return if (needed in 1.0..5.0) needed else null
    }

    private fun buildSuggestions(currentSum: Double, currentWeight: Int, currentAvg: Double): Pair<List<Suggestion>, List<Suggestion>> {
        val currentGrade = roundedGrade(currentAvg)
        if (currentGrade <= 1) return Pair(emptyList(), emptyList())

        val improveTarget = bandMaxAvg[currentGrade - 1] ?: return Pair(emptyList(), emptyList())
        val grade1Target = 1.49

        val improveSuggs = (1..15).mapNotNull { w ->
            neededGrade(currentSum, currentWeight, improveTarget, w)?.let { Suggestion(improveTarget, currentGrade - 1, it, w) }
        }.take(1)

        val grade1Suggs = if (improveTarget > grade1Target + 0.001) {
            (1..15).mapNotNull { w ->
                neededGrade(currentSum, currentWeight, grade1Target, w)?.let { Suggestion(grade1Target, 1, it, w) }
            }.take(1)
        } else emptyList()

        return Pair(improveSuggs, grade1Suggs)
    }

    enum class MarkSortOrder { DATE_NEWEST, DATE_OLDEST, GRADE_BEST, GRADE_WORST, WEIGHT_HIGH, WEIGHT_LOW }

    fun sortedGrades(grades: List<EffectiveGrade>, order: MarkSortOrder): List<EffectiveGrade> =
        when (order) {
            MarkSortOrder.DATE_NEWEST -> grades.sortedByDescending { it.date }
            MarkSortOrder.DATE_OLDEST -> grades.sortedBy { it.date }
            MarkSortOrder.GRADE_BEST -> grades.sortedWith(compareBy(nullsLast()) { it.numericValue })
            MarkSortOrder.GRADE_WORST -> grades.sortedWith(compareByDescending(nullsLast()) { it.numericValue })
            MarkSortOrder.WEIGHT_HIGH -> grades.sortedByDescending { it.weight }
            MarkSortOrder.WEIGHT_LOW -> grades.sortedBy { it.weight }
        }
}
