package cz.vitskalicky.lepsirozvrh.mainActivity

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.KotlinUtils.quantityStringResource
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.database.LessonNote
import cz.vitskalicky.lepsirozvrh.database.PersonalTask
import cz.vitskalicky.lepsirozvrh.database.lessonNoteKey
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCycle
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhGroup
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption
import cz.vitskalicky.lepsirozvrh.grades.GradesActivity
import cz.vitskalicky.lepsirozvrh.grades.homework.HomeworkActivity
import cz.vitskalicky.lepsirozvrh.model.rozvrh.LessonChangeType
import cz.vitskalicky.lepsirozvrh.model.rozvrh.labelRes
import androidx.compose.foundation.shape.RoundedCornerShape
import cz.vitskalicky.lepsirozvrh.settings.SettingsActivity
import cz.vitskalicky.lepsirozvrh.theme.compact
import cz.vitskalicky.lepsirozvrh.ui.theme.LocalRozvrhTheme
import cz.vitskalicky.lepsirozvrh.view.rozvrhtable.RozvrhScrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.Seconds
import org.joda.time.format.DateTimeFormat
import kotlin.math.max

private data class LessonDialogInfo(
    val lesson: RozvrhLesson,
    val caption: RozvrhCaption?,
    val lessonDate: LocalDate? = null,
    val lessonKey: String? = null,
    val noteText: String = ""
)

