package cz.vitskalicky.lepsirozvrh.grades

import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.Mark
import org.junit.Assert.*
import org.junit.Test

class GradePredictorTest {

    // region parseGrade

    @Test
    fun `parseGrade plain integers`() {
        assertEquals(1.0, GradePredictor.parseGrade("1")!!, 0.001)
        assertEquals(2.0, GradePredictor.parseGrade("2")!!, 0.001)
        assertEquals(3.0, GradePredictor.parseGrade("3")!!, 0.001)
        assertEquals(4.0, GradePredictor.parseGrade("4")!!, 0.001)
        assertEquals(5.0, GradePredictor.parseGrade("5")!!, 0.001)
    }

    @Test
    fun `parseGrade modified plus`() {
        assertEquals(1.5, GradePredictor.parseGrade("2+")!!, 0.001)
        assertEquals(2.5, GradePredictor.parseGrade("3+")!!, 0.001)
    }

    @Test
    fun `parseGrade modified minus`() {
        assertEquals(2.5, GradePredictor.parseGrade("2-")!!, 0.001)
        assertEquals(3.5, GradePredictor.parseGrade("3-")!!, 0.001)
    }

    @Test
    fun `parseGrade non-numeric returns null`() {
        assertNull(GradePredictor.parseGrade("OM"))
        assertNull(GradePredictor.parseGrade("N"))
        assertNull(GradePredictor.parseGrade(""))
        assertNull(GradePredictor.parseGrade(" "))
    }

    @Test
    fun `parseGrade trims whitespace`() {
        assertEquals(2.0, GradePredictor.parseGrade(" 2 ")!!, 0.001)
    }

    // endregion

    // region weightedAverage

    @Test
    fun `weightedAverage empty list returns null`() {
        assertNull(GradePredictor.weightedAverage(emptyList()))
    }

    @Test
    fun `weightedAverage single grade`() {
        val result = GradePredictor.weightedAverage(listOf(2.0 to 1))
        assertEquals(2.0, result!!, 0.001)
    }

    @Test
    fun `weightedAverage equal weights`() {
        val result = GradePredictor.weightedAverage(listOf(1.0 to 1, 3.0 to 1))
        assertEquals(2.0, result!!, 0.001)
    }

    @Test
    fun `weightedAverage unequal weights`() {
        // (1*2 + 3*1) / 3 = 5/3 = 1.667
        val result = GradePredictor.weightedAverage(listOf(1.0 to 2, 3.0 to 1))
        assertEquals(5.0 / 3.0, result!!, 0.001)
    }

    // endregion

    // region roundedGrade boundaries

    @Test
    fun `roundedGrade 1_49 is grade 1`() {
        assertEquals(1, GradePredictor.roundedGrade(1.49))
    }

    @Test
    fun `roundedGrade 1_50 is grade 2`() {
        assertEquals(2, GradePredictor.roundedGrade(1.50))
    }

    @Test
    fun `roundedGrade 2_49 is grade 2`() {
        assertEquals(2, GradePredictor.roundedGrade(2.49))
    }

    @Test
    fun `roundedGrade 2_50 is grade 3`() {
        assertEquals(3, GradePredictor.roundedGrade(2.50))
    }

    @Test
    fun `roundedGrade 3_49 is grade 3`() {
        assertEquals(3, GradePredictor.roundedGrade(3.49))
    }

    @Test
    fun `roundedGrade 3_50 is grade 4`() {
        assertEquals(4, GradePredictor.roundedGrade(3.50))
    }

    @Test
    fun `roundedGrade 4_49 is grade 4`() {
        assertEquals(4, GradePredictor.roundedGrade(4.49))
    }

    @Test
    fun `roundedGrade 4_50 is grade 5`() {
        assertEquals(5, GradePredictor.roundedGrade(4.50))
    }

    // endregion

    // region effectiveGrades

    private fun mark(id: String, markText: String, weight: Int = 1, date: String = "2026-01-01") =
        Mark(id, "subj", "Caption $id", markText, weight, date, false, null)

    @Test
    fun `effectiveGrades empty state returns real marks as REAL`() {
        val marks = listOf(mark("a", "2"), mark("b", "3"))
        val state = PredictionState()
        val effective = GradePredictor.effectiveGrades(marks, state)
        assertEquals(2, effective.size)
        assertEquals(GradePredictor.GradeOrigin.REAL, effective[0].origin)
        assertEquals(GradePredictor.GradeOrigin.REAL, effective[1].origin)
    }

    @Test
    fun `effectiveGrades excluded mark becomes EXCLUDED with null numeric`() {
        val marks = listOf(mark("a", "2"))
        val state = PredictionState(excluded = setOf("a"))
        val effective = GradePredictor.effectiveGrades(marks, state)
        assertEquals(GradePredictor.GradeOrigin.EXCLUDED, effective[0].origin)
        assertNull(effective[0].numericValue)
    }

