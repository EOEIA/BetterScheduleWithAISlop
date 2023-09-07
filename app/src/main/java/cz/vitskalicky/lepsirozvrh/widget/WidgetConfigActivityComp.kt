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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.view.preferences.RadioPreference
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme

class WidgetConfigActivityComp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var widgetStyle: Int by rememberSaveable{mutableStateOf(0)}
            var bgColor: Color by rememberSaveable{mutableStateOf(MaterialTheme.colors.surface)}
            var textPrimaryColor: Color by rememberSaveable{ mutableStateOf(MaterialTheme.colors.onSurface) } //todo colors
            var textSecondaryColor: Color by rememberSaveable{ mutableStateOf(MaterialTheme.colors.onSurface.copy(alpha = 0.7f)) }
            var autoTextColor: Boolean by rememberSaveable{ mutableStateOf(true) }

            LepsirozvrhTheme {
                // A surface container using the 'background' color from the theme
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

                                background.imageAlpha = (bgColor.alpha *255).roundToInt()
                                background.setColorFilter(bgColor.toArgb() or 0xff000000.toInt())
                                //todo auto text color
                                textViewPrimary.setTextColor(textPrimaryColor.toArgb())
                                textViewSecondary.setTextColor(textSecondaryColor.toArgb())
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