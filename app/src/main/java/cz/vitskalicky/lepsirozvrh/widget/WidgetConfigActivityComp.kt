package cz.vitskalicky.lepsirozvrh.widget

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.theme.Theme.Utils
import cz.vitskalicky.lepsirozvrh.view.preferences.RadioPreference
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import cz.vitskalicky.lepsirozvrh.view.preferences.ColorPreference
import cz.vitskalicky.lepsirozvrh.view.preferences.SliderPreference
import cz.vitskalicky.lepsirozvrh.view.preferences.SwitchPreference
import kotlin.math.roundToInt

class WidgetConfigActivityComp : ComponentActivity() {
    companion object {
        /** Must match R.array.widget_style_entries*/
        private const val LIGHT = 0
        /** Must match R.array.widget_style_entries*/
        private const val DARK = 1
        /** Must match R.array.widget_style_entries*/
        private const val CUSTOM = 2
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val lightBgColor = colorResource(R.color.widgetLightBackground).toArgb()
            val darkBgColor = colorResource(R.color.widgetDarkBackground).toArgb()

            var widgetStyle: Int by rememberSaveable{mutableStateOf(LIGHT)}

            var bgColor: Int by rememberSaveable{mutableStateOf(lightBgColor)}
            var bgTransparency: Float by rememberSaveable{ mutableStateOf(0f) }
            var textColor: Int by rememberSaveable{ mutableStateOf(Utils.textColorFor(lightBgColor)) }
            var autoTextColor: Boolean by rememberSaveable{ mutableStateOf(true) }

            LepsirozvrhTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.weight(1f, fill = false).fillMaxSize(), contentAlignment = Alignment.Center){
                            AndroidView(
                                modifier = Modifier.width(65.dp).height(90.dp),
                                factory = {
                                val view = layoutInflater.inflate(R.layout.small_widget, null);
                                view
                            },
                            update = { view ->
                                val textViewPrimary: TextView = view.findViewById(R.id.textViewZkrpr)
                                val textViewSecondary: TextView = view.findViewById(R.id.textViewSecondary)
                                val background: ImageView = view.findViewById(R.id.bgcolor)

                                background.imageAlpha = ((1f-bgTransparency) * 255).roundToInt()
                                background.setColorFilter(bgColor or 0xff000000.toInt())
                                val finalTextColor = if (autoTextColor) Utils.textColorFor(bgColor) else textColor
                                textViewPrimary.setTextColor(finalTextColor)
                                textViewSecondary.setTextColor((finalTextColor and 0x00ffffff) or 0x99000000.toInt())
                            })
                        }
                        Surface(Modifier.weight(2f, fill = false).fillMaxWidth(), color = MaterialTheme.colors.surface, elevation = 8.dp ) {
                            val scrollState = rememberScrollState()
                            Column(Modifier.verticalScroll(scrollState)) {
                                Spacer(Modifier.size(16.dp))

                                    val styleOptions = stringArrayResource(R.array.widget_style_entries)
                                    val styleText = styleOptions[widgetStyle]
                                RadioPreference(stringResource(R.string.widget_style), styleText, styleOptions.toList(),widgetStyle, {}){
                                    widgetStyle = it
                                    when (widgetStyle){
                                        LIGHT -> {
                                            bgColor = lightBgColor
                                            textColor = Utils.textColorFor(bgColor)
                                            bgTransparency = 0f
                                        }
                                        DARK -> {
                                            bgColor = darkBgColor
                                            textColor = Utils.textColorFor(bgColor)
                                            bgTransparency = 0f
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
                                    ){autoTextColor = it}
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
                                        //todo accept
                                    },
                                        Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                                    ){ Text(stringResource(R.string.ok)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}