    @Test
    fun `effectiveGrades override changes markText and origin`() {
        val marks = listOf(mark("a", "3", weight = 1))
        val state = PredictionState(overrides = mapOf("a" to MarkOverride(markText = "1", weight = 2)))
        val effective = GradePredictor.effectiveGrades(marks, state)
        assertEquals(GradePredictor.GradeOrigin.EDITED, effective[0].origin)
        assertEquals("1", effective[0].markText)
        assertEquals(2, effective[0].weight)
        assertEquals(1.0, effective[0].numericValue!!, 0.001)
    }

    @Test
    fun `effectiveGrades hypothetical mark appended as ADDED`() {
        val marks = listOf(mark("a", "2"))
        val hypo = HypotheticalMark(id = "hypo-1", markText = "1", weight = 2, caption = "Test")
        val state = PredictionState(added = listOf(hypo))
        val effective = GradePredictor.effectiveGrades(marks, state)
        assertEquals(2, effective.size)
        assertEquals(GradePredictor.GradeOrigin.ADDED, effective[1].origin)
        assertEquals("hypo-1", effective[1].id)
        assertEquals(1.0, effective[1].numericValue!!, 0.001)
    }

    // endregion

    // region predict

    @Test
    fun `predict no state returns same real and predicted averages`() {
        val marks = listOf(mark("a", "2", weight = 1), mark("b", "4", weight = 1))
        val result = GradePredictor.predict(marks, PredictionState())
        assertEquals(3.0, result.realAverage!!, 0.001)
        assertEquals(3.0, result.predictedAverage!!, 0.001)
        assertEquals(0.0, result.delta!!, 0.001)
    }

    @Test
    fun `predict with excluded mark changes predicted average`() {
        val marks = listOf(mark("a", "1", weight = 1), mark("b", "5", weight = 1))
        val state = PredictionState(excluded = setOf("b"))
        val result = GradePredictor.predict(marks, state)
        assertEquals(3.0, result.realAverage!!, 0.001)
        assertEquals(1.0, result.predictedAverage!!, 0.001)
        assertEquals(2.0, result.delta!!, 0.001)
    }

    @Test
    fun `predict with hypothetical mark updates predicted average`() {
        val marks = listOf(mark("a", "3", weight = 1))
        val hypo = HypotheticalMark(id = "h", markText = "1", weight = 1)
        val state = PredictionState(added = listOf(hypo))
        val result = GradePredictor.predict(marks, state)
        assertEquals(3.0, result.realAverage!!, 0.001)
        assertEquals(2.0, result.predictedAverage!!, 0.001)
    }

    @Test
    fun `predict non-numeric marks excluded from average`() {
        val marks = listOf(mark("a", "OM", weight = 1), mark("b", "2", weight = 1))
        val result = GradePredictor.predict(marks, PredictionState())
        assertEquals(2.0, result.realAverage!!, 0.001)
        assertEquals(2.0, result.predictedAverage!!, 0.001)
    }

    // endregion

    // region neededGrade

    @Test
    fun `neededGrade computes correct value using 1_49 boundary`() {
        // current sum = 2.0, weight = 1, targetAvg = 1.49, addedWeight = 2
        // needed = (1.49*(1+2) - 2.0) / 2 = (4.47 - 2.0) / 2 = 1.235
        val needed = GradePredictor.neededGrade(2.0, 1, 1.49, 2)
        assertEquals(1.235, needed!!, 0.001)
    }

    @Test
    fun `neededGrade returns null when unreachable below 1_0`() {
        // avg=5.0, need grade 1 (1.49), with weight 1:
        // needed = (1.49*2 - 5.0) / 1 = -2.02 → outside 1.0..5.0
        val needed = GradePredictor.neededGrade(5.0, 1, 1.0, 1)
        assertNull(needed)
    }

    @Test
    fun `neededGrade returns null for zero addedWeight`() {
        assertNull(GradePredictor.neededGrade(3.0, 1, 2.49, 0))
    }

    // endregion

    // region sortedGrades

    private fun grade(id: String, numericValue: Double?, weight: Int = 1, date: String = "2026-01-01") =
        GradePredictor.EffectiveGrade(id, numericValue?.toString() ?: "OM", weight, "", date, GradePredictor.GradeOrigin.REAL, numericValue)

    @Test
    fun `sortedGrades DATE_NEWEST`() {
        val grades = listOf(grade("a", 1.0, date = "2026-01-01"), grade("b", 2.0, date = "2026-03-01"))
        val sorted = GradePredictor.sortedGrades(grades, GradePredictor.MarkSortOrder.DATE_NEWEST)
        assertEquals("b", sorted[0].id)
    }

    @Test
    fun `sortedGrades GRADE_BEST puts lowest numeric first`() {
        val grades = listOf(grade("a", 3.0), grade("b", 1.0), grade("c", null))
        val sorted = GradePredictor.sortedGrades(grades, GradePredictor.MarkSortOrder.GRADE_BEST)
        assertEquals("b", sorted[0].id)
        assertEquals("a", sorted[1].id)
        assertEquals("c", sorted[2].id)
    }

