package cz.vitskalicky.lepsirozvrh.grades

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.Mark
import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.MarkSubject
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

/** Worst Czech grade label you can receive and still satisfy ≤ [needed]. */
private fun worstAcceptableGrade(needed: Double): String {
    val steps = listOf(5.0 to "5", 4.5 to "4-", 4.0 to "4", 3.5 to "3-",
                       3.0 to "3", 2.5 to "2-", 2.0 to "2", 1.5 to "2+", 1.0 to "1")
    return steps.firstOrNull { it.first <= needed }?.second ?: "1"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubjectDetailScreen(
    subject: MarkSubject,
    predictionState: PredictionState,
    newMarkIds: Set<String> = emptySet(),
    onBack: () -> Unit,
    onUpdateState: (update: (PredictionState) -> PredictionState) -> Unit,
    onReset: () -> Unit
) {
    val marks = subject.Marks

    var markSortOrder by remember { mutableStateOf(GradePredictor.MarkSortOrder.DATE_NEWEST) }
    var editingTarget by remember { mutableStateOf<EditTarget?>(null) }

    val effective = remember(marks, predictionState) {
        GradePredictor.effectiveGrades(marks, predictionState)
    }
    val prediction = remember(marks, predictionState) {
        GradePredictor.predict(marks, predictionState)
    }

    // Split into real/edited/excluded and hypothetical sections (ADDED always at bottom)
    val sorted = remember(effective, markSortOrder) {
        GradePredictor.sortedGrades(effective, markSortOrder)
    }
    val realItems = remember(sorted) { sorted.filter { it.origin != GradePredictor.GradeOrigin.ADDED } }
    val addedItems = remember(sorted) { sorted.filter { it.origin == GradePredictor.GradeOrigin.ADDED } }

    LepsirozvrhTheme(tintStatusBar = true, hasAppBar = true) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(subject.Subject.Name.ifBlank { subject.Subject.Abbrev }) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (predictionState.isActive) {
                            IconButton(onClick = onReset) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.predict_reset))
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { editingTarget = EditTarget.AddHypo },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.predict_add_grade))
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Full prediction card (scrolls away)
                if (marks.isNotEmpty()) {
                    item {
                        PredictionCard(
                            marks = marks,
                            prediction = prediction,
                            isActive = predictionState.isActive
                        )
                    }
                }

                // Sticky compact summary that stays visible while scrolling
                stickyHeader {
                    CompactSummaryBar(
                        prediction = prediction,
                        isActive = predictionState.isActive
                    )
                }

                // Sort row
                item {
                    SortRow(current = markSortOrder, onSelect = { markSortOrder = it })
                }

                // Real / edited / excluded marks
                items(realItems) { grade ->
                    GradeRow(
                        grade = grade,
                        isNew = grade.id in newMarkIds,
                        onToggleExclude = {
                            if (grade.origin == GradePredictor.GradeOrigin.EXCLUDED) {
                                onUpdateState { it.withIncluded(grade.id) }
                            } else {
                                onUpdateState { it.withExcluded(grade.id) }
                            }
                        },
                        onEdit = {
                            editingTarget = EditTarget.EditReal(grade.id, grade.markText, grade.weight)
                        }
                    )
                }

                // Hypothetical / added section
                if (addedItems.isNotEmpty()) {
                    stickyHeader {
                        Surface(
                            color = MaterialTheme.colors.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.predict_section_hypothetical),
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.secondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(addedItems) { grade ->
                        GradeRow(
                            grade = grade,
                            isNew = false,
                            onToggleExclude = { /* ADDED marks cannot be excluded */ },
                            onEdit = {
                                editingTarget = EditTarget.EditHypo(grade.id, grade.markText, grade.weight, grade.caption)
                            }
                        )
                    }
                }
            }
        }
    }

    editingTarget?.let { target ->
        when (target) {
            is EditTarget.EditReal -> GradeEditDialog(
                title = stringResource(R.string.predict_edit_grade_title),
                initialMarkText = target.markText,
                initialWeight = target.weight.toString(),
                showCaption = false,
                showDelete = true,
                onDismiss = { editingTarget = null },
                onSave = { text, weight, _ ->
                    if (text == null) {
                        onUpdateState { it.withoutOverride(target.markId) }
                    } else {
                        onUpdateState { it.withOverride(target.markId, MarkOverride(markText = text, weight = weight)) }
                    }
                    editingTarget = null
                },
                onDelete = {
                    onUpdateState { it.withoutOverride(target.markId) }
                    editingTarget = null
                }
            )
            is EditTarget.EditHypo -> GradeEditDialog(
                title = stringResource(R.string.predict_edit_grade_title),
                initialMarkText = target.markText,
                initialWeight = target.weight.toString(),
                initialCaption = target.caption,
                showCaption = true,
                showDelete = true,
                onDismiss = { editingTarget = null },
                onSave = { text, weight, caption ->
                    if (text != null) {
                        onUpdateState { it.withUpdatedAdded(target.hypoId, text, weight, caption ?: "") }
                    }
                    editingTarget = null
                },
                onDelete = {
                    onUpdateState { it.withRemovedAdded(target.hypoId) }
                    editingTarget = null
                }
            )
            EditTarget.AddHypo -> GradeEditDialog(
                title = stringResource(R.string.predict_add_grade_title),
                initialMarkText = "",
                initialWeight = "1",
                initialCaption = "",
                showCaption = true,
                showDelete = false,
                onDismiss = { editingTarget = null },
                onSave = { text, weight, caption ->
                    if (text != null) {
                        onUpdateState { it.withAdded(HypotheticalMark(markText = text, weight = weight, caption = caption ?: "")) }
                    }
                    editingTarget = null
                },
                onDelete = { editingTarget = null }
            )
        }
    }
}

