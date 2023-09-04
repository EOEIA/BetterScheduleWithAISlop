package cz.vitskalicky.lepsirozvrh.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.accountPicker.AccountPickerActivity
import cz.vitskalicky.lepsirozvrh.activity.LicencesActivity
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import cz.vitskalicky.lepsirozvrh.whatsnew.WhatsNewDialog
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    val viewModel: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val scrollState:ScrollState = rememberScrollState()
            val scaffoldState = rememberScaffoldState()
            val coroutinScope = rememberCoroutineScope()

            var showFeedbackDialog by rememberSaveable{ mutableStateOf(false) }
            var showWhatsNewDialog by rememberSaveable{ mutableStateOf(false) }

            LepsirozvrhTheme {
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.settings)) },
                            navigationIcon = {
                                IconButton({
                                    finish()
                                }) {
                                    Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                                }
                            }
                        )
                    },
                    content = {paddingValues: PaddingValues ->
                        Column(
                            Modifier.verticalScroll(scrollState)
                                .padding(
                                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                                    top = paddingValues.calculateTopPadding(),
                                    bottom = paddingValues.calculateBottomPadding()
                                )
                        ) {
                            Spacer(Modifier.size(paddingValues.calculateTopPadding()))

                            PreferenceGroupHeader(R.string.user.str)

                                val account by viewModel.accountLD.observeAsState()
                                if (viewModel.accountLD.isInitialized && account == null){
                                    intent = Intent(this@SettingsActivity, AccountPickerActivity::class.java)
                                    startActivity(intent)
                                    finishAffinity()
                                }
                            Preference(account?.fullName,account?.userTypeText){switchAccount()}
                            Preference(R.string.switch_account.str, null, Icons.Default.SwitchAccount.icon){ switchAccount() }
                            Preference(R.string.logout.str, null, Icons.Default.Logout.icon){ coroutinScope.launch { account?.let { logOut(it.id)} } }
                            Divider()
                            PreferenceGroupHeader(R.string.pref_category_appearance.str)
                            Preference(R.string.app_theme_screen.str, R.string.app_theme_screen_desc.str, Icons.Default.Palette.icon){ TODO() }
                            SwitchPreference(R.string.info_line.str, R.string.info_line_desc.str,
                                viewModel.showInfolineLD.observeAsState().value ?: false
                            ){
                                newValue: Boolean -> viewModel.showInfoline = newValue
                            }

                                val selectedIndex by viewModel.switchToNextWeekOptionIndexLD.observeAsState()
                                val entries = resources.getStringArray(R.array.switch_to_next_week_entries).toList()
                                val selectedEntry = entries[selectedIndex?:0]
                            RadioPreference(R.string.switch_to_next_week.str,
                                selectedEntry,
                                entries,selectedIndex,
                                {Text(R.string.switch_to_next_week.str)}
                            ){
                                newOptionIndex -> viewModel.switchToNextWeekOptionIndex = newOptionIndex
                            }
                            SwitchPreference(R.string.center_to_current_lesson.str, null,
                                viewModel.centerToCurrentLessonLD.observeAsState().value ?: false,
                                Icons.Default.CenterFocusWeak.icon
                            ) {newValue ->
                                viewModel.centerToCurrentLesson = newValue
                            }

                                val notificationAccount by viewModel.notificationAccountLD.observeAsState()
                                val accounts: List<Account> = (viewModel.accounts.observeAsState().value?.sortedBy { it.username + it.serverUrl } ?: emptyList())
                                val accountNames = accounts.map { it.fullName }
                                val options = mutableListOf(R.string.notification_off.str).apply { addAll(accountNames) }
                                val optionIndex = accounts.indexOfFirst { it.id == notificationAccount?.id } + 1
                                val descText = notificationAccount?.fullName?.let {
                                    stringResource(R.string.notification_status, it)
                                } ?: R.string.notification_off.str
                            RadioPreference(R.string.notification.str, descText,
                                options,
                                optionIndex,
                                { Column {
                                    Text(R.string.notification.str)
                                    Text(R.string.notification_detials.str, style = MaterialTheme.typography.caption) //todo better details and styling
                                } },
                                Icons.Default.Notifications.icon
                            ){newOptionIndex ->
                                val optIndex: Int? = (newOptionIndex -1).takeUnless { it == -1 }
                                viewModel.notificationAccountId = optIndex?.let { accounts[it].id }
                            }
                            Divider()
                            PreferenceGroupHeader(R.string.about.str)

                                if (showWhatsNewDialog) WhatsNewDialog(onDismissed = {showWhatsNewDialog = false})
                            Preference(R.string.whats_new.str, null, Icons.Default.NewReleases.icon){
                                showWhatsNewDialog = true;
                            }
                            Preference(R.string.website.str, R.string.website_desc.str,Icons.Default.Language.icon){
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website_link)))
                                startActivity(browserIntent)
                            }

                                if(showFeedbackDialog) FeedbackDialog(onDismissed = {showFeedbackDialog = false},scaffoldState)
                            Preference(R.string.feedback.str, R.string.feedback_desc.str, Icons.Default.Feedback.icon){ showFeedbackDialog = true }
                            Preference(R.string.privacy_policy.str, null){
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.PRIVACY_POLICY_LINK)))
                                startActivity(browserIntent)
                            }
                            Preference(R.string.oss_licences.str, R.string.oss_licences_desc.str){
                                val intent = Intent(this@SettingsActivity, LicencesActivity::class.java);
                                startActivity(intent)
                            }

                                val versionText = BuildConfig.FLAVOR + "-" + BuildConfig.BUILD_TYPE + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.GitHash + ")"
                            Preference(R.string.app_version.str,
                                versionText
                            ){
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(versionText, versionText)
                                clipboard.setPrimaryClip(clip)
                                lifecycleScope.launch{
                                    scaffoldState.snackbarHostState.showSnackbar(getString(R.string.copied_to_clipboard))
                                }
                            }

                            Spacer(Modifier.size(paddingValues.calculateBottomPadding()))
                        }
                    }
                )
            }
        }
    }

    private fun switchAccount(){
        intent = Intent(this, AccountPickerActivity::class.java)
        startActivity(intent)
    }

    private suspend fun logOut(accountId: Long){
        viewModel.logout(accountId);
        intent = Intent(this, AccountPickerActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}

// shortcuts
private inline val Int.str: String
    @Composable get() = stringResource(this)
private inline val ImageVector.icon: @Composable () -> Unit get() = {Icon(this, null)}