    @Test
    fun `sortedGrades WEIGHT_HIGH`() {
        val grades = listOf(grade("a", 2.0, weight = 1), grade("b", 2.0, weight = 3))
        val sorted = GradePredictor.sortedGrades(grades, GradePredictor.MarkSortOrder.WEIGHT_HIGH)
        assertEquals("b", sorted[0].id)
    }

    // endregion

    // region buildSuggestions (via predict)

    @Test
    fun `already grade 1 has no improve suggestions`() {
        // avg = 1.3 → roundedGrade = 1 → no suggestions
        val result = GradePredictor.predict(listOf(mark("a", "1")), PredictionState())
        // avg = 1.0, roundedGrade = 1
        assertTrue(result.realImproveSuggestions.isEmpty())
        assertTrue(result.realGrade1Suggestions.isEmpty())
    }

    @Test
    fun `grade 2 improve uses 1_49 target and grade1Suggs empty`() {
        // avg = 2.0, roundedGrade = 2, improve target = 1.49 (grade 1)
        // Since improve already targets grade 1, grade1Suggs is empty
        val result = GradePredictor.predict(listOf(mark("a", "2")), PredictionState())
        assertEquals(2, result.realRoundedGrade)
        val improve = result.realImproveSuggestions.firstOrNull { it.addedWeight == 2 }
        assertNotNull(improve)
        assertEquals(1.49, improve!!.targetMaxAvg, 0.001)
        assertEquals(1, improve.targetRoundedGrade)
        assertTrue(result.realGrade1Suggestions.isEmpty()) // same target, so empty
    }

    @Test
    fun `grade 2 w1 improve is impossible, w2 achievable`() {
        // avg=2.0, sum=2.0, weight=1
        // to reach 1.49 with w1: (1.49*2-2.0)/1 = 0.98 < 1.0 → null
        // to reach with w2: (1.49*3-2.0)/2 = (4.47-2.0)/2 = 1.235 → achievable
        val result = GradePredictor.predict(listOf(mark("a", "2")), PredictionState())
        assertNull(result.realImproveSuggestions.firstOrNull { it.addedWeight == 1 })
        assertNotNull(result.realImproveSuggestions.firstOrNull { it.addedWeight == 2 })
    }

    @Test
    fun `grade 3 improve uses 2_49 and grade1Suggs uses 1_49 separately`() {
        // avg = 3.0, roundedGrade = 3
        // improveTarget = 2.49 (grade 2)
        // neededGrade(3.0, 1, 2.49, 1) = (2.49*2 - 3.0)/1 = (4.98-3.0)/1 = 1.98
        val result = GradePredictor.predict(listOf(mark("a", "3")), PredictionState())
        assertEquals(3, result.realRoundedGrade)

        val improve = result.realImproveSuggestions.firstOrNull { it.addedWeight == 1 }
        assertNotNull(improve)
        assertEquals(2.49, improve!!.targetMaxAvg, 0.001)
        assertEquals(2, improve.targetRoundedGrade)
        assertEquals(1.98, improve.neededGrade, 0.001)

        val grade1 = result.realGrade1Suggestions.firstOrNull()
        assertNotNull(grade1)
        assertEquals(1.49, grade1!!.targetMaxAvg, 0.001)
        assertEquals(1, grade1.targetRoundedGrade)
    }

    @Test
    fun `weighted prediction with added hypothetical grade`() {
        // real marks: "2" w1 (sum=2, weight=1, avg=2.0)
        // add "1" w2 hypothetical: (2+2)/3=1.333 predicted avg
        val hypo = HypotheticalMark(id = "h", markText = "1", weight = 2)
        val result = GradePredictor.predict(listOf(mark("a", "2")), PredictionState(added = listOf(hypo)))
        assertEquals(2.0, result.realAverage!!, 0.001)
        assertEquals(4.0 / 3.0, result.predictedAverage!!, 0.001)
        assertEquals(1, result.predictedRoundedGrade)
    }

    @Test
    fun `target after current rounded 3 should use 2_49`() {
        // Explicit: grade 3 → target 2.49 not 2.5
        val result = GradePredictor.predict(listOf(mark("a", "3")), PredictionState())
        val firstImprove = result.realImproveSuggestions.firstOrNull()
        assertNotNull(firstImprove)
        assertEquals(2.49, firstImprove!!.targetMaxAvg, 0.001)
    }

    @Test
    fun `target for grade 1 should use 1_49`() {
        // Explicit: grade 3 → grade1Suggs uses 1.49 not 1.5
        val result = GradePredictor.predict(listOf(mark("a", "3")), PredictionState())
        val firstGrade1 = result.realGrade1Suggestions.firstOrNull()
        assertNotNull(firstGrade1)
        assertEquals(1.49, firstGrade1!!.targetMaxAvg, 0.001)
    }

    // endregion
}
