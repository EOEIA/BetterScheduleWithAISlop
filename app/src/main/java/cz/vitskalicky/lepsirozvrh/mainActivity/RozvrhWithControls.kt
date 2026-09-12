package cz.vitskalicky.lepsirozvrh.mainActivity

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import cz.vitskalicky.lepsirozvrh.grades.homework.fetchHomeworkDescriptions
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

private data class LessonDetailRow(
    val label: String,
    val value: String,
    val copyable: Boolean = false
)

/**
 * Small icon button that puts [text] on the clipboard. Android 13+ shows its own clipboard
 * confirmation popup, so only older versions get a toast.
 */
@Composable
fun CopyTextButton(label: String, text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.copied_to_clipboard)
    IconButton(
        onClick = {
            clipboard.setText(AnnotatedString(text))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.size(28.dp)
    ) {
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = "${stringResource(R.string.copy_to_clipboard)}: $label",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

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
    val showNextLessonCardLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.SHOW_NEXT_LESSON_CARD, true)
    }
    val showNextLessonCard by showNextLessonCardLD.observeAsState(true)
    val showNextLessonCountdownLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.SHOW_NEXT_LESSON_COUNTDOWN, true)
    }
    val showNextLessonCountdown by showNextLessonCountdownLD.observeAsState(true)
    val transposedTimetableLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.TIMETABLE_TRANSPOSED, false)
    }
    val transposedTimetable by transposedTimetableLD.observeAsState(false)
    val compactNextLessonCardLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.COMPACT_NEXT_LESSON_CARD, false)
    }
    val compactNextLessonCard by compactNextLessonCardLD.observeAsState(false)
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

    // Catches the week rolling over while the app is simply left open in the foreground;
    // MainActivity.onResume() covers the case of it having been in the background.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            viewModel.refreshDisplayWeek()
        }
    }

    val isCenterToCurrentLessonEnabled: () -> Boolean = {SharedPrefsKt(context).boolean(PrefsConsts.CENTER_TO_CURRENT_LESSON)?:true}
    val centerToCurrentLesson by viewModel.centerToCurrentLessonLD.observeAsState()

    // Real homework descriptions for the lesson-detail dialog (the timetable API only gives opaque
    // IDs per lesson - same fetch the Homework screen uses to resolve them to actual text).
    var homeworkDescriptionsById by remember { mutableStateOf(emptyMap<String, String>()) }
    LaunchedEffect(accountId) {
        accountId?.let { homeworkDescriptionsById = fetchHomeworkDescriptions(app, it) }
    }

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
        onRefreshPress = {
            viewModel.forceRefresh()
            accountId?.let { id -> coroutineScope.launch { homeworkDescriptionsById = fetchHomeworkDescriptions(app, id) } }
        },
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
        hideEmptyHours = hideEmptyHours,
        showNextLessonCard = showNextLessonCard,
        showNextLessonCountdown = showNextLessonCountdown,
        compactNextLessonCard = compactNextLessonCard,
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
        },
        homeworkDescriptionsById = homeworkDescriptionsById
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
    hideEmptyHours: Boolean = false,
    showNextLessonCard: Boolean = true,
    showNextLessonCountdown: Boolean = true,
    compactNextLessonCard: Boolean = false,
    noteMap: Map<String, String> = emptyMap(),
    lessonTaskMap: Map<String, List<PersonalTask>> = emptyMap(),
    onNoteSave: (lessonKey: String, text: String) -> Unit = { _, _ -> },
    onTaskAdd: (lessonKey: String, title: String, subject: String, dueDate: LocalDate?, dueTime: LocalTime?) -> Unit = { _, _, _, _, _ -> },
    onTaskToggle: (PersonalTask) -> Unit = {},
    onTaskDelete: (PersonalTask) -> Unit = {},
    homeworkDescriptionsById: Map<String, String> = emptyMap()
){
    // the lesson which is shown in dialog or null
    var dialogInfo by remember { mutableStateOf(null as LessonDialogInfo?) }
    // The current-lesson highlight and the today's-row highlight depend on the wall clock, but the
    // table only recomputes them when the schedule data itself changes - so without this tick the
    // highlight would stay stuck on a lesson that is already over. Only the current week has a
    // highlight at all, so don't tick on any other.
    var highlightTick by remember { mutableStateOf(0) }
    LaunchedEffect(weekPosition) {
        if (weekPosition != 0) return@LaunchedEffect
        while (true) {
            delay(30_000)
            highlightTick++
        }
    }
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
            onTaskDelete = onTaskDelete,
            homeworkDescriptionsById = homeworkDescriptionsById
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
                        rozvrhScrollView.setLessonIndicatorKeys(noteMap.keys, lessonTaskMap.keys)
                        rozvrhScrollView.setStickyDayColumn(stickyDayColumn)
                        rozvrhScrollView.setHighlightCurrentDay(highlightCurrentDay)
                        rozvrhScrollView.setChangeVisualMode(if (colorChangedLessons) 1 else 0)
                        rozvrhScrollView.setCompact(compactTimetable)
                        rozvrhScrollView.setTheme(if (compactTimetable) rozvrhTheme.compact() else rozvrhTheme)
                        rozvrhScrollView.setTransposed(transposedTimetable)
                        rozvrhScrollView.setHideEmptyHours(hideEmptyHours)
                        rozvrhScrollView.setRozvrh(rozvrh, isTeacher)
                        @Suppress("UNUSED_EXPRESSION")
                        highlightTick // read here so the tick re-runs this update block
                        rozvrhScrollView.refreshTimeDependentHighlights()
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
                    compact = compactNextLessonCard,
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
                val infolineTheme = LocalRozvrhTheme.current
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = infolineTheme.cInfolineBg,
                    contentColor = infolineTheme.cInfolineText
                ) {
                    Text(
                        statusLineText,
                        // spInfolineTextSize is part of every theme and is user-customizable, but
                        // nothing read it - the band rendered at the default body1 size instead.
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        textAlign = TextAlign.Center,
                        fontSize = infolineTheme.spInfolineTextSize.sp,
                        lineHeight = (infolineTheme.spInfolineTextSize * 1.35f).sp
                    )
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

                    Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                        // separates the view/refresh actions from the week navigation next to them
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(LocalContentColor.current.copy(alpha = 0.16f))
                        )
                        IconButton(onTransposeToggle) {
                            // State is carried by the axis the arrows point along, which mirrors the
                            // table's own axis. No second accent colour in the bar: cPrimary at low
                            // alpha over the elevated surface composites to mud, and at full chroma
                            // it makes a view preference the loudest thing on the screen.
                            val axis by animateFloatAsState(
                                targetValue = if (transposedTimetable) 90f else 0f,
                                label = "transpose axis"
                            )
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.transposed_timetable),
                                modifier = Modifier.rotate(axis)
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
    onTaskDelete: (PersonalTask) -> Unit = {},
    homeworkDescriptionsById: Map<String, String> = emptyMap()
){
    var currentNote by remember(noteText) { mutableStateOf(noteText) }
    var newTaskTitle by remember { mutableStateOf("") }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showTasksEditor by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    if (caption != null) {
        // same granularity policy as the next-lesson card, so the dialog does not redraw once a
        // second to restate a figure that only changes once a minute
        val dialogTarget = lessonDate?.toLocalDateTime(caption.endTime)
        val dialogTick = countdownTickMillis(
            dialogTarget?.let { max(0, Seconds.secondsBetween(now, it).seconds) } ?: Int.MAX_VALUE
        )
        LaunchedEffect(caption, lessonDate, dialogTick) {
            while (true) {
                delay(dialogTick)
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
    if (showNoteEditor && onNoteSave != null) {
        LessonNoteEditorDialog(
            note = currentNote,
            onNoteChange = { currentNote = it },
            onDismiss = { showNoteEditor = false },
            onSave = {
                onNoteSave(currentNote.trim())
                showNoteEditor = false
            }
        )
    }
    if (showTasksEditor && (onTaskAdd != null || tasks.isNotEmpty())) {
        LessonTasksEditorDialog(
            tasks = tasks,
            newTaskTitle = newTaskTitle,
            onNewTaskTitleChange = { newTaskTitle = it },
            onTaskAdd = onTaskAdd,
            onTaskToggle = onTaskToggle,
            onTaskDelete = onTaskDelete,
            onDismiss = { showTasksEditor = false }
        )
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
                LessonActionButtons(
                    showNoteButton = onNoteSave != null,
                    showTasksButton = onTaskAdd != null || tasks.isNotEmpty(),
                    taskCount = tasks.size,
                    onNoteClick = { showNoteEditor = true },
                    onTasksClick = { showTasksEditor = true }
                )
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
            val homeworkText = lesson.homeworkIds
                .mapNotNull { homeworkDescriptionsById[it] }
                .ifEmpty { lesson.homeworkDescriptions }
                .ifEmpty { if (lesson.homeworkIds.isNotEmpty()) listOf(stringResource(R.string.homework_no_description)) else emptyList() }
                .joinToString("\n")
            // `copyable` marks the rows worth a one-tap copy button - the homework assignment is the
            // whole reason this dialog gets opened, and retyping it by hand is the thing to avoid.
            val data = listOf<LessonDetailRow?>(
                timeText?.let { LessonDetailRow(stringResource(R.string.lesson_time), it) },
                if (homeworkText.isNotBlank()) LessonDetailRow(stringResource(R.string.homework), homeworkText, copyable = true) else null,
                if (isPerm) LessonDetailRow(stringResource(R.string.cycle), lesson.cycles.joinToString(", "){ it.abbrev.ifBlank { it.name }}) else null,
                LessonDetailRow(stringResource(R.string.group), lesson.groups.joinToString(", "){ it.abbrev.ifBlank { it.name }}),
                LessonDetailRow(stringResource(R.string.lesson_teacher), lesson.teacherName.ifBlank { lesson.teacherAbbrev }),
                LessonDetailRow(stringResource(R.string.room), lesson.roomName.ifBlank { lesson.roomAbbrev }),
                LessonDetailRow(stringResource(R.string.topic), lesson.theme, copyable = true),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // lets the user drag-select any of the detail text, not just use the copy buttons
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (item in data.filterNotNull().filter { it.value.isNotBlank() }) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    item.label,
                                    modifier = Modifier.weight(0.4F),
                                    textAlign = TextAlign.Right,
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    item.value,
                                    modifier = Modifier.weight(0.6F),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface
                                )
                                if (item.copyable) {
                                    // SelectionContainer swallows clicks on its children, so the
                                    // button has to sit outside the selectable text itself.
                                    DisableSelection {
                                        CopyTextButton(item.label, item.value)
                                    }
                                }
                            }
                        }
                    }
                }
                LessonExtrasSummary(
                    note = currentNote,
                    tasks = tasks,
                    onTaskToggle = onTaskToggle
                )
            }
        }
    )
}

