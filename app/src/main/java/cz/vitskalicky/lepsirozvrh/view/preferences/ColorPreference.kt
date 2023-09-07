package cz.vitskalicky.lepsirozvrh.view.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import cz.vitskalicky.lepsirozvrh.R

@Composable
fun ColorPreferenceStateless(
    title: String?,
    description: String?,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    /** The selected color (and confirmed by the OK button)*/
    color: Color,
    /** The color currently selected on the color picker, but not confirmed yet.*/
    pickerValue: Color = color,
    onPickerValueChanged: (color: Color) -> Unit,
    onColorSelected: (color: Color) -> Unit,
    isDialogOpen: Boolean,
    onOpenDialog: () -> Unit,
    onDialogDismissed: () -> Unit,
){
    if (isDialogOpen){
        AlertDialog(
            onDismissRequest = onDialogDismissed,
            buttons = {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    TextButton(onDialogDismissed){ Text(stringResource(R.string.cancel)) }
                    TextButton({
                        onColorSelected(pickerValue)
                        onDialogDismissed()
                    }){ Text(stringResource(R.string.ok)) }
                }
            },
            text = {
                HarmonyColorPicker(
                    harmonyMode = ColorHarmonyMode.ANALOGOUS,
                    color = HsvColor.from(pickerValue),
                    onColorChanged = {onPickerValueChanged(it.toColor())},
                    showBrightnessBar = true,
                    modifier = Modifier.height(400.dp),
                )
            }
        )
    }
    Preference(
        title = title,
        description = description,
        icon = icon,
        rightContent = {
                       Surface(
                           color = color,
                           shape = CircleShape,
                           border = BorderStroke(1.dp, Color.Black),
                           modifier = Modifier.size(24.dp)
                       ) {  }
        },
        enabled = enabled,
        onClicked = onOpenDialog
    )
}

@Composable
fun ColorPreference(
    title: String?,
    description: String?,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    /** The selected color (and confirmed by the OK button)*/
    color: Color,
    onColorSelected: (color: Color) -> Unit,
){
    var isDialogOpen by rememberSaveable{ mutableStateOf(false) }
    var pickerColor: Int by rememberSaveable{ mutableStateOf(color.toArgb()) }
    ColorPreferenceStateless(
        title,
        description,
        icon,
        enabled,
        color,
        Color(pickerColor),
        {pickerColor = it.toArgb() },
        onColorSelected,
        isDialogOpen,
        {isDialogOpen = true},
        {isDialogOpen = false }
    )
}
