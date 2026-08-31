package cz.vitskalicky.lepsirozvrh.mainActivity

import android.content.Intent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.KotlinUtils.quantityStringResource
import cz.vitskalicky.lepsirozvrh.R
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
import androidx.compose.foundation.clickable
import cz.vitskalicky.lepsirozvrh.settings.SettingsActivity
import cz.vitskalicky.lepsirozvrh.theme.compact
import cz.vitskalicky.lepsirozvrh.ui.theme.LocalRozvrhTheme
import cz.vitskalicky.lepsirozvrh.view.rozvrhtable.RozvrhScrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.LocalDateTime
import org.joda.time.Seconds
import org.joda.time.format.DateTimeFormat
import kotlin.math.max

private data class LessonDialogInfo(val lesson: RozvrhLesson, val caption: RozvrhCaption?)

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
    val showNextLessonCardLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.SHOW_NEXT_LESSON_CARD, true)
    }
    val showNextLessonCard by showNextLessonCardLD.observeAsState(true)

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
        showNextLessonCard = showNextLessonCard
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
    showNextLessonCard: Boolean = true
){
    val coroutineScope = rememberCoroutineScope()
    // the lesson which is shown in dialog or null
    var dialogInfo by remember { mutableStateOf(null as LessonDialogInfo?) }
    dialogInfo?.let {
        LessonDialog(
            lesson = it.lesson,
            isPerm = rozvrh?.permanent ?: false,
            onDismiss = { dialogInfo = null },
            caption = it.caption,
            isCurrentWeek = weekPosition == 0
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
                        rozvrhScrollView.setOnLessonPress { _, captionIndex, _, lesson ->
                            dialogInfo = LessonDialogInfo(lesson, rozvrh?.captions?.getOrNull(captionIndex))
                        }
                        rozvrhScrollView.setStickyDayColumn(stickyDayColumn)
                        rozvrhScrollView.setHighlightCurrentDay(highlightCurrentDay)
                        rozvrhScrollView.setChangeVisualMode(if (colorChangedLessons) 1 else 0)
                        rozvrhScrollView.setCompact(compactTimetable)
                        rozvrhScrollView.setTheme(if (compactTimetable) rozvrhTheme.compact() else rozvrhTheme)
                        rozvrhScrollView.setTransposed(transposedTimetable)
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
                    onLessonClick = { lesson, caption -> dialogInfo = LessonDialogInfo(lesson, caption) }
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

                        if(BuildConfig.DEBUG){
                            var expanded by remember { mutableStateOf(false) }
                            val accountRepository = (LocalContext.current.applicationContext as MainApplication).accountRepository;

                            IconButton({expanded = true}){
                                Icon(Icons.Default.DeveloperMode, contentDescription = "Developer tools")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    content = { Text("Expire access tokens") },
                                    onClick = {
                                        coroutineScope.launch {
                                            accountRepository.debugExpireAccessTokens()
                                        }
                                    }
                                )
    //                            DropdownMenuItem(
    //                                text = { Text("Option 2") },
    //                                onClick = { /* Do something... */ }
    //                            )
                            }
                        }
                    }

                    Box(Modifier.align(Alignment.CenterEnd)) {
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
fun LessonDialog(lesson: RozvrhLesson, isPerm: Boolean, onDismiss: () -> Unit, caption: RozvrhCaption? = null, isCurrentWeek: Boolean = false){
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    if (caption != null && isCurrentWeek) {
        LaunchedEffect(caption) {
            while (true) {
                delay(1_000)
                now = LocalDateTime.now()
            }
        }
    }
    val timeFormatter = DateTimeFormat.shortTime()
    val timeText: String? = caption?.let { cap ->
        val base = "${cap.beginTime.toString(timeFormatter)} – ${cap.endTime.toString(timeFormatter)}"
        if (!isCurrentWeek) return@let base
        val nowTime = now.toLocalTime()
        val suffix = when {
            nowTime.isBefore(cap.beginTime) -> {
                val secs = max(0, Seconds.secondsBetween(now, now.toLocalDate().toLocalDateTime(cap.beginTime)).seconds)
                " (${stringResource(R.string.next_lesson_card_starts_in, durationText(secs))})"
            }
            nowTime.isBefore(cap.endTime) -> {
                val secs = max(0, Seconds.secondsBetween(now, now.toLocalDate().toLocalDateTime(cap.endTime)).seconds)
                " (${stringResource(R.string.next_lesson_card_ends_in, durationText(secs))})"
            }
            else -> ""
        }
        "$base$suffix"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        buttons = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth())
            {
                TextButton(onClick = onDismiss){ Text(stringResource(R.string.close)) }
                Spacer(Modifier.size(8.dp))
            }
        },
        title = {
            Text(lesson.subjectName)
        },
        text = {
            val homeworkText = lesson.homeworkDescriptions
                .ifEmpty { lesson.homeworkIds }
                .joinToString("\n")
            val data = listOf<Pair<String, String>?>(
                timeText?.let { Pair(stringResource(R.string.lesson_time), it) },
                if (homeworkText.isNotBlank()) Pair(stringResource(R.string.homework), homeworkText) else null,
                if (isPerm) Pair(stringResource(R.string.cycle), lesson.cycles.joinToString(", "){ it.abbrev.ifBlank { it.name }}) else null,
                Pair(stringResource(R.string.group), lesson.groups.joinToString(", "){ it.abbrev.ifBlank { it.name }}), //you don't see group on the simplified tile anymore, therefore it is one of the main reasons you may want to see this dialog,
                Pair(stringResource(R.string.lesson_teacher), lesson.teacherName.ifBlank { lesson.teacherAbbrev }),
                Pair(stringResource(R.string.room), lesson.roomName.ifBlank { lesson.roomAbbrev }),
                Pair(stringResource(R.string.subject_name), lesson.subjectName.ifBlank { lesson.subjectAbbrev }),
                Pair(stringResource(R.string.topic), lesson.theme),
                lesson.changeKind.takeIf { it != LessonChangeType.NONE }?.let {
                    Pair(stringResource(R.string.change_kind), stringResource(it.labelRes()))
                },
                lesson.changeDescription?.let { Pair(stringResource(R.string.change), it)},
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (item in data.filterNotNull().filter { it.second.isNotBlank() }){
                    Row {
                        Text(item.first, modifier = Modifier.weight(0.4F), textAlign = TextAlign.Right, fontWeight = FontWeight.Bold )
                        Spacer(Modifier.size(4.dp))
                        Text(item.second, modifier = Modifier.weight(0.6F))
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
fun NextLessonCard(rozvrh: Rozvrh?, isTeacher: Boolean, onLessonClick: ((RozvrhLesson, RozvrhCaption) -> Unit)? = null) {
    var now by remember(rozvrh) { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(rozvrh) {
        while (true) {
            delay(1_000)
            now = LocalDateTime.now()
        }
    }

    val relativeLesson = rozvrh?.getCurrentOrNextLesson(now)
    val clickableModifier = if (relativeLesson != null && onLessonClick != null) {
        Modifier.clickable { onLessonClick(relativeLesson.lesson, relativeLesson.block.caption) }
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
