package cz.vitskalicky.lepsirozvrh.compose

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.KotlinUtils.quantityStringResource
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.fragment.RozvrhViewModel
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCycle
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhGroup
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import cz.vitskalicky.lepsirozvrh.settings.SettingsActivity
import cz.vitskalicky.lepsirozvrh.view.rozvrhtable.RozvrhLayout

@Composable
fun RozvrhWithControls(viewModel: RozvrhViewModel){
    val rozvrh by viewModel.getDisplayLD().observeAsState()
    val status by viewModel.getStatusLD().observeAsState()
    val account by viewModel.getAccountLD().observeAsState()

    val context = LocalContext.current;
    val infolineLD = remember {
        SharedPrefsKt(context).sharedPreferences.booleanLiveData(PrefsConsts.SHOW_INFO_LINE, true)
    }
    val showInfoline by infolineLD.observeAsState()

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

    RozvrhWithControls(
        rozvrh = rozvrh?.data,
        isTeacher = account?.isTeacher() ?: false,
        weekPosition = viewModel.weekPosition,
        status = status?.status ?: StatusInfo.Status.UNKNOWN,
        statusLineText = infotext, //todo theme hide infoline
        onNextPress = {viewModel.weekPosition++},
        onPrevPress = {viewModel.weekPosition--},
        onCurrentPress = {viewModel.weekPosition = 0},
        onPermPress = {viewModel.weekPosition = RozvrhViewModel.PERM},
        onSettingsPress = {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        },
        onRefreshPress = {viewModel.forceRefresh()}
    )
}

@Composable
fun RozvrhWithControls(
    rozvrh: Rozvrh?,
    isTeacher: Boolean,
    weekPosition: Int,
    status: StatusInfo.Status,
    /** null to hide the statusline */
    statusLineText: String?,
    onNextPress: () -> Unit,
    onPrevPress: () -> Unit,
    onCurrentPress: () -> Unit,
    onPermPress: () -> Unit,
    onSettingsPress: () -> Unit,
    onRefreshPress: () -> Unit,
){
    val scroolState = rememberScrollState()
    // the lesson which is shown in dialog or null
    var dialogLesson by remember { mutableStateOf(null as RozvrhLesson?) }
    dialogLesson?.let{
        LessonDialog(it, rozvrh?.permanent?:false, onDismiss = {dialogLesson = null})
    }
    Column(
        verticalArrangement = Arrangement.Top
    ) {

        Box(
            modifier = Modifier.horizontalScroll(scroolState).weight(1F)
        ) {
            Row{
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        RozvrhLayout(context).apply {
                            setOnLessonPress { _, _, _, lesson -> dialogLesson = lesson }
                            createViews()
                        }
                    },
                    update = { rozvrhLayout ->
                        rozvrhLayout.setRozvrh(rozvrh, isTeacher, false)
                    }
                )
            }
        }
        //todo shadow

        if (statusLineText != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xff757575),//todo theming
                //contentColor = Color(0xffffffff)
            ) {
                Text(statusLineText, textAlign = TextAlign.Center)
            }
        }
        Surface(
            color = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                //todo tooltips
                IconButton(onSettingsPress) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                }
                Row(Modifier.align(Alignment.Center)) {
                    if (weekPosition != RozvrhViewModel.PERM)
                        IconButton(onPrevPress) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = stringResource(R.string.prev_week))
                        }
                    if (weekPosition != 0){
                        IconButton(onCurrentPress) {
                            Icon(Icons.Default.Home, contentDescription = stringResource(R.string.current_week))
                        }
                    }else{
                        IconButton(onPermPress) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.permanent_schedule))
                        }
                    }
                    if (weekPosition != RozvrhViewModel.PERM)
                        IconButton(onNextPress) {
                            Icon(Icons.Default.NavigateNext, contentDescription = stringResource(R.string.next_week))
                        }
                }

                Box(Modifier.align(Alignment.CenterEnd)){
                    if(status == StatusInfo.Status.LOADING){
                        CircularProgressIndicator(color = MaterialTheme.colors.secondary) // todo change color
                    }else{
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


@Composable
fun LessonDialog(lesson: RozvrhLesson, isPerm: Boolean, onDismiss: () -> Unit){
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
            val data = listOf<Pair<String, String>?>(
                if (lesson.homeworkIds.isNotEmpty()) Pair(stringResource(R.string.homework), lesson.homeworkIds.size.toString()) else null,
                if (isPerm) Pair(stringResource(R.string.cycle), lesson.cycles.joinToString(", "){ it.abbrev.ifBlank { it.name }}) else null,
                Pair(stringResource(R.string.group), lesson.groups.joinToString(", "){ it.abbrev.ifBlank { it.name }}), //you don't see group on the simplified tile anymore, therefore it is one of the main reasons you may want to see this dialog,
                Pair(stringResource(R.string.lesson_teacher), lesson.teacherName.ifBlank { lesson.teacherAbbrev }),
                Pair(stringResource(R.string.room), lesson.roomName.ifBlank { lesson.roomAbbrev }),
                Pair(stringResource(R.string.subject_name), lesson.subjectName.ifBlank { lesson.subjectAbbrev }),
                Pair(stringResource(R.string.topic), lesson.theme),
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
fun Rozvrhpreview(){
    RozvrhWithControls(
        DebugUtils.getDemoRozvrh(Utils.getCurrentMonday(), LocalContext.current),
        false,
        0,
        StatusInfo.Status.SUCCESS,
        "Aktuální týden",
        {},
        {},
        {},
        {},
        {},
        {}
    )
}