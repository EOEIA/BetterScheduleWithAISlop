package cz.vitskalicky.lepsirozvrh.settings

import android.content.*
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import kotlinx.coroutines.*

// UI and logic for the "send feedback" button. Asks the user whether to include their current
// schedule, copies a diagnostic report to the clipboard and opens this fork's issue tracker so
// they can paste it in. (This fork is not maintained by the original author, so feedback must not
// go to their mailbox.)

@Composable
fun FeedbackDialog(onDismissed: () -> Unit, scaffoldState: ScaffoldState){
    val context = LocalContext.current;
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog)
        AlertDialog(
            onDismissRequest = {
                coroutineScope.launch {
                    showDialog = false
                    sendFeedback(false, scaffoldState, context)
                    onDismissed()
                }
            },
            buttons = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton({
                        coroutineScope.launch {
                            showDialog = false
                            sendFeedback(false, scaffoldState, context)
                            onDismissed()
                        }
                    }){
                        Text(stringResource(R.string.no).uppercase())
                    }
                    TextButton({
                        coroutineScope.launch {
                            showDialog = false
                            sendFeedback(true, scaffoldState, context)
                            onDismissed()
                        }
                    }){
                        Text(stringResource(R.string.yes).uppercase())
                    }
                }
            },
            title = {
                Text(stringResource(R.string.include_schedule))
            },
            text = {
                Text(stringResource(R.string.include_schedule_desc))
            }
        )
}

private suspend fun sendFeedback(includeRozvrh: Boolean, scaffoldState: ScaffoldState, context: Context){
    coroutineScope {
        var sendJob: Job? = null;
        val toastJob = launch {
            val result = scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.feedback_loading), duration = SnackbarDuration.Indefinite)
            if (result == SnackbarResult.Dismissed){
                sendJob?.cancel()
            }
        }
        sendJob = launch {
            val body = prepareBody(context, includeRozvrh)
            copyToClipboard(context, body)
            val opened = openIssueTracker(context)
            toastJob.cancel()
            if (opened) {
                scaffoldState.snackbarHostState.showSnackbar(
                    context.getString(R.string.feedback_report_copied),
                    duration = SnackbarDuration.Long)
            } else {
                val url = context.getString(R.string.ISSUES_LINK)
                val result = scaffoldState.snackbarHostState.showSnackbar(
                    context.getString(R.string.no_browser),
                    actionLabel = context.getString(R.string.copy_to_clipboard),
                    duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed){
                    copyToClipboard(context, url)
                    scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                }
            }
        }

        sendJob.join()
        toastJob.cancel()
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.feedback), text))
}

/**
 * Opens this fork's issue tracker, returns true if a browser was found, false otherwise.
 */
private fun openIssueTracker(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.ISSUES_LINK)))
    return try {
        context.startActivity(intent)
        true
    } catch (ex: ActivityNotFoundException) {
        false
    }
}

private suspend fun prepareBody(context: Context, includeRozvrh: Boolean): String {
    val mainBody:String = try {
        """

-----------------------------
${context.getString(R.string.email_message)}
 Device OS: Android
 Device OS version: ${Build.VERSION.RELEASE}
 App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}
 Commit hash: ${BuildConfig.GitHash}
 Build type: ${BuildConfig.BUILD_TYPE}
 Device Brand: ${Build.BRAND}
 Device Model: ${Build.MODEL}
 Device Manufacturer: ${Build.MANUFACTURER}
 ${SharedPrefs.getString(context, SharedPrefs.SENTRY_ID)
    .takeUnless { it.isNullOrBlank() }
    ?.let {"Sentry client id: $it" }
    ?: "Sentry client id not available"
 }
 Sentry enabled: ${context.prefs.boolean(PrefsConsts.ENABLE_SENTRY)}
 """
    }catch (e: Exception){
        if (e is CancellationException) throw e
        "App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}\n"
    }

    val rozvrhs: String = if (!includeRozvrh) "" else prepareRozvrhsBody(context)

    return mainBody + rozvrhs;
}

private suspend fun prepareRozvrhsBody(context: Context): String {
    val mainApplication = context.applicationContext as MainApplication
    val accountId = context.prefs.long(PrefsConsts.ACTIVE_ACCOUNT_ID) ?: return "Active account id is null"

    val current = mainApplication.repository.getRozvrh(RozvrhRecord.Key(accountId, Utils.getDisplayWeekMonday(context)), true)
    val currentText = MainApplication.objectMapper.writeValueAsString(current)
    val perm = mainApplication.repository.getRozvrh(RozvrhRecord.Key(accountId, Rozvrh.PERM), true)
    val permText = MainApplication.objectMapper.writeValueAsString(perm)

    return """
 Current schedule:

 $currentText

 Permanent schedule:

 $permText
    """
}
