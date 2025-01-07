package cz.vitskalicky.lepsirozvrh.settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cz.vitskalicky.lepsirozvrh.KotlinUtils.str
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.theme.SelectedTheme
import cz.vitskalicky.lepsirozvrh.theme.ThemeExchangeData
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ImportThemeActivity : ComponentActivity(){
    private val themeViewModel: ThemeViewModel by viewModels()
    private val viewModel: ImportThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val scaffoldState = rememberScaffoldState()
            val textFieldText by viewModel.textFieldTextLD.observeAsState()

            val paste: () -> Unit = {
                val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (clipboard.primaryClip != null && clipboard.primaryClip!!.itemCount > 0) {
                    viewModel.textFieldText = clipboard.primaryClip!!.getItemAt(0).text.toString()
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
                            TextField(textFieldText ?: "", {viewModel.textFieldText = it }, modifier = Modifier.fillMaxSize().weight(1f))
                            //buttons
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {viewModel.textFieldText = ""}){
                                    Text(R.string.import_clear.str)
                                }
                                Button(onClick = {
                                    val success = import(textFieldText ?: "")
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
        var input = cleanInput(data)
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
            themeViewModel.selectedTheme = SelectedTheme.CUSTOM
            themeViewModel.customTheme = td.toRozvrhTheme()
            return true
        } else {
            return false
        }
    }

    /** Takes theme data encoded in various forms of URL and tries to strip the url and leave only data */
    private fun cleanInput(data: String):String{
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
        return input
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("ignore", false)) {
            return
        }
        val dataString = intent.dataString
        val input = dataString?.let { cleanInput(it) }
        if (input != null) {
            viewModel.textFieldText = input;
        }
        intent.putExtra("ignore", true)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }
}