/** UI for schedule table with buttons for changing weeks etc.*/
@Composable
fun RozvrhWithControls(viewModel: RozvrhViewModel){
    val rozvrh by viewModel.getDisplayLD().observeAsState()
    val status by viewModel.getStatusLD().observeAsState()
    val account by viewModel.getAccountLD().observeAsState()
    val showSettingsBadge by viewModel.getShowSettingsBadgeLD().observeAsState()

    val context = LocalContext.current;
    val infolineLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.SHOW_INFO_LINE, true)
    }
    val showInfoline by infolineLD.observeAsState()
    val stickyDayColumnLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.STICKY_DAY_COLUMN, true)
    }
    val stickyDayColumn by stickyDayColumnLD.observeAsState(true)
    val highlightCurrentDayLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.HIGHLIGHT_CURRENT_DAY, false)
    }
    val highlightCurrentDay by highlightCurrentDayLD.observeAsState(false)
    val colorChangedLessonsLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.CHANGED_LESSON_VISUALS, true)
    }
    val colorChangedLessons by colorChangedLessonsLD.observeAsState(true)
    val compactTimetableLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.COMPACT_TIMETABLE, false)
    }
    val compactTimetable by compactTimetableLD.observeAsState(false)
    val transposedTimetableLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.TIMETABLE_TRANSPOSED, false)
    }
    val transposedTimetable by transposedTimetableLD.observeAsState(false)
    val alternatingRowsLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.ALTERNATING_ROWS, false)
    }
    val alternatingRows by alternatingRowsLD.observeAsState(false)
    val alternatingColsLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.ALTERNATING_COLS, false)
    }
    val alternatingCols by alternatingColsLD.observeAsState(false)
    val showNextLessonCardLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.SHOW_NEXT_LESSON_CARD, true)
    }
    val showNextLessonCard by showNextLessonCardLD.observeAsState(true)
    val showNextLessonCountdownLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.SHOW_NEXT_LESSON_COUNTDOWN, true)
    }
    val showNextLessonCountdown by showNextLessonCountdownLD.observeAsState(true)
    val hideEmptyHoursLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.HIDE_EMPTY_HOURS, false)
    }
    val hideEmptyHours by hideEmptyHoursLD.observeAsState(false)

    var infotext: String? = viewModel.weekPosition.let {
        when{
            it == RozvrhViewModel.PERM -> stringResource(R.string.info_permanent)
            it == 0 -> stringResource(R.string.info_this_week)
            it == 1 -> stringResource(R.string.info_next_week)
            it == -1 -> stringResource(R.string.info_last_week)
            it < -1 -> quantityStringResource(R.plurals.info_weeks_back, -it, -it)
            it > 1 -> quantityStringResource(R.plurals.info_weeks_forward, it, it)
            else -> ""
        }
    }
    if (viewModel.getIsOfflineLD().value != false){
        if ((viewModel.showError || viewModel.getDisplayLD().value == null) && status?.errMessage != null){
            infotext = status?.errMessage?.let{ stringResource(it) } ?: ""
        }else{
            infotext = stringResource(R.string.info_offline, infotext?:"")
        }
    }
    if (showInfoline == false) infotext = null

    val app = context.applicationContext as MainApplication
    val accountId by viewModel.getAccountIdLD().observeAsState()
    val coroutineScope = rememberCoroutineScope()
    val noteList by remember(accountId) {
        accountId?.let { app.rozvrhDb.lessonNoteDao().getAllForAccount(it) }
            ?: androidx.lifecycle.MutableLiveData(emptyList())
    }.observeAsState(emptyList())
    val noteMap = remember(noteList) { noteList.associate { it.lessonKey to it.text } }
    val taskList by remember(accountId) {
        accountId?.let { app.rozvrhDb.personalTaskDao().getAllForAccount(it) }
            ?: androidx.lifecycle.MutableLiveData(emptyList())
    }.observeAsState(emptyList())
    val lessonTaskMap = remember(taskList) {
        taskList.filter { it.lessonKey != null }.groupBy { it.lessonKey!! }
    }

    val isCenterToCurrentLessonEnabled: () -> Boolean = {SharedPrefsKt(context).boolean(PrefsConsts.CENTER_TO_CURRENT_LESSON)?:true}
    val centerToCurrentLesson by viewModel.centerToCurrentLessonLD.observeAsState()
    RozvrhWithControlsStateless(
        rozvrh = rozvrh?.data,
        isTeacher = account?.isTeacher() ?: false,
        weekPosition = viewModel.weekPosition,
        status = status?.status ?: StatusInfo.Status.UNKNOWN,
        statusLineText = infotext,
        onNextPress = {viewModel.weekPosition++},
        onPrevPress = {viewModel.weekPosition--},
        onCurrentPress = {viewModel.weekPosition = 0; viewModel.centerToCurrentLessonLD.value = isCenterToCurrentLessonEnabled()},
        onPermPress = {viewModel.weekPosition = RozvrhViewModel.PERM},
        onSettingsPress = {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        },
        onGradesPress = {
            val intent = Intent(context, GradesActivity::class.java)
            context.startActivity(intent)
        },
        onHomeworkPress = {
            val intent = Intent(context, HomeworkActivity::class.java)
            context.startActivity(intent)
        },
        onRefreshPress = {viewModel.forceRefresh()},
        centerToCurrentLesson = centerToCurrentLesson ?: isCenterToCurrentLessonEnabled(),
        onCenterCompleted = {viewModel.centerToCurrentLessonLD.value = false},
        showSettingsBadge = showSettingsBadge ?: false,
        stickyDayColumn = stickyDayColumn,
        highlightCurrentDay = highlightCurrentDay,
        colorChangedLessons = colorChangedLessons,
        compactTimetable = compactTimetable,
        transposedTimetable = transposedTimetable,
        onTransposeToggle = {
            SharedPrefsKt(context).edit { putBoolean(PrefsConsts.TIMETABLE_TRANSPOSED, !transposedTimetable) }
        },
        alternatingRows = alternatingRows,
        alternatingCols = alternatingCols,
        hideEmptyHours = hideEmptyHours,
        showNextLessonCard = showNextLessonCard,
        showNextLessonCountdown = showNextLessonCountdown,
        noteMap = noteMap,
        lessonTaskMap = lessonTaskMap,
        onNoteSave = { key, text ->
            val aid = accountId ?: return@RozvrhWithControlsStateless
            coroutineScope.launch {
                if (text.isBlank()) {
                    app.rozvrhDb.lessonNoteDao().delete(aid, key)
                } else {
                    app.rozvrhDb.lessonNoteDao().upsert(LessonNote(aid, key, text))
                }
            }
        },
        onTaskAdd = { key, title, subject, dueDate, dueTime ->
            val aid = accountId ?: return@RozvrhWithControlsStateless
            coroutineScope.launch {
                app.rozvrhDb.personalTaskDao().insert(
                    PersonalTask(
                        accountId = aid,
                        title = title,
                        subject = subject,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        lessonKey = key
                    )
                )
            }
        },
        onTaskToggle = { task ->
            coroutineScope.launch {
                app.rozvrhDb.personalTaskDao().setDone(task.id, !task.isDone)
            }
        },
        onTaskDelete = { task ->
            coroutineScope.launch {
                app.rozvrhDb.personalTaskDao().delete(task.id)
            }
        }
    )
}