@Composable
private fun LessonExtrasSummary(
    note: String,
    tasks: List<PersonalTask>,
    onTaskToggle: (PersonalTask) -> Unit
) {
    val trimmedNote = note.trim()
    if (trimmedNote.isBlank() && tasks.isEmpty()) return

    Spacer(Modifier.height(8.dp))
    Divider()
    Spacer(Modifier.height(4.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (trimmedNote.isNotBlank()) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.lesson_note),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.primary
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    trimmedNote,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body2
                )
            }
        }
        tasks.forEach { task ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = { onTaskToggle(task) },
                    modifier = Modifier.size(32.dp),
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body2,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colors.onSurface.copy(alpha = if (task.isDone) 0.5f else 1f)
                )
            }
        }
    }
}

@Composable
private fun LessonNoteEditorDialog(
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lesson_note)) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.lesson_note)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                singleLine = false,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colors.primary,
                    cursorColor = MaterialTheme.colors.primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LessonTasksEditorDialog(
    tasks: List<PersonalTask>,
    newTaskTitle: String,
    onNewTaskTitleChange: (String) -> Unit,
    onTaskAdd: ((String) -> Unit)?,
    onTaskToggle: (PersonalTask) -> Unit,
    onTaskDelete: (PersonalTask) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lesson_tasks)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tasks.forEach { task ->
                    LessonTaskRow(
                        task = task,
                        onTaskToggle = onTaskToggle,
                        onTaskDelete = onTaskDelete
                    )
                }
                if (onTaskAdd != null) {
                    LessonTaskInput(
                        title = newTaskTitle,
                        onTitleChange = onNewTaskTitleChange,
                        onAdd = {
                            val title = newTaskTitle.trim()
                            if (title.isNotBlank()) {
                                onTaskAdd(title)
                                onNewTaskTitleChange("")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun LessonTaskRow(
    task: PersonalTask,
    onTaskToggle: (PersonalTask) -> Unit,
    onTaskDelete: (PersonalTask) -> Unit
) {
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

@Composable
private fun LessonTaskInput(
    title: String,
    onTitleChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.lesson_task)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                cursorColor = MaterialTheme.colors.primary
            )
        )
        TextButton(onClick = onAdd) {
            Text(stringResource(R.string.add))
        }
    }
}

@Composable
private fun LessonActionButtons(
    showNoteButton: Boolean,
    showTasksButton: Boolean,
    taskCount: Int,
    onNoteClick: () -> Unit,
    onTasksClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showNoteButton) {
            IconButton(onClick = onNoteClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.lesson_note)
                )
            }
        }
        if (showTasksButton) {
            IconButton(onClick = onTasksClick) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = if (taskCount == 0) {
                        stringResource(R.string.lesson_tasks)
                    } else {
                        "${stringResource(R.string.lesson_tasks)} ($taskCount)"
                    }
                )
            }
        }
    }
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
    /** Two-line layout with a state badge and the weekday, instead of the three-line captioned one. */
    compact: Boolean = false,
    onLessonClick: ((RozvrhLesson, RozvrhCaption, LocalDate?) -> Unit)? = null
) {
    var now by remember(rozvrh) { mutableStateOf(LocalDateTime.now()) }
    val relativeLesson = rozvrh?.getCurrentOrNextLesson(now)
    val secondsAway = relativeLesson
        ?.let { max(0, Seconds.secondsBetween(now, it.targetDateTime).seconds) }
        ?: Int.MAX_VALUE
    val tickMillis = countdownTickMillis(secondsAway)
    LaunchedEffect(rozvrh, tickMillis) {
        while (true) {
            delay(tickMillis)
            now = LocalDateTime.now()
        }
    }

    // The compact card floats, so it is rounded; the classic one kept its square edges. Both get
    // the same even inset - without top padding the card sits glued to the grid with a strip of
    // surface showing only underneath it.
    val cardShape = if (compact) RoundedCornerShape(10.dp) else RectangleShape
    val clickableModifier = if (relativeLesson != null && onLessonClick != null) {
        Modifier.clickable { onLessonClick(relativeLesson.lesson, relativeLesson.block.caption, relativeLesson.block.day.date) }
    } else Modifier

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            // clip before clickable so the ripple follows the corners
            .clip(cardShape)
            .then(clickableModifier),
        shape = cardShape,
        elevation = 2.dp,
        color = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        if (relativeLesson == null) {
            Text(
                text = stringResource(R.string.next_lesson_card_school_over),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.SemiBold
            )
            return@Surface
        }

        val block = relativeLesson.block
        val lesson = relativeLesson.lesson
        val isCurrent = relativeLesson.state == Rozvrh.RelativeLessonState.CURRENT
        val timeFormatter = DateTimeFormat.shortTime()
        val beginText = block.caption.beginTime.toString(timeFormatter)
        val endText = block.caption.endTime.toString(timeFormatter)
        val isToday = block.day.date == now.toLocalDate()
        // Same short weekday form the grid header uses (see DenView). Only the compact card shows
        // it; without it a lesson two days out reads as if it were about to start.
        val dayLabel = if (isToday) null else block.day.date.toString("E")
        val countdown = if (isCurrent) {
            stringResource(R.string.next_lesson_card_ends_in, durationText(secondsAway))
        } else {
            stringResource(R.string.next_lesson_card_starts_in, durationText(secondsAway))
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
        val details = listOf(room, teacher).filter { it.isNotBlank() }.joinToString(" \u2022 ")
        val indicators = listOfNotNull(
            stringResource(R.string.next_lesson_card_homework_indicator).takeIf { lesson.homeworkIds.isNotEmpty() },
            stringResource(R.string.next_lesson_card_change_indicator)
                .takeIf { lesson.changeKind != LessonChangeType.NONE || lesson.changeType != RozvrhLesson.NO_CHANGE }
        )

        if (compact) {
            CompactLessonRow(
                isCurrent = isCurrent,
                subject = subject,
                details = details,
                indicators = indicators,
                dayLabel = dayLabel,
                timeText = if (isToday) beginText + "\u2013" + endText else beginText,
                countdown = countdown,
                showCountdown = showCountdown
            )
        } else {
            ClassicLessonRow(
                isCurrent = isCurrent,
                subject = subject,
                details = details,
                indicators = indicators,
                timeText = "$beginText - $endText",
                countdown = countdown,
                showCountdown = showCountdown
            )
        }
    }
}

