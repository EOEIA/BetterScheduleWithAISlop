package cz.vitskalicky.lepsirozvrh.settings

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cz.vitskalicky.lepsirozvrh.KotlinUtils.str
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.SharedPrefs
import cz.vitskalicky.lepsirozvrh.donations.DonationHelper
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme
import cz.vitskalicky.lepsirozvrh.theme.SelectedTheme
import cz.vitskalicky.lepsirozvrh.theme.ThemeExchangeData
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.RuntimeException
import kotlin.coroutines.coroutineContext

//todo open links
class ImportThemeActivity : ComponentActivity(){
    val viewModel: ThemeViewModel by viewModels()
    val donHelper = DonationHelper(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        donHelper.onCreate()

        setContent {
            val scaffoldState = rememberScaffoldState()
            var showDonateDialog by rememberSaveable{mutableStateOf(donHelper.donations?.let { !it.isSponsor && it.isEnabled} ?: false)}
            var textFieldText by rememberSaveable{mutableStateOf("")}

            val paste: () -> Unit = {
                val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (clipboard.primaryClip != null && clipboard.primaryClip!!.itemCount > 0) {
                    textFieldText = clipboard.primaryClip!!.getItemAt(0).text.toString()
                }
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
                        if (showDonateDialog){
                            donHelper.donations?.ShowDialog { showDonateDialog = false }
                        }
                        val textScrollState = rememberScrollState()
                        Column(Modifier.padding(paddingValues).padding(horizontal = 16.dp, vertical = 16.dp)) {

                            // instruction text
                            //slightly hacky, but did not find a better option
                            val linkUrl = R.string.MORE_THEMES_LINK.str
                            val wholeString = getString(R.string.import_theme_detail, linkUrl)

                            val startIndex = wholeString.indexOf(linkUrl)
                            val endIndex = startIndex + linkUrl.length

                            val annotatedString = buildAnnotatedString {
                                append(wholeString)
                                addStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.onBackground,
                                    ),
                                    start = 0,
                                    end = wholeString.length
                                )
                                addStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.secondary,
                                        textDecoration = TextDecoration.Underline
                                    ), start = startIndex, end = endIndex
                                )

                                // attach a string annotation that
                                // stores a URL to the text "link"
                                addStringAnnotation(
                                    tag = "URL",
                                    annotation = linkUrl,
                                    start = startIndex,
                                    end = endIndex
                                )
                            }
                            ClickableText(
                                annotatedString,
                                onClick = {index ->
                                    if (index >= startIndex && index < endIndex){
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.MORE_THEMES_LINK)))
                                        startActivity(browserIntent)
                                    }
                                }
                            )
                            Spacer(Modifier.size(8.dp))
                            //paste button
                            Button(onClick = paste){
                                Row {
                                    Icon(Icons.Default.ContentPaste, null)
                                    Spacer(Modifier.size(4.dp))
                                    Text(R.string.paste_from_clipboard.str)
                                }
                            }
                            Spacer(Modifier.size(16.dp))
                            //text field
                            TextField(textFieldText, {textFieldText = it }, modifier = Modifier.fillMaxSize().weight(1f))
                            //buttons
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {textFieldText = ""}){
                                    Text(R.string.import_clear.str)
                                }
                                Button(onClick = {
                                    val success = import(textFieldText)
                                    if (!success){
                                        lifecycleScope.launch {
                                            scaffoldState.snackbarHostState.showSnackbar(
                                                getString(R.string.import_invalid),
                                                actionLabel = getString(R.string.ok),
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                }){
                                    Text(R.string.import_button.str)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun import(data:String):Boolean {
        var td: ThemeExchangeData? = null
        val original = data.replace("\\s".toRegex(), "") //remove all whitespaces
        var input = original
        val possibleUrlBases = resources.getStringArray(R.array.theme_url_bases);
        for (base in possibleUrlBases ){
            if (input.startsWith(base)){
                val uri = Uri.parse(input)
                input = uri.getQueryParameter("data") ?: ""
            }
        }
        if (input.startsWith("lepsi-rozvrh:motiv/")) {
            input = input.substring("lepsi-rozvrh:motiv/".length)
        }
        if (input.isBlank()) {
            input = original
        }
        try {
            td = ThemeExchangeData.parseZipped(input)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            //try to parse as json (not zipped)
            try {
                td = ThemeExchangeData.parseJson(input)
            } catch (ex: Exception) {
                if (ex is CancellationException) throw e
                //try to parse as base64, but not url-safe
                input = input.replace('+', '-').replace('/', '_')
                try {
                    td = ThemeExchangeData.parseZipped(input)
                } catch (exc: Exception) {
                    if (exc is CancellationException) throw e
                    //try fixing the data (find the magic number (+compression method - always same) of gzip: H4s)
                    try {
                        val index = input.indexOf("H4s")
                        if (index > -1) {
                            td = ThemeExchangeData.parseZipped(input.substring(index))
                        }
                    } catch (ignored: Exception) {
                        if (ignored is CancellationException) throw e
                    }
                }
            }
        }

        if (td != null) {
            viewModel.selectedTheme = SelectedTheme.CUSTOM
            viewModel.theme = td.toRozvrhTheme()
            return true
        } else {
            return false
        }
    }

    override fun onDestroy() {
        donHelper.release()
        super.onDestroy()
    }
}