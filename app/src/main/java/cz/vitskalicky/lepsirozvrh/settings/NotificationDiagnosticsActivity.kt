package cz.vitskalicky.lepsirozvrh.settings

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.notification.GradeNotification
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.prefs
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

class NotificationDiagnosticsActivity : ComponentActivity() {

    private val viewModel: NotificationDiagnosticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val accounts by viewModel.accounts.observeAsState(emptyList())
            var refreshKey by remember { mutableStateOf(0) }
            val rows = remember(refreshKey, accounts) { buildRows(accounts) }

            LepsirozvrhTheme(tintStatusBar = true, hasAppBar = true) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.notif_diagnostics_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            },
                            actions = {
                                IconButton(onClick = { refreshKey++ }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                                }
                            }
                        )
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(0.dp, 8.dp)
                    ) {
                        items(rows) { (label, value) ->
                            DiagRow(label, value)
                        }
                    }
                }
            }
        }
    }

    private fun buildRows(accounts: List<Account>): List<Pair<String, String>> {
        val ctx: Context = this
        val prefs = ctx.prefs
        val granted = getString(R.string.notif_diag_granted)
        val denied = getString(R.string.notif_diag_denied)
        val enabled = getString(R.string.notif_diag_enabled)
        val disabled = getString(R.string.notif_diag_disabled_value)
        val notTracked = getString(R.string.notif_diag_not_tracked)

        val permOk = PermanentNotification.areNotificationEnabled(ctx)

        val notifAccountId = prefs.long(PrefsConsts.NOTIFICATION_ACCOUNT)
        val notifAccountText = when (notifAccountId) {
            null -> getString(R.string.notif_diag_not_set)
            PermanentNotification.ACCOUNT_NOTIFICATION_DISABLED -> getString(R.string.notif_diag_user_disabled)
            PermanentNotification.ACCOUNT_NOTIFICATION_LOGGED_OUT -> getString(R.string.notif_diag_logged_out)
            else -> accounts.firstOrNull { it.id == notifAccountId }?.fullName ?: "ID: $notifAccountId"
        }

        val gradeAlerts = prefs.boolean(PrefsConsts.GRADE_ALERTS_ENABLED) ?: false
        val homeworkAlerts = prefs.boolean(PrefsConsts.HOMEWORK_ALERTS_ENABLED) ?: false
        val changeAlerts = prefs.boolean(PrefsConsts.CHANGE_ALERTS_ENABLED) ?: false

        val diagInfo = GradeNotification.getDiagnosticInfo(ctx)
        val seenGradeText = buildString {
            append(getString(R.string.notif_diag_seen_count, diagInfo.gradeIdCount))
            if (diagInfo.gradeIdPreview.isNotEmpty()) {
                append("\n")
                append(diagInfo.gradeIdPreview.joinToString("\n") { "  $it" })
            }
        }

        return listOf(
            getString(R.string.notif_diag_permission) to if (permOk) granted else denied,
            getString(R.string.notif_diag_notif_account) to notifAccountText,
            getString(R.string.notif_diag_grade_alerts) to if (gradeAlerts) enabled else disabled,
            getString(R.string.notif_diag_homework_alerts) to if (homeworkAlerts) enabled else disabled,
            getString(R.string.notif_diag_change_alerts) to if (changeAlerts) enabled else disabled,
            getString(R.string.notif_diag_last_refresh) to notTracked,
            getString(R.string.notif_diag_last_grade_notif) to notTracked,
            getString(R.string.notif_diag_last_homework_notif) to notTracked,
            getString(R.string.notif_diag_seen_grade_ids) to seenGradeText,
            getString(R.string.notif_diag_seen_homework_keys) to getString(R.string.notif_diag_seen_count, diagInfo.homeworkKeyCount),
        )
    }
}

class NotificationDiagnosticsViewModel(app: Application) : AndroidViewModel(app) {
    val accounts: LiveData<List<Account>> =
        (app as MainApplication).accountRepository.getAccountsLD()
}

@Composable
private fun DiagRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
    }
    Divider(modifier = Modifier.padding(horizontal = 16.dp))
}
