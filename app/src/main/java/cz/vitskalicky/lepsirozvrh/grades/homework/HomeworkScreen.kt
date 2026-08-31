package cz.vitskalicky.lepsirozvrh.grades.homework

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

private enum class HwSortOrder { DATE_NEWEST, DATE_OLDEST, SUBJECT }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeworkScreen(viewModel: HomeworkViewModel, onBack: () -> Unit) {
    val allItems by viewModel.items.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    var sortOrder by remember { mutableStateOf(HwSortOrder.DATE_NEWEST) }

    val sorted = remember(allItems, sortOrder) {
        when (sortOrder) {
            HwSortOrder.DATE_NEWEST -> allItems.sortedByDescending { it.date }
            HwSortOrder.DATE_OLDEST -> allItems.sortedBy { it.date }
            HwSortOrder.SUBJECT -> allItems.sortedWith(compareBy({ it.subjectName }, { it.date }))
        }
    }

    // Grouped: date → items (null date goes to a "No date" bucket)
    val grouped: List<Pair<LocalDate?, List<HomeworkItem>>> = remember(sorted, sortOrder) {
        if (sortOrder == HwSortOrder.SUBJECT) {
            // group by subject name, not date
            val bySubject = sorted.groupBy { it.subjectName }
            bySubject.entries.map { null to it.value }
        } else {
            val byDate = sorted.groupBy { it.date }
            byDate.entries.map { it.key to it.value }
        }
    }

    val dateFmt = remember { DateTimeFormat.fullDate() }
    val timeFmt = remember { DateTimeFormat.shortTime() }

    LepsirozvrhTheme(tintStatusBar = true, hasAppBar = true) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.homework_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadHomework() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                when {
                    isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    allItems.isEmpty() -> EmptyState()
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        item {
                            SortRow(sortOrder) { sortOrder = it }
                        }
                        if (sortOrder == HwSortOrder.SUBJECT) {
                            grouped.forEach { (_, subjectItems) ->
                                val subject = subjectItems.first()
                                stickyHeader {
                                    SubjectHeader(subject.subjectName, subject.subjectAbbrev)
                                }
                                items(subjectItems) { hw ->
                                    HomeworkCard(hw, timeFmt = timeFmt, showSubjectChip = false)
                                }
                            }
                        } else {
                            grouped.forEach { (date, dateItems) ->
                                stickyHeader {
                                    DateHeader(date?.toString(dateFmt) ?: stringResource(R.string.homework_no_date))
                                }
                                items(dateItems) { hw ->
                                    HomeworkCard(hw, timeFmt = timeFmt, showSubjectChip = true)
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
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.homework_empty),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.homework_empty_hint),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun DateHeader(label: String) {
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SubjectHeader(subjectName: String, abbrev: String) {
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    abbrev,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary
                )
            }
            Text(
                subjectName,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SortRow(current: HwSortOrder, onSelect: (HwSortOrder) -> Unit) {
    val options = listOf(
        HwSortOrder.DATE_NEWEST to R.string.homework_sort_newest,
        HwSortOrder.DATE_OLDEST to R.string.homework_sort_oldest,
        HwSortOrder.SUBJECT to R.string.homework_sort_subject
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
private fun HomeworkCard(
    hw: HomeworkItem,
    timeFmt: org.joda.time.format.DateTimeFormatter,
    showSubjectChip: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (showSubjectChip) {
                Surface(
                    color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                ) {
                    Text(
                        hw.subjectAbbrev.ifBlank { "?" },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(hw.description, style = MaterialTheme.typography.body2)
                if (hw.lessonBeginTime != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        hw.lessonBeginTime.toString(timeFmt),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