/** Two lines: the "next lesson / current lesson" caption becomes a badge, freeing a whole row. */
@Composable
private fun CompactLessonRow(
    isCurrent: Boolean,
    subject: String,
    details: String,
    indicators: List<String>,
    dayLabel: String?,
    timeText: String,
    countdown: String,
    showCountdown: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // solid while a lesson is running, tinted while one is merely coming up
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrent) MaterialTheme.colors.primary
                    else MaterialTheme.colors.primary.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = stringResource(
                    if (isCurrent) R.string.next_lesson_card_current else R.string.next_lesson_card_next
                ),
                modifier = Modifier.size(16.dp),
                tint = if (isCurrent) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1F)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1F, fill = false)
                )
                if (indicators.isNotEmpty()) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = indicators.joinToString(" "),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = details,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dayLabel != null) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(Modifier.size(4.dp))
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.End
                )
            }
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

/** The original three-line card, kept as an option while the compact one is being evaluated. */
@Composable
private fun ClassicLessonRow(
    isCurrent: Boolean,
    subject: String,
    details: String,
    indicators: List<String>,
    timeText: String,
    countdown: String,
    showCountdown: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1F)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (isCurrent) R.string.next_lesson_card_current else R.string.next_lesson_card_next
                    ),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                if (indicators.isNotEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = indicators.joinToString(" \u2022 "),
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
                text = details,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeText,
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

/**
 * Duration at a granularity that matches how far away it is. Seconds only appear in the final
 * minute, where they are the only part still changing; past a day the weekday shown beside the
 * time carries the detail, so this rounds to whole days. Pairs with [countdownTickMillis], which
 * only redraws as often as the chosen granularity actually needs.
 */
@Composable
private fun durationText(totalSeconds: Int): String = when {
    totalSeconds >= 86_400 -> {
        val days = (totalSeconds + 43_200) / 86_400
        quantityStringResource(R.plurals.duration_days, days, days)
    }
    totalSeconds >= 3_600 ->
        stringResource(R.string.duration_hm, totalSeconds / 3_600, (totalSeconds % 3_600) / 60)
    totalSeconds >= 60 -> stringResource(R.string.duration_m, totalSeconds / 60)
    else -> stringResource(R.string.duration_s, totalSeconds)
}

/**
 * How often a countdown to something [secondsAway] needs redrawing. A second only buys anything
 * while the seconds digit is on screen or a current/next flip is imminent - everything further out
 * is displayed at minute granularity or coarser, so redrawing it every second just burns battery.
 */
private fun countdownTickMillis(secondsAway: Int): Long =
    if (secondsAway <= 90) 1_000L else 60_000L

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
