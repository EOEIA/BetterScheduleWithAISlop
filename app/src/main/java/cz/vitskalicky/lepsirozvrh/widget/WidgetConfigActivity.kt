package cz.vitskalicky.lepsirozvrh.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.theme.Theme.Utils
import cz.vitskalicky.lepsirozvrh.view.preferences.RadioPreference
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import cz.vitskalicky.lepsirozvrh.view.preferences.ColorPreference
import cz.vitskalicky.lepsirozvrh.view.preferences.SliderPreference
import cz.vitskalicky.lepsirozvrh.view.preferences.SwitchPreference
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

abstract class WidgetConfigActivity : ComponentActivity() {
    companion object {
        /** Must match R.array.widget_style_entries*/
        private const val LIGHT = 0
        /** Must match R.array.widget_style_entries*/
        private const val DARK = 1
        /** Must match R.array.widget_style_entries*/
        private const val CUSTOM = 2
    }

    private val viewModel: WidgetConfigActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            val lightBgColor = colorResource(R.color.widgetLightBackground).toArgb()
            val darkBgColor = colorResource(R.color.widgetDarkBackground).toArgb()

            val widgetID: Int? by rememberSaveable{
                val intent = intent
                val extras = intent.extras
                val widgetID: Int? = extras?.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID)
                mutableStateOf(widgetID)
            }
            BackHandler {
                if (widgetID != null) {
                    val resultValue = Intent()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID)
                    setResult(RESULT_CANCELED, resultValue)
                }
                finish()
            }

            var widgetStyle: Int by rememberSaveable{mutableStateOf(LIGHT)}

            var bgColor: Int by rememberSaveable{mutableStateOf(lightBgColor)}
            var bgTransparency: Float by rememberSaveable{ mutableStateOf(0f) }
            var autoTextColor: Boolean by rememberSaveable{ mutableStateOf(true) }
            var textColor: Int by rememberSaveable{mutableStateOf(Utils.textColorFor(bgColor))}
            if (autoTextColor){
                val newColor = Utils.textColorFor(bgColor)
                if (newColor != textColor){
                    textColor = newColor
                }
            }

            var selectedAccount: Account? by rememberSaveable{mutableStateOf(null)}


            LepsirozvrhTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.weight(1f, fill = false).fillMaxSize(), contentAlignment = Alignment.Center){
                            WidgetView(bgColor, bgTransparency,textColor)
                        }
                        Surface(Modifier.weight(2f, fill = false).fillMaxWidth(), color = MaterialTheme.colors.surface, elevation = 8.dp ) {
                            val scrollState = rememberScrollState()
                            Column(Modifier.verticalScroll(scrollState)) {
                                Spacer(Modifier.size(16.dp))

                                val accounts: List<Account> = (viewModel.accountsLD.observeAsState().value?.sortedBy { it.username + it.serverUrl } ?: emptyList())
                                if (accounts.size == 1 && selectedAccount == null){
                                    selectedAccount = accounts[0]
                                }
                                val accountNames = accounts.map { it.fullName }
                                val optionIndex = accounts.indexOfFirst { it.id == selectedAccount?.id }.takeUnless { it == -1 }
                                val descText = selectedAccount?.fullName ?: stringResource(R.string.widget_account_none)
                                RadioPreference(stringResource(R.string.widget_account), descText,
                                    accountNames,
                                    optionIndex,
                                    { },
                                    { Icon(Icons.Default.Person, null) }
                                ){newOptionIndex ->
                                    selectedAccount = accounts[newOptionIndex]
                                }

                                    val styleOptions = stringArrayResource(R.array.widget_style_entries)
                                    val styleText = styleOptions[widgetStyle]
                                RadioPreference(
                                    title = stringResource(R.string.widget_style),
                                    description = styleText,
                                    options = styleOptions.toList(),
                                    selectedOptionIndex = widgetStyle,
                                    dialogTitle = { },
                                    icon = {Icon(Icons.Default.Palette, null) }){
                                    widgetStyle = it
                                    when (widgetStyle){
                                        LIGHT -> {
                                            bgColor = lightBgColor
                                            textColor = Utils.textColorFor(bgColor)
                                            bgTransparency = 0f
                                            autoTextColor = true
                                        }
                                        DARK -> {
                                            bgColor = darkBgColor
                                            textColor = Utils.textColorFor(bgColor)
                                            bgTransparency = 0f
                                            autoTextColor = true
                                        }
                                    }
                                }
                                if (widgetStyle == CUSTOM){
                                    ColorPreference(
                                        title = stringResource(R.string.widget_background),
                                        description = null,
                                        icon = null,
                                        enabled = true,
                                        color = Color(bgColor),
                                        onColorSelected = {bgColor = it.toArgb()}
                                    )
                                    SliderPreference(
                                        title = stringResource(R.string.widget_transparency),
                                        icon = null,
                                        enabled = true,
                                        onChanged = {bgTransparency = it},
                                        onValueChangeFinished = null,
                                        value = bgTransparency,
                                    )
                                    SwitchPreference(
                                        stringResource(R.string.widget_autotext),
                                        null,
                                        autoTextColor,
                                    ){
                                        autoTextColor = it
                                    }
                                    if (!autoTextColor){
                                        ColorPreference(
                                            title = stringResource(R.string.widget_text_color),
                                            description = null,
                                            icon = null,
                                            enabled = true,
                                            color = Color(textColor),
                                            onColorSelected = {textColor = it.toArgb()}
                                        )
                                    }
                                }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button({
                                        @Suppress("NAME_SHADOWING")
                                        val selectedAccount = selectedAccount ?: return@Button;
                                        if (widgetID != null){
                                            saveConfig(widgetID!!, bgColor, bgTransparency, textColor)
                                        }
                                        val resultValue = Intent()
                                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID)
                                        setResult(RESULT_OK, resultValue)

                                        widgetID?.let { widgetID ->
                                            lifecycleScope.launch{
                                                val rozvrh: Rozvrh? = (application as? MainApplication)?.repository?.getRozvrh(RozvrhRecord.Key(selectedAccount.id,cz.vitskalicky.lepsirozvrh.Utils.getCurrentMonday()),true)
                                                val tmp = rozvrh?.getWidgetDisplayBlocks(5)
                                                WidgetProvider.update(this@WidgetConfigActivity,widgetID,tmp?.first,selectedAccount.isTeacher(), tmp?.second )
                                                finish()
                                            }
                                        }?:finish()
                                    },
                                        Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                        enabled = selectedAccount != null,
                                    ){ Text(stringResource(R.string.ok)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveConfig(widgetID: Int, bgColor: Int, transparency: Float, textColor: Int) {
        val ws: WidgetsSettings.Widget = WidgetsSettings.Widget();

        ws.primaryTextSize = resources.getDimension(R.dimen.widgetTextPrimary) / resources.displayMetrics.scaledDensity;
        ws.secondaryTextSize = resources.getDimension(R.dimen.widgetTextSecondary) / resources.displayMetrics.scaledDensity;

        ws.primaryTextColor = textColor;
        ws.secondaryTextColor = calculateSecondaryTextColor(textColor)
        ws.backgroundColor = (bgColor and 0x00ffffff) or (((1f - transparency)*255f).roundToInt() shl 24);

        viewModel.saveWidgetConfig(widgetID, ws)
    }

    protected fun calculateSecondaryTextColor(primaryTextColor: Int): Int = (primaryTextColor and 0x00ffffff) or 0x99000000.toInt()

    @Composable
    abstract fun WidgetView(bgColor: Int, transparency: Float, textColor: Int)
}