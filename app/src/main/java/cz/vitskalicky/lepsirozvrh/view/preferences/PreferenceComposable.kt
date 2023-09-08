package cz.vitskalicky.lepsirozvrh.view.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R

// Composables for preferences UI
@Composable
private fun PreferenceBase(
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
    rightContent: (@Composable () -> Unit)? = null,
    onClicked: () -> Unit = {},
    enabled: Boolean = true,
){

    Surface(color = MaterialTheme.colors.surface) {
        Row(Modifier
            .alpha(if (enabled) {1f} else {ContentAlpha.disabled})
            .clickable(enabled,null, null, if (enabled) onClicked else {{}})
            .defaultMinSize(minHeight = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(72.dp), contentAlignment = Alignment.Center){
                icon?.invoke()
            }
            content()
            Spacer(Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.size(16.dp))
            rightContent?.invoke()
            Spacer(Modifier.size(16.dp))
        }
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
            if (!title.isNullOrBlank()) Text(title, style = MaterialTheme.typography.subtitle1)
            if (!description.isNullOrBlank()) Text(description, style = MaterialTheme.typography.caption)
        }
    })
}

@Preview
@Composable
fun PreferencePreview(){
    Surface(
        color = MaterialTheme.colors.surface
    ) {
        Preference("Appearence", "Customize app's look", { Icon(Icons.Default.Palette, "Palette icon") }) {}
    }
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
private fun RadioPreferenceStateless(
    title: String?,
    description: String?,
    options: List<String>,
    selectedOptionIndex: Int?, // null for none
    dialogTitle: (@Composable () -> Unit)?,
    icon: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    isDialogOpen: Boolean,
    onDismissDialog: () -> Unit,
    onOpenDialog: () -> Unit,
    onSelected: (optionIndex: Int) -> Unit,
){
    Preference(title, description, icon, rightContent, enabled, onClicked = onOpenDialog)
    if (isDialogOpen){
        AlertDialog(
            onDismissRequest = onDismissDialog,
            confirmButton = {},
            dismissButton = { TextButton(onDismissDialog){Text(stringResource(R.string.cancel))} },
            title = dialogTitle,
            text = {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                    Column(Modifier.padding(top = 35.dp)) {
                        for (item in options.withIndex()) {
                            val index = item.index
                            val option = item.value

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .selectable(
                                        selected = index == selectedOptionIndex,
                                        onClick = {
                                            onDismissDialog()
                                            onSelected(index)
                                        },
                                        role = Role.RadioButton,
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(index == selectedOptionIndex, onClick = null)
                                Spacer(Modifier.size(32.dp))
                                Text(option, style = MaterialTheme.typography.subtitle1)
                            }
                        }
                    }
                }
            }
        )
    }
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
    RadioPreferenceStateless(
        title,
        description,
        options,
        selectedOptionIndex,
        dialogTitle,
        icon,
        rightContent,
        enabled,
        isDialogOpen,
        { isDialogOpen = false },
        { isDialogOpen = true },
        onSelected
    )
}

@Composable
fun SliderPreference(
    title: String?,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onChanged: (value: Float) -> Unit = {},
    onValueChangeFinished: (() -> Unit)? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f .. 1f,
    steps: Int = 0,
    sliderColors: SliderColors = SliderDefaults.colors()
){
    PreferenceBase(
        icon = icon,
        enabled = enabled,
        content = {
            Column {
                if (title != null) {
                    Text(title, style = MaterialTheme.typography.subtitle1, modifier = Modifier.paddingFromBaseline(32.dp))
                }
                Slider(
                    modifier = Modifier.padding(end = 8.dp),
                    value = value,
                    onValueChange = onChanged,
                    enabled = enabled,
                    valueRange = valueRange,
                    onValueChangeFinished = onValueChangeFinished,
                    steps = steps,
                    colors = sliderColors
                )
            }
        },
    )
}

@Preview
@Composable
private fun SliderPreferencePriview(){
    SliderPreference("Transparency", value = 0.5f)
}

@Preview
@Composable
fun RadioPreferencePreview(){
    Surface(
        color = MaterialTheme.colors.surface
    ) {
        RadioPreferenceStateless(
            title = "Persistent notification",
            description = "Disabled",
            options = listOf("one","two"),
            selectedOptionIndex = 0,
            dialogTitle = {Text("Select best option")},
            icon = {Icon(Icons.Default.Notifications,null)},
            isDialogOpen = false,
            onDismissDialog = {},
            onOpenDialog = {},
            rightContent = { Icon(Icons.Default.NavigateNext, null) },
        ){}
    }
}

@Preview
@Composable
fun RadioPreferenceDialogPreview(){
    Surface(
        color = MaterialTheme.colors.surface
    ) {
        RadioPreferenceStateless(
            title = "Persistent notification",
            description = "Disabled",
            options = listOf("one","two"),
            selectedOptionIndex = 0,
            dialogTitle = {Text("Select best option")},
            icon = {Icon(Icons.Default.Notifications,null)},
            isDialogOpen = true,
            onDismissDialog = {},
            onOpenDialog = {},
            rightContent = { Icon(Icons.Default.NavigateNext, null) },
        ){}
    }
}

@Composable
fun PreferenceGroupHeader(title: String){
    Text(title,
        style = MaterialTheme.typography.subtitle2,
        modifier = Modifier
            .padding(start = 72.dp)
            .height(34.dp)
            .paddingFrom(FirstBaseline, before = 28.dp),
        color = MaterialTheme.colors.secondary)
}

@Preview
@Composable
private fun GrouHEaderPreview(){
    PreferenceGroupHeader("Appearance")
}