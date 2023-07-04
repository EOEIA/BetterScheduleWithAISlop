package cz.vitskalicky.lepsirozvrh.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.color.MaterialColors
import cz.vitskalicky.lepsirozvrh.DebugUtils
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import cz.vitskalicky.lepsirozvrh.view.rozvrhtable.RozvrhLayout

@Composable
fun RozvrhWithControls(
    rozvrh: Rozvrh,
    isTeacher: Boolean,
    status: StatusInfo.Status,
    /** null to hide the statusline */
    statusLineText: String?,
    middleButton: MiddleButton,
    onLessonPress: (dayIndex: Int, captionIndex: Int, lessonInBlockIndex: Int, lesson: RozvrhLesson) -> Unit,
    onNextPress: () -> Unit,
    onPrevPress: () -> Unit,
    onCurrentPress: () -> Unit,
    onPermPress: () -> Unit,
    onSettingsPress: () -> Unit,
    onRefreshPress: () -> Unit,
){
    val scroolState = rememberScrollState()
    Column(
        verticalArrangement = Arrangement.Top
    ) {

        Row(
            modifier = Modifier.horizontalScroll(scroolState).weight(1F)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    RozvrhLayout(context).apply {
                        this.createViews()
                    }
                },
                update = {rozvrhLayout ->
                    rozvrhLayout.setRozvrh(rozvrh, isTeacher, false)
                }
            )
        }
        Surface(
            elevation = 5F.dp
        ) {
            Column {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        //todo tooltips
                        IconButton(onSettingsPress) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                        Row {
                            IconButton(onPrevPress) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = stringResource(R.string.prev_week))
                            }
                            if (middleButton == MiddleButton.CURRENT_WEEK){
                                IconButton(onCurrentPress) {
                                    Icon(Icons.Default.Today, contentDescription = stringResource(R.string.current_week))
                                }
                            }else{
                                IconButton(onPermPress) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.permanent_schedule))
                                }
                            }
                            IconButton(onNextPress) {
                                Icon(Icons.Default.NavigateNext, contentDescription = stringResource(R.string.next_week))
                            }
                        }
                        if(status == StatusInfo.Status.LOADING){
                            CircularProgressIndicator()
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
}

enum class MiddleButton{
    CURRENT_WEEK,
    PERMANENT
}

@Preview
@Composable
fun Rozvrhpreview(){
    RozvrhWithControls(
        DebugUtils.getDemoRozvrh(Utils.getCurrentMonday(), LocalContext.current),
        false,
        StatusInfo.Status.SUCCESS,
        "Aktuální týden",
        MiddleButton.CURRENT_WEEK,
        {_,_,_,_ ->},
        {},
        {},
        {},
        {},
        {},
        {}
    )
}