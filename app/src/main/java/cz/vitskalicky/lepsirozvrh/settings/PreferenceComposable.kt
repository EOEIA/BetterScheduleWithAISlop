package cz.vitskalicky.lepsirozvrh.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R

@Composable
private fun PreferenceBase(
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
    rightContent: (@Composable () -> Unit)? = null,
    onClicked: () -> Unit = {},
    enabled: Boolean = true,
){

    Row(Modifier
        .alpha(if (enabled) {1f} else {ContentAlpha.disabled})
        .clickable(enabled,null, null, if (enabled) onClicked else {{}})
    ) {
        Box(Modifier.width(88.dp)){
            icon?.invoke()
        }
        content()
        Spacer(Modifier.fillMaxWidth().weight(0f))
        rightContent?.invoke()
    }
}

@Composable
fun Preference(
    title: String?,
    description: String?,
    icon: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClicked: () -> Unit
){
    PreferenceBase(icon = icon, rightContent = rightContent, onClicked = onClicked, enabled = enabled, content = {
        Column {
            if (!title.isNullOrBlank()) Text(title)
            if (!description.isNullOrBlank()) Text(description)
        }
    })
}

@Composable
fun SwitchPreference(
    title: String?,
    description: String?,
    checked: Boolean,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onChanged: (newValue: Boolean) -> Unit,
){
    Preference(title, description, onClicked = {onChanged(!checked)}, icon = icon, enabled = enabled, rightContent = {
        Switch(checked, onCheckedChange = {onChanged(it)})
    })
}

@Composable
fun RadioPreference(
    title: String?,
    description: String?,
    options: List<String>,
    selectedOptionIndex: Int?, // null for none
    dialogTitle: (@Composable () -> Unit)?,
    icon: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onSelected: (optionIndex: Int) -> Unit,
){
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    Preference(title, description, icon, rightContent, enabled, onClicked = {isDialogOpen = true})
    if (isDialogOpen){
        AlertDialog(
            onDismissRequest = {isDialogOpen = false},
            confirmButton = {},
            dismissButton = { TextButton({isDialogOpen = false}){Text(stringResource(R.string.cancel))} },
            title = dialogTitle,
            text = {
                Column {
                    for (item in options.withIndex()){
                        val index = item.index
                        val option = item.value

                        Row (Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = index == selectedOptionIndex,
                                onClick = {
                                    isDialogOpen = false
                                    onSelected(index)
                                },
                                role = Role.RadioButton,
                            )
                        ) {
                            RadioButton(index == selectedOptionIndex, onClick = null)
                            Text(option)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun PreferenceGroupHeader(title: String){
    Text(title, modifier = Modifier.padding(start = 88.dp))
}