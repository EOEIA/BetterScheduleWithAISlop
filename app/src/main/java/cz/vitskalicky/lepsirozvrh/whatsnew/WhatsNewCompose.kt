package cz.vitskalicky.lepsirozvrh.whatsnew

import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object WhatsNew {
    private val TAG = WhatsNew::class.simpleName!!

    /** If last version seen is older than this, notify the user that there has been an update */
    private const val INTERESTING_VERSION = 41

    /** Shows a dialog with new features in the app */
    @Composable
    fun WhatsNewDialog(onDismissed: () -> Unit) {
        val scrollState = rememberScrollState()
        Dialog(
            onDismissRequest = onDismissed,
            content = {
                Surface(
                    color = MaterialTheme.colors.surface,
                    shape = MaterialTheme.shapes.medium,
                    contentColor = contentColorFor(MaterialTheme.colors.surface),
                    elevation = 24.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
                    ) {
                        Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.BottomStart) {
                            Text(stringResource(R.string.whats_new), style = MaterialTheme.typography.h5)
                        }
                        Spacer(Modifier.size(8.dp))
                        AndroidView(
                            modifier = Modifier.verticalScroll(scrollState).weight(1f).fillMaxWidth(),
                            factory = { context ->
                                val br =
                                    BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.changelog)))
                                val sb = StringBuilder()
                                try {
                                    var line = br.readLine()
                                    while (line != null) {
                                        sb.append(line).append('\n')
                                        line = br.readLine()
                                    }
                                } catch (e: IOException) {
                                    Log.e("WhatsNewDialog", "Failed to load changelog")
                                    sb.append("ERROR")
                                }

                                val cs: CharSequence =
                                    HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT)

                                val tw = TextView(context)
                                tw.text = cs
                                tw
                            },
                            update = { _ ->

                            }
                        )
                        Spacer(Modifier.size(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onDismissed) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    }

                }
            }
        )
    }

    fun shouldNotifyAboutNewLD(context: Context): LiveData<Boolean> {
        val sp = SharedPrefsKt(context)
        val lastSeen = SharedPrefsIntLiveData(sp.sharedPreferences, PrefsConsts.LAST_VERSION_ACKNOWLEDGED, BuildConfig.VERSION_CODE)
        return lastSeen.map { it < INTERESTING_VERSION }
    }

    fun shouldNotifyAboutNew(context: Context): Boolean =
        (SharedPrefsKt(context).int(PrefsConsts.LAST_VERSION_ACKNOWLEDGED) ?: 0) < INTERESTING_VERSION

    fun userAcknowledgedNew(context: Context) {
        SharedPrefsKt(context).edit {
            putInt(PrefsConsts.LAST_VERSION_ACKNOWLEDGED, BuildConfig.VERSION_CODE)
        }
    }

}