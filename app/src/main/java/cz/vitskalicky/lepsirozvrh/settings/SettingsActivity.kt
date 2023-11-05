package cz.vitskalicky.lepsirozvrh.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.accountPicker.AccountPickerActivity
import cz.vitskalicky.lepsirozvrh.LicencesActivity
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import cz.vitskalicky.lepsirozvrh.view.preferences.Preference
import cz.vitskalicky.lepsirozvrh.view.preferences.PreferenceGroupHeader
import cz.vitskalicky.lepsirozvrh.view.preferences.RadioPreference
import cz.vitskalicky.lepsirozvrh.view.preferences.SwitchPreference
import cz.vitskalicky.lepsirozvrh.whatsnew.WhatsNewDialog
import kotlinx.coroutines.launch
import cz.vitskalicky.lepsirozvrh.KotlinUtils.str
import cz.vitskalicky.lepsirozvrh.KotlinUtils.icon
import cz.vitskalicky.lepsirozvrh.donations.DonationHelper

class SettingsActivity : ComponentActivity() {
    val viewModel: SettingsViewModel by viewModels()
    var donHelper = DonationHelper(this);
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        donHelper.onCreate()

        // handles permission for notification
        // if the user denies, reset the setting. If allows, set it to the pending account
        var pendingNotificationAccountId: Long? = null
        val showNotiPermissionDialogLD: MutableLiveData<Boolean> = MutableLiveData(false)
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    viewModel.notificationAccountId = pendingNotificationAccountId
                    pendingNotificationAccountId = null
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    viewModel.notificationAccountId = null
                    showNotiPermissionDialogLD.value = true
                }
                lifecycleScope.launch {
                    PermanentNotification.update(application as MainApplication)
                }
            }

        setContent {
            val scrollState:ScrollState = rememberScrollState()
            val scaffoldState = rememberScaffoldState()
            val coroutinScope = rememberCoroutineScope()

            var showFeedbackDialog by rememberSaveable{ mutableStateOf(false) }
            var showWhatsNewDialog by rememberSaveable{ mutableStateOf(false) }
            val showNotiPermissionDialog: Boolean by showNotiPermissionDialogLD.observeAsState(false)


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
                            /** for better readability, composable functions have 0 indent and everything else has at least 1*/

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
                            PreferenceGroupHeader(R.string.look_and_behaviour.str)
                            Preference(R.string.app_theme_screen.str, R.string.app_theme_screen_desc.str, Icons.Default.Palette.icon) {
                                val intent = Intent(this@SettingsActivity, ThemeSettingsActivity::class.java);
                                startActivity(intent)
                            }
                            SwitchPreference(R.string.info_line.str, R.string.info_line_desc.str,
                                viewModel.showInfolineLD.observeAsState().value ?: true
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
                                viewModel.centerToCurrentLessonLD.observeAsState().value ?: true,
                                Icons.Default.CenterFocusWeak.icon
                            ) {newValue ->
                                viewModel.centerToCurrentLesson = newValue
                            }

                                if (showNotiPermissionDialog){
                                    PermanentNotification.ShowNoPermissionDialog { showNotiPermissionDialogLD.value = false }
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
                                val selectedAccount = optIndex?.let { accounts[it].id }

                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    //lower api - no need for notification permission
                                    viewModel.notificationAccountId = selectedAccount
                                }else{
                                    //higher api - need to check and request permission
                                    //check for notification permission
                                    if (ActivityCompat.checkSelfPermission(this@SettingsActivity,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        //already granted - no more action needed
                                        viewModel.notificationAccountId = selectedAccount
                                    }else{
                                        //not granted - request it
                                        pendingNotificationAccountId = selectedAccount
                                        requestPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                lifecycleScope.launch {
                                    PermanentNotification.update(application as MainApplication)
                                }
                            }
                            Divider()
                            PreferenceGroupHeader(R.string.about.str)

                                if (showWhatsNewDialog) WhatsNewDialog(onDismissed = {showWhatsNewDialog = false})
                            Preference(R.string.whats_new.str, null, Icons.Default.NewReleases.icon){
                                showWhatsNewDialog = true;
                            }
                                val donationsEnabled by donHelper.donationsEnabledLD.observeAsState()
                                val isSponsor by donHelper.isSponsorLD.observeAsState()
                                if (donationsEnabled ?: false){
                                    var displayDonationDialog by rememberSaveable{mutableStateOf(false)}
                                    if (displayDonationDialog){
                                        donHelper.donations?.ShowDialog(onDismiss = {displayDonationDialog = false})
                                    }
                            Preference(
                                    title = if (isSponsor ?: false) R.string.donate_title_ok.str else R.string.donate_title.str,
                                    description = if (isSponsor ?: false) R.string.donate_text1.str else R.string.donate_text1_ok.str,
                                    icon = {Icon(Icons.Default.AttachMoney, null)},
                                ){
                                    displayDonationDialog = true;
                                }
                                }
                            Preference(R.string.website.str, R.string.website_desc.str,Icons.Default.Language.icon){
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website_link)))
                                startActivity(browserIntent)
                            }

                                if(showFeedbackDialog) FeedbackDialog(onDismissed = {showFeedbackDialog = false},scaffoldState)
                            Preference(R.string.feedback.str, R.string.feedback_desc.str, Icons.Default.Feedback.icon){ showFeedbackDialog = true }
                                if (BuildConfig.ALLOW_SENTRY) {
                            SwitchPreference(R.string.send_crash.str, R.string.send_crash_desc.str, viewModel.sendCrashReportsLD.observeAsState().value ?: false, Icons.Default.BugReport.icon) { viewModel.sendCrashReports = it }
                                }
                            Preference(R.string.privacy_policy.str, null){
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.PRIVACY_POLICY_LINK)))
                                startActivity(browserIntent)
                            }
                                if (donationsEnabled ?: false){
                            Preference(R.string.restore_purchases.str, R.string.restore_purchases_desc.str){donHelper.donations?.restorePurchases()}
                                }
                            Preference(R.string.oss_licences.str, R.string.oss_licences_desc.str){
                                val intent = Intent(this@SettingsActivity, LicencesActivity::class.java);
                                startActivity(intent)
                            }

                                val versionText = BuildConfig.FLAVOR + "-" + BuildConfig.BUILD_TYPE + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.GitHash + ")"
                            Preference(R.string.app_version.str,
                                versionText
                            ){
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
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

    override fun onResume() {
        super.onResume()
        // check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this@SettingsActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                //reset notification settings
                viewModel.notificationAccountId = null
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

    override fun onDestroy() {
        super.onDestroy()
        donHelper.release()
    }
}