package cz.vitskalicky.lepsirozvrh.grades.homework

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.database.PersonalTask
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.prefs
import org.joda.time.Days
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.joda.time.format.DateTimeFormat

/** The last one is really a filter, but it belongs in the same row of chips. */
private enum class HwSortOrder { DATE_NEWEST, DATE_OLDEST, SUBJECT, PAST_DUE }

/** Overdue means dated strictly before today and not already ticked off. */
private fun HomeworkItem.isPastDue(doneMap: Map<String, Boolean>): Boolean =
    date != null && date.isBefore(LocalDate.now()) && !doneWith(doneMap)

private fun PersonalTask.isPastDue(): Boolean =
    dueDate != null && dueDate.isBefore(LocalDate.now()) && !isDone
private enum class HomeworkTab { BOTH, HOMEWORK, TASKS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeworkScreen(viewModel: HomeworkViewModel, onBack: () -> Unit) {
    val allItems by viewModel.items.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val personalTasks by viewModel.personalTasks.observeAsState(emptyList())

    var selectedTab by remember { mutableStateOf(HomeworkTab.BOTH) }
    // earliest first: what is due soonest is what matters, so it belongs at the top
    var sortOrder by remember { mutableStateOf(HwSortOrder.DATE_OLDEST) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val doneMap by viewModel.homeworkDone.observeAsState(emptyMap())
    var hideDone by remember { mutableStateOf(context.prefs.boolean(PrefsConsts.HOMEWORK_HIDE_DONE) ?: false) }
    val onToggleHomework: (HomeworkItem, Boolean) -> Unit = { hw, done -> viewModel.setHomeworkDone(hw.id, done) }
    val onCopied: (String) -> Unit = { message ->
        coroutineScope.launch { scaffoldState.snackbarHostState.showSnackbar(message) }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onAdd = { title, subject, dueDate, dueTime -> viewModel.addTask(title, subject, dueDate, dueTime) },
            onDismiss = { showAddTaskDialog = false }
        )
    }

