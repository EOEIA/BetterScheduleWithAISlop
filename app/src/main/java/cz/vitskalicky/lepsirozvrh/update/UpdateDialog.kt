package cz.vitskalicky.lepsirozvrh.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.launch

private enum class Stage { OFFER, DOWNLOADING, READY, FAILED }

/**
 * Offers a newer release, downloads it and hands it to the system installer.
 *
 * The install itself is the system's package installer, not us: we can only start it, and only if
 * the user has allowed this app to install unknown apps, hence the permission detour.
 */
@Composable
fun UpdateDialog(release: GithubRelease, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf(Stage.OFFER) }
    var progress by remember { mutableStateOf(0f) }
    var apk by remember { mutableStateOf<java.io.File?>(null) }

    fun startInstall(file: java.io.File) {
        if (!UpdateChecker.canInstall(context)) {
            // send them to the system screen; they come back and press install again
            UpdateChecker.requestInstallPermission(context)
            return
        }
        UpdateChecker.install(context, file)
    }

    AlertDialog(
        onDismissRequest = { if (stage != Stage.DOWNLOADING) onDismiss() },
        title = {
            Text(
                stringResource(
                    if (stage == Stage.FAILED) R.string.update_failed else R.string.update_available,
                    release.tagName.trimStart('v')
                )
            )
        },
        text = {
            Column(Modifier.heightIn(max = 320.dp)) {
                Text(
                    stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                when (stage) {
                    Stage.DOWNLOADING -> {
                        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Text("${(progress * 100).toInt()} %", style = MaterialTheme.typography.caption)
                    }
                    Stage.FAILED -> Text(stringResource(R.string.update_failed_desc))
                    else -> {
                        if (release.body.isNotBlank()) {
                            Text(
                                release.body.trim(),
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                }
            }
        },
        buttons = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (stage == Stage.OFFER) {
                    TextButton(onClick = {
                        context.prefs.putOne(PrefsConsts.SKIPPED_UPDATE_TAG, release.tagName)
                        onDismiss()
                    }) { Text(stringResource(R.string.update_skip)) }
                }
                if (stage != Stage.DOWNLOADING) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.later)) }
                }
                when (stage) {
                    Stage.OFFER, Stage.FAILED -> TextButton(onClick = {
                        stage = Stage.DOWNLOADING
                        progress = 0f
                        scope.launch {
                            val file = UpdateChecker.download(context, release) { progress = it }
                            apk = file
                            if (file == null) {
                                stage = Stage.FAILED
                            } else {
                                stage = Stage.READY
                                startInstall(file)
                            }
                        }
                    }) {
                        Text(stringResource(if (stage == Stage.FAILED) R.string.update_retry else R.string.update_download))
                    }
                    Stage.READY -> TextButton(onClick = { apk?.let { startInstall(it) } }) {
                        Text(stringResource(R.string.update_install))
                    }
                    Stage.DOWNLOADING -> {}
                }
            }
        }
    )
}
