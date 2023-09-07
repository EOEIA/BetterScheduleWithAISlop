package cz.vitskalicky.lepsirozvrh.widget

import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cz.vitskalicky.lepsirozvrh.R
import kotlin.math.roundToInt

class SmallWidgetConfigActivity: WidgetConfigActivity() {
    @Composable
    override fun WidgetView(bgColor: Int, transparency: Float, textColor: Int) {
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

                background.imageAlpha = ((1f-transparency) * 255).roundToInt()
                background.setColorFilter(bgColor or 0xff000000.toInt())
                textViewPrimary.setTextColor(textColor)
                textViewSecondary.setTextColor(calculateSecondaryTextColor(textColor))
            })
    }
}