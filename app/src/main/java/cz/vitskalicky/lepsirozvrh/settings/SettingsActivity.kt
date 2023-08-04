package cz.vitskalicky.lepsirozvrh.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollState:ScrollState = rememberScrollState()

        setContent {
            LepsirozvrhTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.settings)) },
                            navigationIcon = { Icon(Icons.Default.ArrowBack, stringResource(R.string.back)) }
                        )
                    },
                    content = {paddingValues: PaddingValues ->
                        Column(
                            Modifier.scrollable(scrollState, Orientation.Vertical)
                                .padding(
                                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                                )
                        ) {
                            Spacer(Modifier.size(paddingValues.calculateTopPadding()))

                            PreferenceGroupHeader(R.string.user.str)
                            Preference(TODO(), TODO(), enabled = false){}
                            Preference(R.string.switch_account.str, null, Icons.Default.SwitchAccount.icon){ TODO() }
                            Preference(R.string.logout.str, null, Icons.Default.Logout.icon){ TODO() }
                            Divider()
                            PreferenceGroupHeader(R.string.pref_category_appearance.str)
                            Preference(R.string.app_theme_screen.str, R.string.app_theme_screen_desc.str, Icons.Default.Palette.icon){ TODO() }
                            SwitchPreference(R.string.info_line.str, R.string.info_line_desc.str, TODO()){ newValue: Boolean -> TODO() }
                            RadioPreference(R.string.switch_to_next_week.str, TODO(), TODO(),TODO(), TODO()){ TODO() }
                            SwitchPreference(R.string.center_to_current_lesson.str, null, TODO(), Icons.Default.CenterFocusWeak.icon) {TODO()}
                            SwitchPreference(R.string.notification.str, R.string.notification_desc.str, TODO(), Icons.Default.Notifications.icon){TODO()}
                            Divider()
                            PreferenceGroupHeader(R.string.about.str)
                            Preference(R.string.whats_new.str, null, Icons.Default.NewReleases.icon){ TODO() }
                            Preference(R.string.website.str, R.string.website_desc.str,Icons.Default.Language.icon){TODO()}
                            Preference(R.string.feedback.str, R.string.feedback_desc.str, Icons.Default.Feedback.icon){ TODO() }
                            Preference(R.string.privacy_policy.str, null){TODO()}
                            Preference(R.string.oss_licences.str, R.string.oss_licences_desc.str){TODO()}
                            Preference(R.string.app_version.str, TODO()){ TODO("Copy") }

                            Spacer(Modifier.size(paddingValues.calculateBottomPadding()))
                        }
                    }
                )
            }
        }
    }
}

// shortcuts
private inline val Int.str: String get() = stringResource(this)
private inline val ImageVector.icon: () -> Unit get() = {Icon(this, null)}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LepsirozvrhTheme {
        Greeting("Android")
    }
}