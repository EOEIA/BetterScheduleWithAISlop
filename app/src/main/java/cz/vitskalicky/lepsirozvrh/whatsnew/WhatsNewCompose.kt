package cz.vitskalicky.lepsirozvrh.whatsnew

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
import cz.vitskalicky.lepsirozvrh.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Composable
fun WhatsNewDialog(onDismissed: () -> Unit){
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
                    Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.BottomStart){
                        Text(stringResource(R.string.whats_new), style = MaterialTheme.typography.h5)
                    }
                    Spacer(Modifier.size(8.dp))
                    AndroidView(
                        modifier = Modifier.verticalScroll(scrollState).weight(1f).fillMaxWidth(),
                        factory = { context ->
                            val br = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.changelog)))
                            val sb = StringBuilder()
                            try {
                                var line = br.readLine()
                                while (line != null) {
                                    sb.append(line).append('\n')
                                    line = br.readLine()
                                }
                            } catch (e: IOException) {
                                Log.e(WhatsNewFragment.TAG, "Failed to load changelog")
                                sb.append("ERROR")
                            }

                            val cs: CharSequence = HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT)

                            val tw = TextView(context)
                            tw.text = cs
                            tw
                        },
                        update = { _ ->

                        }
                    )
                    Spacer(Modifier.size(8.dp))
                    Row (horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onDismissed){
                            Text(stringResource(R.string.close))
                        }
                    }
                }

            }
        }
    )
}