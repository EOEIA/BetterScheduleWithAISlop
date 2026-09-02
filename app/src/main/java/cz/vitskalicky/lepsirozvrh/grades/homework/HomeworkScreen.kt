package cz.vitskalicky.lepsirozvrh.grades.homework

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.database.PersonalTask
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat

private enum class HwSortOrder { DATE_NEWEST, DATE_OLDEST, SUBJECT }
private enum class HomeworkTab { BOTH, HOMEWORK, TASKS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeworkScreen(viewModel: HomeworkViewModel, onBack: () -> Unit) {
    val allItems by viewModel.items.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val personalTasks by viewModel.personalTasks.observeAsState(emptyList())

    var selectedTab by remember { mutableStateOf(HomeworkTab.BOTH) }
    var sortOrder by remember { mutableStateOf(HwSortOrder.DATE_NEWEST) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onAdd = { title, subject, dueDate, dueTime -> viewModel.addTask(title, subject, dueDate, dueTime) },
            onDismiss = { showAddTaskDialog = false }
        )
    }

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
                Column {
                    TopAppBar(
                        title = { Text(stringResource(R.string.homework_title)) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        },
                        actions = {
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
                        allItems.isEmpty() -> EmptyState()
                        else -> HomeworkList(sortOrder, grouped, dateFmt, timeFmt) { sortOrder = it }
                    }
                    HomeworkTab.TASKS -> TasksTab(
                        tasks = personalTasks,
                        onToggle = { viewModel.toggleTaskDone(it) },
                        onDelete = { viewModel.deleteTask(it.id) }
                    )
                    HomeworkTab.BOTH -> when {
                        isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        allItems.isEmpty() && personalTasks.isEmpty() -> EmptyState()
                        else -> HomeworkAndTasksList(
                            sortOrder = sortOrder,
                            grouped = grouped,
                            dateFmt = dateFmt,
                            timeFmt = timeFmt,
                            tasks = personalTasks,
                            onSortChange = { sortOrder = it },
                            onToggleTask = { viewModel.toggleTaskDone(it) },
                            onDeleteTask = { viewModel.deleteTask(it.id) }
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
    onSortChange: (HwSortOrder) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        homeworkContent(sortOrder, grouped, dateFmt, timeFmt, onSortChange)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeworkAndTasksList(
    sortOrder: HwSortOrder,
    grouped: List<Pair<LocalDate?, List<HomeworkItem>>>,
    dateFmt: org.joda.time.format.DateTimeFormatter,
    timeFmt: org.joda.time.format.DateTimeFormatter,
    tasks: List<PersonalTask>,
    onSortChange: (HwSortOrder) -> Unit,
    onToggleTask: (PersonalTask) -> Unit,
    onDeleteTask: (PersonalTask) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        if (grouped.isNotEmpty()) {
            homeworkContent(sortOrder, grouped, dateFmt, timeFmt, onSortChange)
        }
        stickyHeader {
            DateHeader(stringResource(R.string.tab_tasks))
        }
        if (tasks.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.tasks_empty),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskCard(task = task, onToggle = onToggleTask, onDelete = onDeleteTask)
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
    onSortChange: (HwSortOrder) -> Unit
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
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) {
            items(tasks, key = { it.id }) { task ->
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