/** Stateless versioin of [RozvrhWithControls]*/
@Composable
fun RozvrhWithControlsStateless(
    rozvrh: Rozvrh?,
    isTeacher: Boolean,
    weekPosition: Int,
    status: StatusInfo.Status,
    /** null to hide the statusline */
    statusLineText: String?,
    centerToCurrentLesson: Boolean = false,
    onCenterCompleted: () -> Unit = {},
    onNextPress: () -> Unit,
    onPrevPress: () -> Unit,
    onCurrentPress: () -> Unit,
    onPermPress: () -> Unit,
    onSettingsPress: () -> Unit,
    onGradesPress: () -> Unit = {},
    onHomeworkPress: () -> Unit = {},
    onRefreshPress: () -> Unit,
    showSettingsBadge: Boolean,
    stickyDayColumn: Boolean = true,
    highlightCurrentDay: Boolean = false,
    colorChangedLessons: Boolean = true,
    compactTimetable: Boolean = false,
    transposedTimetable: Boolean = false,
    onTransposeToggle: () -> Unit = {},
    alternatingRows: Boolean = false,
    alternatingCols: Boolean = false,
    hideEmptyHours: Boolean = false,
    showNextLessonCard: Boolean = true,
    showNextLessonCountdown: Boolean = true,
    noteMap: Map<String, String> = emptyMap(),
    lessonTaskMap: Map<String, List<PersonalTask>> = emptyMap(),
    onNoteSave: (lessonKey: String, text: String) -> Unit = { _, _ -> },
    onTaskAdd: (lessonKey: String, title: String, subject: String, dueDate: LocalDate?, dueTime: LocalTime?) -> Unit = { _, _, _, _, _ -> },
    onTaskToggle: (PersonalTask) -> Unit = {},
    onTaskDelete: (PersonalTask) -> Unit = {}
){
    // the lesson which is shown in dialog or null
    var dialogInfo by remember { mutableStateOf(null as LessonDialogInfo?) }
    dialogInfo?.let { info ->
        val saveCallback = info.lessonKey?.let { key -> { text: String -> onNoteSave(key, text) } }
        LessonDialog(
            lesson = info.lesson,
            isPerm = rozvrh?.permanent ?: false,
            onDismiss = { dialogInfo = null },
            caption = info.caption,
            isCurrentWeek = weekPosition == 0,
            lessonDate = info.lessonDate,
            noteText = info.lessonKey?.let { noteMap[it] } ?: info.noteText,
            onNoteSave = saveCallback,
            tasks = info.lessonKey?.let { lessonTaskMap[it] }.orEmpty(),
            onTaskAdd = info.lessonKey?.let { key ->
                { title: String ->
                    onTaskAdd(
                        key,
                        title,
                        info.lesson.subjectAbbrev.ifBlank { info.lesson.subjectName },
                        info.lessonDate,
                        info.caption?.beginTime
                    )
                }
            },
            onTaskToggle = onTaskToggle,
            onTaskDelete = onTaskDelete
        )
    }
    Surface(color = MaterialTheme.colors.surface) {
        Column(
            verticalArrangement = Arrangement.Top
        ) {
            Box(modifier = Modifier.weight(1F)) {
                val rozvrhTheme = LocalRozvrhTheme.current
                val screenWidth = LocalContext.current.resources.displayMetrics?.widthPixels ?: 0 // todo not optimal - assumes the view take up entire screen width, but proper solution is currently unnecessarily complicated
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        RozvrhScrollView(context).apply {
                            createViews()
                        }
                    },
                    update = { rozvrhScrollView ->
                        rozvrhScrollView.setOnLessonPress { dayIndex, captionIndex, _, lesson ->
                            val day = rozvrh?.days?.getOrNull(dayIndex)
                            val caption = rozvrh?.captions?.getOrNull(captionIndex)
                            val key = if (day != null && caption != null) {
                                lessonNoteKey(day.date, caption.beginTime)
                            } else null
                            dialogInfo = LessonDialogInfo(
                                lesson,
                                caption,
                                day?.date,
                                key,
                                key?.let { noteMap[it] } ?: ""
                            )
                        }
                        rozvrhScrollView.setNoteKeys(noteMap.keys + lessonTaskMap.keys)
                        rozvrhScrollView.setStickyDayColumn(stickyDayColumn)
                        rozvrhScrollView.setHighlightCurrentDay(highlightCurrentDay)
                        rozvrhScrollView.setChangeVisualMode(if (colorChangedLessons) 1 else 0)
                        rozvrhScrollView.setCompact(compactTimetable)
                        rozvrhScrollView.setTheme(if (compactTimetable) rozvrhTheme.compact() else rozvrhTheme)
                        rozvrhScrollView.setTransposed(transposedTimetable)
                        rozvrhScrollView.setAlternatingRows(alternatingRows)
                        rozvrhScrollView.setAlternatingCols(alternatingCols)
                        rozvrhScrollView.setHideEmptyHours(hideEmptyHours)
                        rozvrhScrollView.setRozvrh(rozvrh, isTeacher)
                        if (centerToCurrentLesson) {
                            rozvrhScrollView.centerToCurrentLesson(screenWidth, onCenterCompleted)
                        }
                    }
                )
            }
            if (weekPosition == 0 && showNextLessonCard) {
                NextLessonCard(
                    rozvrh = rozvrh,
                    isTeacher = isTeacher,
                    showCountdown = showNextLessonCountdown,
                    onLessonClick = { lesson, caption, date ->
                        val key = date?.let { lessonNoteKey(it, caption.beginTime) }
                        dialogInfo = LessonDialogInfo(
                            lesson,
                            caption,
                            date,
                            key,
                            key?.let { noteMap[it] } ?: ""
                        )
                    }
                )
            }
            //todo shadow

            if (statusLineText != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LocalRozvrhTheme.current.cInfolineBg,
                    contentColor = LocalRozvrhTheme.current.cInfolineText
                ) {
                    Text(statusLineText, textAlign = TextAlign.Center)
                }
            }
            Surface(
                color = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                elevation = 8.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    //todo tooltips
                    Row(Modifier.align(Alignment.CenterStart)) {
                        IconButton(onSettingsPress) {
                            BadgedBox(badge = { if(showSettingsBadge) Badge(Modifier.size(6.dp), backgroundColor = MaterialTheme.colors.error.copy(alpha = 1.0f)) { }}) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        }
                        IconButton(onGradesPress) {
                            Icon(Icons.Default.School, contentDescription = "Grades")
                        }
                        IconButton(onHomeworkPress) {
                            Icon(Icons.Default.Assignment, contentDescription = stringResource(R.string.homework_title))
                        }
                    }
                    Row(Modifier.align(Alignment.Center)) {
                        if (weekPosition != RozvrhViewModel.PERM)
                            IconButton(onPrevPress) {
                                Icon(
                                    Icons.Default.NavigateBefore,
                                    contentDescription = stringResource(R.string.prev_week)
                                )
                            }
                        if (weekPosition != 0) {
                            IconButton(onCurrentPress) {
                                Icon(Icons.Default.Home, contentDescription = stringResource(R.string.current_week))
                            }
                        } else {
                            IconButton(onPermPress) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = stringResource(R.string.permanent_schedule)
                                )
                            }
                        }
                        if (weekPosition != RozvrhViewModel.PERM)
                            IconButton(onNextPress) {
                                Icon(
                                    Icons.Default.NavigateNext,
                                    contentDescription = stringResource(R.string.next_week)
                                )
                            }

                    }

                    Row(Modifier.align(Alignment.CenterEnd)) {
                        IconButton(onTransposeToggle) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Transpose timetable",
                                tint = if (transposedTimetable) MaterialTheme.colors.primary else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                            )
                        }
                        if (status == StatusInfo.Status.LOADING) {
                            CircularProgressIndicator(color = MaterialTheme.colors.onPrimary)
                        } else {
                            IconButton(onRefreshPress) {
                                Icon(
                                    if (status == StatusInfo.Status.ERROR) Icons.Default.SyncProblem else Icons.Default.Sync,
                                    contentDescription = stringResource(R.string.prev_week)
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}


@Composable
fun LessonDialog(
    lesson: RozvrhLesson,
    isPerm: Boolean,
    onDismiss: () -> Unit,
    caption: RozvrhCaption? = null,
    isCurrentWeek: Boolean = false,
    lessonDate: LocalDate? = null,
    noteText: String = "",
    onNoteSave: ((String) -> Unit)? = null,
    tasks: List<PersonalTask> = emptyList(),
    onTaskAdd: ((String) -> Unit)? = null,
    onTaskToggle: (PersonalTask) -> Unit = {},
    onTaskDelete: (PersonalTask) -> Unit = {}
){
    var currentNote by remember(noteText) { mutableStateOf(noteText) }
    var newTaskTitle by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    if (caption != null) {
        LaunchedEffect(caption, lessonDate) {
            while (true) {
                delay(1_000)
                now = LocalDateTime.now()
            }
        }
    }
    val timeFormatter = DateTimeFormat.shortTime()
    val timeText: String? = caption?.let { cap ->
        "${cap.beginTime.toString(timeFormatter)} – ${cap.endTime.toString(timeFormatter)}"
    }
    val dialogCountdownText: String? = caption?.let { cap ->
        val date = lessonDate ?: if (isCurrentWeek) now.toLocalDate() else return@let null
        val begin = date.toLocalDateTime(cap.beginTime)
        val end = date.toLocalDateTime(cap.endTime)
        when {
            now.isBefore(begin) -> durationText(max(0, Seconds.secondsBetween(now, begin).seconds))
            now.isBefore(end) -> durationText(max(0, Seconds.secondsBetween(now, end).seconds))
            else -> null
        }
    }
    val changeLabel = lesson.changeKind
        .takeIf { it != LessonChangeType.NONE }
        ?.let { stringResource(it.labelRes()) }
    val dismiss = {
        onNoteSave?.invoke(currentNote.trim())
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = dismiss,
        buttons = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth())
            {
                if (dialogCountdownText != null) {
                    Text(
                        dialogCountdownText,
                        modifier = Modifier
                            .weight(1F)
                            .padding(start = 16.dp)
                            .align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                } else {
                    Spacer(Modifier.weight(1F))
                }
                TextButton(onClick = dismiss){ Text(stringResource(R.string.close)) }
                Spacer(Modifier.size(8.dp))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(lesson.subjectName, modifier = Modifier.weight(1f, fill = false))
                if (changeLabel != null) {
                    Spacer(Modifier.size(8.dp))
                    Surface(
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            changeLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                }
            }
        },
        text = {
            val homeworkText = lesson.homeworkDescriptions
                .ifEmpty { lesson.homeworkIds }
                .joinToString("\n")
            val data = listOf<Pair<String, String>?>(
                timeText?.let { Pair(stringResource(R.string.lesson_time), it) },
                if (homeworkText.isNotBlank()) Pair(stringResource(R.string.homework), homeworkText) else null,
                if (isPerm) Pair(stringResource(R.string.cycle), lesson.cycles.joinToString(", "){ it.abbrev.ifBlank { it.name }}) else null,
                Pair(stringResource(R.string.group), lesson.groups.joinToString(", "){ it.abbrev.ifBlank { it.name }}),
                Pair(stringResource(R.string.lesson_teacher), lesson.teacherName.ifBlank { lesson.teacherAbbrev }),
                Pair(stringResource(R.string.room), lesson.roomName.ifBlank { lesson.roomAbbrev }),
                Pair(stringResource(R.string.topic), lesson.theme),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (item in data.filterNotNull().filter { it.second.isNotBlank() }){
                    Row {
                        Text(item.first, modifier = Modifier.weight(0.4F), textAlign = TextAlign.Right, fontWeight = FontWeight.Bold )
                        Spacer(Modifier.size(4.dp))
                        Text(item.second, modifier = Modifier.weight(0.6F))
                    }
                }
                if (onNoteSave != null) {
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = currentNote,
                        onValueChange = { currentNote = it },
                        label = { Text(stringResource(R.string.lesson_note)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        singleLine = false,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            cursorColor = MaterialTheme.colors.primary
                        )
                    )
                }
                if (onTaskAdd != null || tasks.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.lesson_tasks),
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    tasks.forEach { task ->
                        val alpha = if (task.isDone) 0.5f else 1f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { onTaskToggle(task) },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                            )
                            Text(
                                task.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.body2,
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = MaterialTheme.colors.onSurface.copy(alpha = alpha)
                            )
                            IconButton(onClick = { onTaskDelete(task) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                    if (onTaskAdd != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTaskTitle,
                                onValueChange = { newTaskTitle = it },
                                label = { Text(stringResource(R.string.lesson_task)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    cursorColor = MaterialTheme.colors.primary
                                )
                            )
                            TextButton(
                                onClick = {
                                    val title = newTaskTitle.trim()
                                    if (title.isNotBlank()) {
                                        onTaskAdd(title)
                                        newTaskTitle = ""
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.add))
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
@Preview
fun LessonPreview(){
    LessonDialog(
        RozvrhLesson(
            "Matematicka a její aplikace",
            "MA",
            "Mgr. Milan Kohout",
            "Ko",
            "A105",
            "A105",
            listOf(RozvrhGroup("123","6.A","6.A")),
            listOf(RozvrhCycle("1", "Lichý", "L"), RozvrhCycle("2", "Sudý", "S")),
            emptyList(),
            "Kvadratické funkce a jejich graf",
            RozvrhLesson.NO_CHANGE,
            null
        ),false,{} )
}

@Composable
fun NextLessonCard(
    rozvrh: Rozvrh?,
    isTeacher: Boolean,
    showCountdown: Boolean = true,
    onLessonClick: ((RozvrhLesson, RozvrhCaption, LocalDate?) -> Unit)? = null
) {
    var now by remember(rozvrh) { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(rozvrh) {
        while (true) {
            delay(1_000)
            now = LocalDateTime.now()
        }
    }

    val relativeLesson = rozvrh?.getCurrentOrNextLesson(now)
    val clickableModifier = if (relativeLesson != null && onLessonClick != null) {
        Modifier.clickable { onLessonClick(relativeLesson.lesson, relativeLesson.block.caption, relativeLesson.block.day.date) }
    } else Modifier

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
            .then(clickableModifier),
        elevation = 2.dp,
        color = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        if (relativeLesson == null) {
            Text(
                text = stringResource(R.string.next_lesson_card_school_over),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.SemiBold
            )
            return@Surface
        }

        val block = relativeLesson.block
        val lesson = relativeLesson.lesson
        val timeFormatter = DateTimeFormat.shortTime()
        val time = "${block.caption.beginTime.toString(timeFormatter)} - ${block.caption.endTime.toString(timeFormatter)}"
        val durationText = durationText(max(0, Seconds.secondsBetween(now, relativeLesson.targetDateTime).seconds))
        val countdown = when (relativeLesson.state) {
            Rozvrh.RelativeLessonState.CURRENT -> stringResource(R.string.next_lesson_card_ends_in, durationText)
            Rozvrh.RelativeLessonState.NEXT -> stringResource(R.string.next_lesson_card_starts_in, durationText)
        }
        val subject = lesson.subjectName.ifBlank {
            lesson.subjectAbbrev.ifBlank { stringResource(R.string.lesson_cancelled) }
        }
        val teacher = if (isTeacher) {
            lesson.groups.joinToString(", ") { it.name.ifBlank { it.abbrev } }
        } else {
            lesson.teacherName.ifBlank { lesson.teacherAbbrev }
        }
        val room = lesson.roomName.ifBlank { lesson.roomAbbrev }
        val indicators = listOfNotNull(
            stringResource(R.string.next_lesson_card_homework_indicator).takeIf { lesson.homeworkIds.isNotEmpty() },
            stringResource(R.string.next_lesson_card_change_indicator)
                .takeIf { lesson.changeKind != LessonChangeType.NONE || lesson.changeType != RozvrhLesson.NO_CHANGE }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1F)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (relativeLesson.state == Rozvrh.RelativeLessonState.CURRENT) {
                            stringResource(R.string.next_lesson_card_current)
                        } else {
                            stringResource(R.string.next_lesson_card_next)
                        },
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    if (indicators.isNotEmpty()) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = indicators.joinToString(" • "),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
                Text(
                    text = subject,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(room, teacher).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.End
                )
                if (showCountdown) {
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun durationText(totalSeconds: Int): String {
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return stringResource(R.string.next_lesson_card_duration, days, hours, minutes, seconds)
}

@Composable
fun Rozvrhpreview(){
    RozvrhWithControlsStateless(
        rozvrh = DebugUtils.getDemoRozvrh(Utils.getCurrentMonday(), LocalContext.current),
        isTeacher = false,
        weekPosition = 0,
        status = StatusInfo.Status.SUCCESS,
        statusLineText = "Aktuální týden",
        centerToCurrentLesson = false,
        onCenterCompleted = {},
        onNextPress = {},
        onPrevPress = {},
        onCurrentPress = {},
        onPermPress = {},
        onSettingsPress = {},
        onGradesPress = {},
        onRefreshPress = {},
        showSettingsBadge = true,
        stickyDayColumn = true,
        highlightCurrentDay = false
    )
}
