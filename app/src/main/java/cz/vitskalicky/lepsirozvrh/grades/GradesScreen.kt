package cz.vitskalicky.lepsirozvrh.grades

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.Mark
import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.MarkSubject
import cz.vitskalicky.lepsirozvrh.grades.homework.HomeworkActivity
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import kotlinx.coroutines.delay
import org.joda.time.format.DateTimeFormat

@Composable
fun GradesScreen(viewModel: GradesViewModel, onBack: () -> Unit) {
    val subjects by viewModel.subjects.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val sortOrder by viewModel.subjectSortOrder.observeAsState(SubjectSortOrder.NAME)
    val viewMode by viewModel.gradesViewMode.observeAsState(GradesViewMode.BY_SUBJECT)
    val newMarkIds by viewModel.newMarkIds.observeAsState(emptySet())

    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    val selectedSubject = selectedSubjectId?.let { id -> subjects.firstOrNull { it.Subject.Id == id } }

    // Ephemeral prediction state — resets whenever selectedSubjectId changes
    var predictionState by remember { mutableStateOf(PredictionState()) }
    LaunchedEffect(selectedSubjectId) { predictionState = PredictionState() }

    // Navigate to subject detail
    if (selectedSubject != null) {
        BackHandler { selectedSubjectId = null }
        // Mark all this subject's grades as seen after 3 seconds
        LaunchedEffect(selectedSubject.Subject.Id) {
            delay(3000)
            viewModel.markSubjectAsSeen(selectedSubject)
        }
        SubjectDetailScreen(
            subject = selectedSubject,
            predictionState = predictionState,
            newMarkIds = newMarkIds,
            onBack = { selectedSubjectId = null },
            onUpdateState = { update -> predictionState = update(predictionState) },
            onReset = { predictionState = PredictionState() }
        )
        return
    }

    val sortedSubjects = remember(subjects, sortOrder) {
        when (sortOrder) {
            SubjectSortOrder.NAME -> subjects.sortedBy { it.Subject.Name.ifBlank { it.Subject.Abbrev } }
            SubjectSortOrder.AVERAGE_BEST -> subjects.sortedBy {
                GradePredictor.predict(it.Marks, PredictionState()).realAverage ?: Double.MAX_VALUE
            }
            SubjectSortOrder.AVERAGE_WORST -> subjects.sortedByDescending {
                GradePredictor.predict(it.Marks, PredictionState()).realAverage ?: Double.MIN_VALUE
            }
        }
    }

    // All marks sorted by date for date view
    data class DateMark(val subject: MarkSubject, val mark: Mark)
    val marksByDate = remember(subjects) {
        subjects.flatMap { s -> s.Marks.map { DateMark(s, it) } }
            .sortedByDescending { it.mark.Date }
    }

    val context = LocalContext.current
    val dateFmt = remember { DateTimeFormat.mediumDate() }

    LepsirozvrhTheme(tintStatusBar = true, hasAppBar = true) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.grades_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { context.startActivity(Intent(context, HomeworkActivity::class.java)) }) {
                            Icon(Icons.Default.Assignment, contentDescription = stringResource(R.string.homework_title))
                        }
                        IconButton(onClick = { viewModel.loadGrades() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                when {
                    isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null -> Text(error ?: "", Modifier.align(Alignment.Center))
                    subjects.isEmpty() -> Text(stringResource(R.string.grades_no_grades), Modifier.align(Alignment.Center))
                    else -> LazyColumn(contentPadding = PaddingValues(8.dp, 4.dp)) {
                        item {
                            ViewModeRow(current = viewMode, onSelect = { viewModel.gradesViewMode.value = it })
                        }
                        when (viewMode) {
                            GradesViewMode.BY_SUBJECT -> {
                                item {
                                    SubjectSortRow(
                                        current = sortOrder,
                                        onSelect = { viewModel.subjectSortOrder.value = it }
                                    )
                                }
                                items(sortedSubjects) { subject ->
                                    SubjectCard(
                                        subject = subject,
                                        hasNew = subject.Marks.any { it.Id in newMarkIds },
                                        onClick = { selectedSubjectId = subject.Subject.Id }
                                    )
                                }
                            }
                            GradesViewMode.BY_DATE -> {
                                items(marksByDate) { dm ->
                                    DateMarkRow(dm.subject, dm.mark, dateFmt)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeRow(current: GradesViewMode, onSelect: (GradesViewMode) -> Unit) {
    val options = listOf(
        GradesViewMode.BY_SUBJECT to R.string.grades_view_by_subject,
        GradesViewMode.BY_DATE to R.string.grades_view_by_date,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (mode, labelRes) ->
            val selected = current == mode
            Surface(
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                contentColor = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.25f)
                ),
                modifier = Modifier.clickable { onSelect(mode) }
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
private fun SubjectSortRow(current: SubjectSortOrder, onSelect: (SubjectSortOrder) -> Unit) {
    val options = listOf(
        SubjectSortOrder.NAME to R.string.grades_sort_name,
        SubjectSortOrder.AVERAGE_BEST to R.string.grades_sort_best,
        SubjectSortOrder.AVERAGE_WORST to R.string.grades_sort_worst,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
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
private fun DateMarkRow(subject: MarkSubject, mark: Mark, dateFmt: org.joda.time.format.DateTimeFormatter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                subject.Subject.Abbrev,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.caption
            )
        }
        Column(Modifier.weight(1f)) {
            Text(mark.Caption.ifBlank { "—" }, style = MaterialTheme.typography.body2)
            Text(
                mark.Date,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
        Text("×${mark.Weight}", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
        if (mark.MarkText.isNotBlank()) {
            Surface(
                color = MaterialTheme.colors.primary.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    mark.MarkText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SubjectCard(subject: MarkSubject, hasNew: Boolean = false, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = 2.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject.Subject.Name.ifBlank { subject.Subject.Abbrev },
                        fontWeight = FontWeight.Bold
                    )
                    if (hasNew) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colors.secondary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "NEW",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.overline,
                                color = MaterialTheme.colors.onSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                val avgText = GradePredictor.weightedAverage(
                    subject.Marks.mapNotNull { m -> GradePredictor.parseGrade(m.MarkText)?.let { it to m.Weight } }
                )?.let { "Ø ${"%.2f".format(it)}" }
                if (avgText != null) {
                    Text(
                        text = avgText,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MarkRow(mark: Mark) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = mark.Caption.ifBlank { "—" },
                style = MaterialTheme.typography.body2,
                fontWeight = if (mark.IsNew) FontWeight.Bold else FontWeight.Normal
            )
            if (mark.PointsText != null) {
                Text(
                    text = mark.PointsText,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (mark.MarkText.isNotBlank()) {
            val color = if (mark.IsNew) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            Text(
                text = mark.MarkText,
                fontWeight = if (mark.IsNew) FontWeight.Bold else FontWeight.Normal,
                color = color,
                fontSize = 16.sp
            )
        }
    }
}
