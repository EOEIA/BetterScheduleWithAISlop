package cz.vitskalicky.lepsirozvrh.settings

import android.content.*
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

// UI and logic for "send feedback" button. Asks the user whether to include their current schedule in the feedback and then send an email.

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
            val successful = sendEmail(body, context)
            if (!successful) {
                toastJob.cancel()
                val result = scaffoldState.snackbarHostState.showSnackbar(
                    context.getString(R.string.no_email_client),
                    actionLabel = context.getString(R.string.copy_address),
                    duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed){
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val address = context.getString(R.string.CONTACT_MAIL)
                    val clip = ClipData.newPlainText(address, address)
                    clipboard.setPrimaryClip(clip)
                    val result2 = scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                }
            }
        }

        sendJob.join()
        toastJob.cancel()
    }
}

/**
 * tries to send an email, returns true if successful, false if no suitable email app found
 */
private suspend fun sendEmail(body: String, context: Context): Boolean{
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "message/rfc822"
    val address = context.getString(R.string.CONTACT_MAIL)
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
    intent.putExtra(Intent.EXTRA_SUBJECT, "")
    intent.putExtra(Intent.EXTRA_TEXT, body)
    return try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)))
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

//    var body: String? = null
//    try {
//        body =
//        body =
//        val finBody: String = body
//        if (includeRozvrh) {
//            if (accountId == null){
//
//            }
//            val current = mainApplication.repository.getRozvrh(Utils.getDisplayWeekMonday(context), true)
//            val currentText = MainApplication.objectMapper.writeValueAsString(current)
//            val perm = mainApplication.repository.getRozvrh(Rozvrh.PERM, true)
//            val permText = MainApplication.objectMapper.writeValueAsString(perm)
//            withContext(Dispatchers.Main){
//                var newBody = finBody
//                newBody += "\nCurrent schedule:\n\n$currentText\n"
//                newBody += "\nPermanent schedule:\n\n$permText\n"
//                val intent = Intent(Intent.ACTION_SEND)
//                intent.type = "message/rfc822"
//                val address = context.getString(R.string.CONTACT_MAIL)
//                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
//                intent.putExtra(Intent.EXTRA_SUBJECT, "")
//                intent.putExtra(Intent.EXTRA_TEXT, newBody)
//                try {
//                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)))
//                } catch (ex: ActivityNotFoundException) {
//                    val snackbar = com.google.android.material.snackbar.Snackbar.make(view, context.getText(R.string.no_email_client), com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
//                    snackbar.setAction(R.string.copy_address) { v ->
//                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                        val clip = ClipData.newPlainText(address, address)
//                        clipboard.setPrimaryClip(clip)
//                        com.google.android.material.snackbar.Snackbar.make(view, R.string.copied_to_clipboard, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
//                    }
//                    snackbar.show()
//                }
//            }
//        } else {
//            val intent = Intent(Intent.ACTION_SEND)
//            intent.type = "message/rfc822"
//            val address = context.getString(R.string.CONTACT_MAIL)
//            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
//            intent.putExtra(Intent.EXTRA_SUBJECT, "")
//            intent.putExtra(Intent.EXTRA_TEXT, body)
//            try {
//                context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)))
//            } catch (ex: ActivityNotFoundException) {
//                val snackbar = com.google.android.material.snackbar.Snackbar.make(view, context.getText(R.string.no_email_client), com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
//                snackbar.setAction(R.string.copy_address) { v ->
//                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                    val clip = ClipData.newPlainText(address, address)
//                    clipboard.setPrimaryClip(clip)
//                    com.google.android.material.snackbar.Snackbar.make(view, R.string.copied_to_clipboard, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
//                }
//                snackbar.show()
//            }
//        }
//    } catch (e: PackageManager.NameNotFoundException) {
//        Toast.makeText(context, "!", Toast.LENGTH_SHORT).show()
//    }
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