    val visibleItems = remember(allItems, hideDone, doneMap) {
        if (hideDone) allItems.filterNot { it.doneWith(doneMap) } else allItems
    }
    val visibleTasks = remember(personalTasks, hideDone) {
        if (hideDone) personalTasks.filterNot { it.isDone } else personalTasks
    }
    val sorted = remember(visibleItems, sortOrder, doneMap) {
        val allItems = visibleItems
        when (sortOrder) {
            HwSortOrder.DATE_NEWEST -> allItems.sortedByDescending { it.date }
            HwSortOrder.DATE_OLDEST -> allItems.sortedBy { it.date }
            HwSortOrder.SUBJECT -> allItems.sortedWith(compareBy({ it.subjectName }, { it.date }))
            // most overdue first - the thing that has been outstanding longest
            HwSortOrder.PAST_DUE -> allItems.filter { it.isPastDue(doneMap) }.sortedBy { it.date }
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
            scaffoldState = scaffoldState,
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(stringResource(R.string.homework_title)) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                hideDone = !hideDone
                                context.prefs.putOne(PrefsConsts.HOMEWORK_HIDE_DONE, hideDone)
                            }) {
                                Icon(
                                    if (hideDone) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(
                                        if (hideDone) R.string.homework_show_done else R.string.homework_hide_done
                                    )
                                )
                            }
                            if (selectedTab != HomeworkTab.TASKS) {
                                IconButton(onClick = { viewModel.loadHomework() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                                }
                            }
                        }
                    )
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        Tab(selected = selectedTab == HomeworkTab.BOTH, onClick = { selectedTab = HomeworkTab.BOTH }) {
                            Text(stringResource(R.string.tab_both), Modifier.padding(vertical = 12.dp))
                        }
                        Tab(selected = selectedTab == HomeworkTab.HOMEWORK, onClick = { selectedTab = HomeworkTab.HOMEWORK }) {
                            Text(stringResource(R.string.tab_homework), Modifier.padding(vertical = 12.dp))
                        }
                        Tab(selected = selectedTab == HomeworkTab.TASKS, onClick = { selectedTab = HomeworkTab.TASKS }) {
                            Text(stringResource(R.string.tab_tasks), Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab != HomeworkTab.HOMEWORK) {
                    FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                when (selectedTab) {
                    HomeworkTab.HOMEWORK -> when {
                        isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        // deliberately allItems, not the filtered list: with nothing past due the
                        // empty state would replace the chip row and there would be no way back
                        allItems.isEmpty() -> EmptyState()
                        else -> HomeworkList(sortOrder, grouped, dateFmt, timeFmt, { sortOrder = it }, doneMap, onToggleHomework, onCopied)
                    }
                    HomeworkTab.TASKS -> TasksTab(
                        tasks = visibleTasks,
                        onToggle = { viewModel.toggleTaskDone(it) },
                        onDelete = { viewModel.deleteTask(it.id) }
                    )
                    HomeworkTab.BOTH -> when {
                        isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        allItems.isEmpty() && personalTasks.isEmpty() -> EmptyState()
                        else -> HomeworkAndTasksList(
                            sortOrder = sortOrder,
                            items = sorted,
                            dateFmt = dateFmt,
                            timeFmt = timeFmt,
                            tasks = visibleTasks,
                            onSortChange = { sortOrder = it },
                            onToggleTask = { viewModel.toggleTaskDone(it) },
                            onDeleteTask = { viewModel.deleteTask(it.id) },
                            doneMap = doneMap,
                            onToggleHomework = onToggleHomework,
                            onCopied = onCopied
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeworkList(
    sortOrder: HwSortOrder,
    grouped: List<Pair<LocalDate?, List<HomeworkItem>>>,
    dateFmt: org.joda.time.format.DateTimeFormatter,
    timeFmt: org.joda.time.format.DateTimeFormatter,
    onSortChange: (HwSortOrder) -> Unit,
    doneMap: Map<String, Boolean>,
    onToggleHomework: (HomeworkItem, Boolean) -> Unit,
    onCopied: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        homeworkContent(sortOrder, grouped, dateFmt, timeFmt, onSortChange, doneMap, onToggleHomework, onCopied)
    }
}

/** One row of the combined tab - either a real assignment or one of the user's own tasks. */
private sealed class CombinedEntry {
    abstract val date: LocalDate?
    abstract val time: LocalTime?
    abstract val subject: String
    abstract val key: String

    data class Hw(val item: HomeworkItem) : CombinedEntry() {
        override val date get() = item.date
        override val time get() = item.lessonBeginTime
        override val subject get() = item.subjectAbbrev.ifBlank { item.subjectName }
        override val key get() = "hw-${item.subjectName}-${item.date}-${item.description?.take(24)}"
    }

    data class Task(val task: PersonalTask) : CombinedEntry() {
        override val date get() = task.dueDate
        override val time get() = task.dueTime
        override val subject get() = task.subject
        override val key get() = "task-${task.id}"
    }
}

/** A local tick overrides whatever Bakaláři reported. */
private fun HomeworkItem.doneWith(doneMap: Map<String, Boolean>): Boolean = doneMap[id] ?: isDone

private sealed class GroupKey {
    data class ByDate(val date: LocalDate?) : GroupKey()
    data class BySubject(val subject: String) : GroupKey()
}

/**
 * Groups assignments and personal tasks together, so a date that has both shows both under the one
 * heading rather than splitting the tab into a homework half and a tasks half.
 */
private fun groupCombined(
    items: List<HomeworkItem>,
    tasks: List<PersonalTask>,
    sortOrder: HwSortOrder,
    doneMap: Map<String, Boolean>
): List<Pair<GroupKey, List<CombinedEntry>>> {
    val all: List<CombinedEntry> = if (sortOrder == HwSortOrder.PAST_DUE) {
        items.filter { it.isPastDue(doneMap) }.map { CombinedEntry.Hw(it) } +
            tasks.filter { it.isPastDue() }.map { CombinedEntry.Task(it) }
    } else {
        items.map { CombinedEntry.Hw(it) } + tasks.map { CombinedEntry.Task(it) }
    }
    if (all.isEmpty()) return emptyList()

    // within a group, order by time of day; entries without a time sink to the bottom
    val byTime = compareBy<CombinedEntry>({ it.time == null }, { it.time?.millisOfDay ?: 0 })

    return if (sortOrder == HwSortOrder.SUBJECT) {
        all.groupBy { it.subject }
            .toList()
            .sortedBy { it.first.lowercase() }
            .map { (subject, entries) -> GroupKey.BySubject(subject) to entries.sortedWith(byTime) }
    } else {
        val newestFirst = sortOrder == HwSortOrder.DATE_NEWEST
        all.groupBy { it.date }
            .toList()
            // undated entries always last, whichever direction the dates run
            .sortedWith(compareBy<Pair<LocalDate?, List<CombinedEntry>>> { it.first == null }
                .thenComparator { a, b ->
                    val x = a.first
                    val y = b.first
                    when {
                        x == null || y == null -> 0
                        newestFirst -> y.compareTo(x)
                        else -> x.compareTo(y)
                    }
                })
            .map { (date, entries) -> GroupKey.ByDate(date) to entries.sortedWith(byTime) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeworkAndTasksList(
    sortOrder: HwSortOrder,
    items: List<HomeworkItem>,
    dateFmt: org.joda.time.format.DateTimeFormatter,
    timeFmt: org.joda.time.format.DateTimeFormatter,
    tasks: List<PersonalTask>,
    onSortChange: (HwSortOrder) -> Unit,
    onToggleTask: (PersonalTask) -> Unit,
    onDeleteTask: (PersonalTask) -> Unit,
    doneMap: Map<String, Boolean>,
    onToggleHomework: (HomeworkItem, Boolean) -> Unit,
    onCopied: (String) -> Unit
) {
    val groups = remember(items, tasks, sortOrder, doneMap) { groupCombined(items, tasks, sortOrder, doneMap) }
    val noDateLabel = stringResource(R.string.homework_no_date)
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        item { SortRow(sortOrder) { onSortChange(it) } }
        groups.forEach { (groupKey, entries) ->
            stickyHeader {
                when (groupKey) {
                    is GroupKey.ByDate -> DateHeader(groupKey.date?.toString(dateFmt) ?: noDateLabel, groupKey.date)
                    is GroupKey.BySubject -> SubjectHeader(groupKey.subject, groupKey.subject)
                }
            }
            items(entries, key = { it.key }) { entry ->
                when (entry) {
                    is CombinedEntry.Hw -> HomeworkCard(
                        entry.item,
                        timeFmt = timeFmt,
                        showSubjectChip = groupKey is GroupKey.ByDate,
                        isDone = entry.item.doneWith(doneMap),
                        onToggleDone = { onToggleHomework(entry.item, it) },
                        onCopied = onCopied
                    )
                    is CombinedEntry.Task -> TaskCard(
                        task = entry.task,
                        onToggle = onToggleTask,
                        onDelete = onDeleteTask
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.homeworkContent(
    sortOrder: HwSortOrder,
    grouped: List<Pair<LocalDate?, List<HomeworkItem>>>,
    dateFmt: org.joda.time.format.DateTimeFormatter,
    timeFmt: org.joda.time.format.DateTimeFormatter,
    onSortChange: (HwSortOrder) -> Unit,
    doneMap: Map<String, Boolean>,
    onToggleHomework: (HomeworkItem, Boolean) -> Unit,
    onCopied: (String) -> Unit
) {
    item {
        SortRow(sortOrder) { onSortChange(it) }
    }
    if (sortOrder == HwSortOrder.SUBJECT) {
        grouped.forEach { (_, subjectItems) ->
            val subject = subjectItems.first()
            stickyHeader {
                SubjectHeader(subject.subjectName, subject.subjectAbbrev)
            }
            items(subjectItems) { hw ->
                HomeworkCard(hw, timeFmt = timeFmt, showSubjectChip = false, isDone = hw.doneWith(doneMap), onToggleDone = { onToggleHomework(hw, it) }, onCopied = onCopied)
            }
        }
    } else {
        grouped.forEach { (date, dateItems) ->
            stickyHeader {
                DateHeader(date?.toString(dateFmt) ?: stringResource(R.string.homework_no_date), date)
            }
            items(dateItems) { hw ->
                HomeworkCard(hw, timeFmt = timeFmt, showSubjectChip = true, isDone = hw.doneWith(doneMap), onToggleDone = { onToggleHomework(hw, it) }, onCopied = onCopied)
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

/**
 * How far a date is from today, and how loudly to say it. Colour carries the urgency: overdue reads
 * as an error, today as the accent, the next couple of days as a warning, anything further out as
 * ordinary muted text.
 */
private data class DayOffset(val label: String, val color: Color, val emphasised: Boolean)

@Composable
private fun rememberDayOffset(date: LocalDate?): DayOffset? {
    if (date == null) return null
    val days = Days.daysBetween(LocalDate.now(), date).days
    val label = if (days == 0) stringResource(R.string.date_offset_today) else "%+d d".format(days)
    return when {
        days < 0 -> DayOffset(label, MaterialTheme.colors.error, false)
        days == 0 -> DayOffset(label, MaterialTheme.colors.primary, true)
        days <= 2 -> DayOffset(label, DueSoonColor, true)
        days <= 7 -> DayOffset(label, MaterialTheme.colors.secondary, false)
        else -> DayOffset(label, MaterialTheme.colors.onSurface.copy(alpha = 0.55f), false)
    }
}

/** Amber for "due in a day or two" - neither an error nor the calm end of the scale. */
private val DueSoonColor = Color(0xFFF9A825)

@Composable
private fun DateHeader(label: String, date: LocalDate? = null) {
    val offset = rememberDayOffset(date)
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.primary
            )
            if (offset != null) {
                Surface(
                    color = offset.color.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        offset.label,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.caption,
                        fontWeight = if (offset.emphasised) FontWeight.Bold else FontWeight.Medium,
                        color = offset.color
                    )
                }
            }
        }
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
        HwSortOrder.DATE_OLDEST to R.string.homework_sort_oldest,
        HwSortOrder.DATE_NEWEST to R.string.homework_sort_newest,
        HwSortOrder.SUBJECT to R.string.homework_sort_subject,
        HwSortOrder.PAST_DUE to R.string.homework_sort_past_due
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
    showSubjectChip: Boolean,
    isDone: Boolean,
    onToggleDone: (Boolean) -> Unit,
    onCopied: (String) -> Unit
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
            Checkbox(
                checked = isDone,
                onCheckedChange = onToggleDone,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 4.dp),
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
            )
            Spacer(Modifier.size(6.dp))
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
                // per-card rather than around the whole list: selection across a LazyColumn breaks
                // as soon as an item scrolls out and is recycled
                SelectionContainer {
                    Column {
                        Text(
                            hw.description ?: stringResource(R.string.homework_no_description),
                            style = MaterialTheme.typography.body2,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                            color = when {
                                isDone -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                hw.description == null -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                else -> Color.Unspecified
                            }
                        )
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
            if (hw.description != null) {
                val clipboard = LocalClipboardManager.current
                val copiedMessage = stringResource(R.string.copied_to_clipboard)
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(hw.description))
                        onCopied(copiedMessage)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.homework_copy),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TasksTab(
    tasks: List<PersonalTask>,
    onToggle: (PersonalTask) -> Unit,
    onDelete: (PersonalTask) -> Unit
) {
    if (tasks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.tasks_empty),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        // the DAO orders by creation time; on screen what matters is what is due soonest
        val ordered = remember(tasks) {
            tasks.sortedWith(
                compareBy<PersonalTask> { it.dueDate == null }
                    .thenBy { it.dueDate }
                    .thenBy { it.dueTime == null }
                    .thenBy { it.dueTime }
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) {
            items(ordered, key = { it.id }) { task ->
                TaskCard(task = task, onToggle = onToggle, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun TaskCard(task: PersonalTask, onToggle: (PersonalTask) -> Unit, onDelete: (PersonalTask) -> Unit) {
    val alpha = if (task.isDone) 0.5f else 1f
    val dateFmt = remember { DateTimeFormat.mediumDate() }
    val timeFmt = remember { DateTimeFormat.shortTime() }
    val dueText = listOfNotNull(
        task.dueDate?.toString(dateFmt),
        task.dueTime?.toString(timeFmt)
    ).joinToString(" ")
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
        elevation = if (task.isDone) 0.dp else 2.dp,
        backgroundColor = if (task.isDone) MaterialTheme.colors.surface else MaterialTheme.colors.secondary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle(task) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.body2,
                    fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colors.onSurface.copy(alpha = alpha)
                )
                val metadata = listOfNotNull(
                    task.subject.takeIf { it.isNotBlank() },
                    dueText.takeIf { it.isNotBlank() },
                    stringResource(R.string.task_attached_to_lesson).takeIf { task.lessonKey != null }
                ).joinToString(" · ")
                if (metadata.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        metadata,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = alpha * 0.6f)
                    )
                }
            }
            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun AddTaskDialog(onAdd: (title: String, subject: String, dueDate: LocalDate?, dueTime: LocalTime?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("") }
    var dueTimeText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val invalidDateText = stringResource(R.string.task_due_date_invalid)
    val invalidTimeText = stringResource(R.string.task_due_time_invalid)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.task_subject_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it },
                    label = { Text(stringResource(R.string.task_due_date_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dueTimeText,
                    onValueChange = { dueTimeText = it },
                    label = { Text(stringResource(R.string.task_due_time_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                errorText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val dueDate = if (dueDateText.isBlank()) null else try {
                            LocalDate.parse(dueDateText.trim())
                        } catch (_: IllegalArgumentException) {
                            errorText = invalidDateText
                            return@TextButton
                        }
                        val dueTime = if (dueTimeText.isBlank()) null else try {
                            LocalTime.parse(dueTimeText.trim())
                        } catch (_: IllegalArgumentException) {
                            errorText = invalidTimeText
                            return@TextButton
                        }
                        onAdd(title.trim(), subject.trim(), dueDate, dueTime)
                        onDismiss()
                    }
                }
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