// region helpers

private sealed class EditTarget {
    data class EditReal(val markId: String, val markText: String, val weight: Int) : EditTarget()
    data class EditHypo(val hypoId: String, val markText: String, val weight: Int, val caption: String) : EditTarget()
    object AddHypo : EditTarget()
}

@Composable
private fun CompactSummaryBar(
    prediction: GradePredictor.PredictionResult,
    isActive: Boolean
) {
    val realAvg = prediction.realAverage ?: return
    val displayedRealAverage = "%.2f".format(realAvg)
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                displayedRealAverage,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (isActive && prediction.predictedAverage != null && prediction.predictedAverage != realAvg) {
                val delta = prediction.delta
                Text("→", color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                Text("%.2f".format(prediction.predictedAverage), color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
                if (delta != null && delta != 0.0) {
                    val improved = delta > 0
                    Text(
                        "%s%.2f".format(if (improved) "↑" else "↓", kotlin.math.abs(delta)),
                        fontSize = 13.sp,
                        color = if (improved) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
private fun PredictionCard(
    marks: List<Mark>,
    prediction: GradePredictor.PredictionResult,
    isActive: Boolean
) {
    val realPairs = remember(marks) {
        marks.mapNotNull { m -> GradePredictor.parseGrade(m.MarkText)?.let { it to m.Weight } }
    }
    val realSum = realPairs.sumOf { it.first * it.second }
    val realWeight = realPairs.sumOf { it.second }

    val currentGrade = prediction.realRoundedGrade
    val bandTargets = mapOf(2 to 1.49, 3 to 2.49, 4 to 3.49, 5 to 4.49)
    val improveTarget = if (currentGrade != null && currentGrade > 1) bandTargets[currentGrade] else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        elevation = 2.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Real avg column
                Column {
                    Text(
                        stringResource(R.string.predict_real_avg),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        prediction.realAverage?.let { "%.2f".format(it) } ?: "—",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                // Predicted avg + delta (only when prediction is active)
                if (isActive && prediction.predictedAverage != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.predict_predicted),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "%.2f".format(prediction.predictedAverage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colors.primary
                            )
                        }
                        val delta = prediction.delta
                        if (delta != null && delta != 0.0) {
                            val improved = delta > 0
                            Text(
                                "%s%.2f".format(if (improved) "↑" else "↓", kotlin.math.abs(delta)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (improved) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }

            // What you need hints (no buttons — computed automatically for grade 1)
            if (improveTarget != null) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(6.dp))

                // Helper: one hint line for a target band
                @Composable
                fun HintLine(targetGrade: Int, targetAvg: Double) {
                    val exactW = GradePredictor.exactWeightNeeded(realSum, realWeight, targetAvg, 1.0)
                    val minW   = exactW?.let { kotlin.math.ceil(it).toInt() }
                    val text = when {
                        exactW == null -> "→ grade $targetGrade: not reachable with grade 1"
                        minW != null   -> "→ grade $targetGrade: add 1 · w≥$minW (exact ${"%.2f".format(exactW)})"
                        else           -> "→ grade $targetGrade: —"
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                val improveGrade = (currentGrade ?: 2) - 1
                HintLine(improveGrade, improveTarget)
                if (improveTarget > 1.49 + 0.001) {
                    HintLine(1, 1.49)
                }
            }
        }
    }
}

@Composable
private fun SortRow(current: GradePredictor.MarkSortOrder, onSelect: (GradePredictor.MarkSortOrder) -> Unit) {
    val options = listOf(
        GradePredictor.MarkSortOrder.DATE_NEWEST to R.string.predict_sort_newest,
        GradePredictor.MarkSortOrder.DATE_OLDEST to R.string.predict_sort_oldest,
        GradePredictor.MarkSortOrder.GRADE_BEST to R.string.predict_sort_best,
        GradePredictor.MarkSortOrder.GRADE_WORST to R.string.predict_sort_worst,
        GradePredictor.MarkSortOrder.WEIGHT_HIGH to R.string.predict_sort_heavy,
        GradePredictor.MarkSortOrder.WEIGHT_LOW to R.string.predict_sort_light,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (order, labelRes) ->
            val selected = current == order
            Surface(
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                contentColor = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.25f)
                ),
                modifier = Modifier.clickable { onSelect(order) }
            ) {
                Text(
                    stringResource(labelRes),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private fun GradeRow(
    grade: GradePredictor.EffectiveGrade,
    isNew: Boolean = false,
    onToggleExclude: () -> Unit,
    onEdit: () -> Unit
) {
    val isExcluded = grade.origin == GradePredictor.GradeOrigin.EXCLUDED
    val isHypo = grade.origin == GradePredictor.GradeOrigin.ADDED
    val isEdited = grade.origin == GradePredictor.GradeOrigin.EDITED

    val alpha = if (isExcluded) 0.4f else 1f
    val gradeColor = when {
        isHypo -> MaterialTheme.colors.secondary
        isEdited -> MaterialTheme.colors.primary
        else -> MaterialTheme.colors.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isHypo) {
            Checkbox(
                checked = !isExcluded,
                onCheckedChange = { onToggleExclude() },
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(36.dp))
        }

        Column(Modifier.weight(1f)) {
            val captionDecoration = if (isExcluded) TextDecoration.LineThrough else TextDecoration.None
            Text(
                grade.caption.ifBlank { "—" },
                style = MaterialTheme.typography.body2,
                textDecoration = captionDecoration,
                color = MaterialTheme.colors.onSurface.copy(alpha = alpha)
            )
            if (isHypo) {
                Text(
                    stringResource(R.string.predict_origin_added),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.secondary.copy(alpha = 0.8f),
                    fontStyle = FontStyle.Italic
                )
            } else if (isEdited) {
                Text(
                    stringResource(R.string.predict_origin_edited),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.8f),
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Text(
            "×${grade.weight}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = alpha * 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        if (isNew) {
            Surface(
                color = MaterialTheme.colors.secondary.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    "NEW",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.overline,
                    color = MaterialTheme.colors.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (grade.markText.isNotBlank()) {
            val isNonNumeric = grade.numericValue == null && grade.origin != GradePredictor.GradeOrigin.EXCLUDED
            if (isNonNumeric) {
                Text(
                    grade.markText,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f * alpha)
                )
            } else {
                Surface(
                    color = gradeColor.copy(alpha = 0.15f * alpha),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        grade.markText,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = gradeColor.copy(alpha = alpha)
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.predict_edit_grade_title),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun GradeEditDialog(
    title: String,
    initialMarkText: String,
    initialWeight: String,
    initialCaption: String = "",
    showCaption: Boolean,
    showDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (text: String?, weight: Int, caption: String?) -> Unit,
    onDelete: () -> Unit
) {
    var markText by remember { mutableStateOf(initialMarkText) }
    var weightText by remember { mutableStateOf(initialWeight) }
    var caption by remember { mutableStateOf(initialCaption) }
    var markError by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, elevation = 8.dp) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = markText,
                    onValueChange = { markText = it; markError = false },
                    label = { Text(stringResource(R.string.predict_grade_label)) },
                    isError = markError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it; weightError = false },
                    label = { Text(stringResource(R.string.predict_weight_label)) },
                    isError = weightError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (showCaption) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text(stringResource(R.string.predict_caption_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showDelete) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.predict_delete))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.predict_cancel)) }
                    Button(onClick = {
                        val parsedWeight = weightText.trim().toIntOrNull()
                        if (parsedWeight == null || parsedWeight < 1) { weightError = true; return@Button }
                        val trimmed = markText.trim()
                        if (trimmed.isEmpty()) { markError = true; return@Button }
                        if (GradePredictor.parseGrade(trimmed) == null && !trimmed.equals("OM", ignoreCase = true)) {
                            markError = true; return@Button
                        }
                        onSave(trimmed, parsedWeight, if (showCaption) caption else null)
                    }) {
                        Text(stringResource(R.string.predict_save))
                    }
                }
            }
        }
    }
}

// endregion
