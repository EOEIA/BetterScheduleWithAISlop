package cz.vitskalicky.lepsirozvrh.widget

import android.util.TypedValue
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

/** Configuration activity for wide widget. Most logic is in [WidgetConfigActivity]. This only proved wide widget specific view and stuff */
class WideWidgetConfigActivity: WidgetConfigActivity() {
    @Composable
    override fun WidgetView(bgColor: Int, transparency: Float, textColor: Int) {
        AndroidView(
            modifier = Modifier.width(300.dp).height(90.dp),
            factory = {
                val view = layoutInflater.inflate(R.layout.wide_widget, null);
                view
            },
            update = { view ->
                val secondaryTextColor: Int = calculateSecondaryTextColor(textColor)

                //find views
                val textViewPrimary0: TextView = view.findViewById(R.id.textViewZkrpr0);
                val textViewSecondary0: TextView = view.findViewById(R.id.textViewSecondary0);
                val textViewPrimary1: TextView = view.findViewById(R.id.textViewZkrpr1);
                val textViewSecondary1: TextView = view.findViewById(R.id.textViewSecondary1);
                val textViewPrimary2: TextView = view.findViewById(R.id.textViewZkrpr2);
                val textViewSecondary2: TextView = view.findViewById(R.id.textViewSecondary2);
                val textViewPrimary3: TextView = view.findViewById(R.id.textViewZkrpr3);
                val textViewSecondary3: TextView = view.findViewById(R.id.textViewSecondary3);
                val textViewPrimary4: TextView = view.findViewById(R.id.textViewZkrpr4);
                val textViewSecondary4: TextView = view.findViewById(R.id.textViewSecondary4);

                val background: ImageView = view.findViewById(R.id.bgcolor);
                val divider: ImageView = view.findViewById(R.id.imageViewDivider);

                //set background color
                background.imageAlpha = ((1f-transparency) * 255).roundToInt()
                background.setColorFilter(bgColor or 0xff000000.toInt())

                //set primary text color
                textViewPrimary0.setTextColor(textColor);
                textViewPrimary0.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextPrimary));

                textViewPrimary1.setTextColor(textColor);
                textViewPrimary1.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextPrimary));

                textViewPrimary2.setTextColor(textColor);
                textViewPrimary2.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextPrimary));

                textViewPrimary3.setTextColor(textColor);
                textViewPrimary3.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextPrimary));

                textViewPrimary4.setTextColor(textColor);
                textViewPrimary4.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextPrimary));

                //set divider color
                divider.setImageAlpha(255);
                divider.setColorFilter(textColor);

                //set secondary text color
                textViewSecondary0.setTextColor(secondaryTextColor);
                textViewSecondary0.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextSecondary));

                textViewSecondary1.setTextColor(secondaryTextColor);
                textViewSecondary1.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextSecondary));

                textViewSecondary2.setTextColor(secondaryTextColor);
                textViewSecondary2.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextSecondary));

                textViewSecondary3.setTextColor(secondaryTextColor);
                textViewSecondary3.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextSecondary));

                textViewSecondary4.setTextColor(secondaryTextColor);
                textViewSecondary4.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.widgetTextSecondary));
            })
    }
}