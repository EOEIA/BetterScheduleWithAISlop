package cz.vitskalicky.lepsirozvrh.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import cz.vitskalicky.lepsirozvrh.KotlinUtils.str
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.theme.ThemeExchangeData
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import kotlinx.coroutines.launch

class ExportThemeActivity: ComponentActivity() {
    val viewModel: ThemeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val scaffoldState = rememberScaffoldState()
            val themeDataUrlPrefix = R.string.THEME_DATA_LINK_PREFIX.str
            val themeStringLD = remember{
                viewModel.customThemeLD.map {
                    ThemeExchangeData
                        .fromRozvrhTheme(it)
                        .toZippedString()
                        ?.let {
                            themeDataUrlPrefix + it
                        }
                }
            }
            val themeString by themeStringLD.observeAsState()

            fun copy(){
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(themeString, themeString)
                clipboard.setPrimaryClip(clip)
                lifecycleScope.launch{
                    scaffoldState.snackbarHostState.showSnackbar(getString(R.string.copied_to_clipboard))
                }
            }
            val shareSubject = R.string.share_subject.str
            val shareInstructions = R.string.share_instructions.str
            fun share(){
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.setType("text/plain")
                sharingIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    shareSubject
                )
                sharingIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    shareInstructions + "\n\n" + themeString
                )
                startActivity(
                    Intent.createChooser(
                        sharingIntent,
                        resources.getString(R.string.share_via)
                    )
                )
            }

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
                        val textScrollState = rememberScrollState()
                        Column(Modifier.verticalScroll(textScrollState)) {
                            Column(Modifier.padding(paddingValues).padding(horizontal = 16.dp, vertical = 16.dp)) {
                                Text(R.string.export_theme_detail.str)
                                Spacer(Modifier.size(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(onClick = { copy() }) {
                                        Row {
                                            Icon(Icons.Default.ContentCopy, null)
                                            Spacer(Modifier.size(4.dp))
                                            Text(R.string.copy_to_clipboard.str)
                                        }
                                    }
                                    Button(onClick = { share() }) {
                                        Row {
                                            Icon(Icons.Default.Share, null)
                                            Spacer(Modifier.size(4.dp))
                                            Text(R.string.share.str)
                                        }
                                    }
                                }

                                Spacer(Modifier.size(16.dp))

                                SelectionContainer(Modifier.padding(4.dp).fillMaxWidth()) {
                                    Text(
                                        themeString ?: "null" /*todo*/,
                                        style = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}