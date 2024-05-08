package cz.vitskalicky.lepsirozvrh.view

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Adds a line for error text, but does not implement all features of TextField*/
@OptIn(ExperimentalComposeUiApi::class) //for autofill
@Composable
fun TextFieldWithError(
    value: String,
    onValueChange: (String) -> Unit,
    textFieldModifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    errorMessage: String? = null,
    columnModifier: Modifier = Modifier,
    textFieldInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    autofillTypes: List<AutofillType>? = null // leave null to disable autofill
){
    var tfMod = textFieldModifier;
    Column(modifier = columnModifier) {
        val afn: AutofillNode? = if (autofillTypes == null) null else {
            AutofillNode(
                autofillTypes = autofillTypes,
                onFill = { onValueChange(it) }
            )
        }
        if (afn != null) {
            LocalAutofillTree.current += afn;
            val autofill = LocalAutofill.current;
            tfMod = tfMod.onGloballyPositioned {
                afn.boundingBox = it.boundsInWindow();
            }.onFocusChanged {focusState ->
                autofill?.run {
                    if (focusState.isFocused) {
                        requestAutofillForNode(afn)
                    } else {
                        cancelAutofillForNode(afn)
                    }
                }
            }
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = tfMod,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = errorMessage != null,
            interactionSource = textFieldInteractionSource,
        )
        Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colors.error.copy(alpha = ContentAlpha.medium),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}