package cz.vitskalicky.lepsirozvrh.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.vitskalicky.lepsirozvrh.R

/** Does not implement all features of TextField*/
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
    columnModifier: Modifier = Modifier
){
    Column(modifier = columnModifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = textFieldModifier,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = errorMessage != null
        )
        Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colors.error.copy(alpha = ContentAlpha.medium),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}

@Composable
fun LoginForm(
    school: String, username: String, password: String,
    loading: Boolean,
    schoolError: String?, usernameError: String?, passwordError: String?, genericError: String?,
    /** When change school button pressed */
    onSchoolChange: ()->Unit,
    onUsernameChange: (String)-> Unit,
    onPasswordChange: (String)->Unit,
    onLogin: ()->Unit,){
    var showPassword: Boolean by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxHeight()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 0.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painterResource(R.drawable.bakalari_logo),
                stringResource(R.string.bakalari_logo)
            )
        }
        Text(stringResource(R.string.login_message),
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = !loading,onClick = onSchoolChange)
        ) { // School
            TextFieldWithError(
                value = school.ifBlank { stringResource(R.string.no_school_selected) },
                onValueChange = {_->},
                label = { Text(stringResource(R.string.school)) },
                readOnly = true,
                textFieldModifier = Modifier.semantics { if (schoolError != null) error(schoolError) },
                columnModifier = Modifier.weight(1f),
                errorMessage = if (!loading) schoolError else null,
                enabled = !loading
            )
            Spacer(Modifier.size(8.dp))
            Button(
                enabled = !loading,
                onClick = onSchoolChange
            ){
                Text(stringResource(if (school.isBlank()) R.string.choose_school else R.string.change_school).uppercase())
            }
        }
        TextFieldWithError(
            value = username,
            onValueChange = onUsernameChange,
            enabled = !loading,
            label = {Text(stringResource(R.string.username))},
            errorMessage = if (!loading) usernameError else null,
            singleLine = true,
            textFieldModifier = Modifier.fillMaxWidth()
        )
        TextFieldWithError(
            value = password,
            onValueChange = onPasswordChange,
            enabled = !loading,
            label = { Text(stringResource(R.string.password)) },
            textFieldModifier = Modifier.fillMaxWidth(),
            errorMessage = if (!loading) passwordError else null,
            singleLine = true,
            visualTransformation =  if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    val visibilityIcon =
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    // Please provide localized description for accessibility services
                    val description =
                        if (showPassword) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    Icon(imageVector = visibilityIcon, contentDescription = description)
                }
            }
        )
        Spacer(Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (!loading && genericError != null){
                Text(genericError, color = MaterialTheme.colors.error)
            }
            if(loading){
                CircularProgressIndicator()
            }
            Button(
                onClick = onLogin,
                modifier = Modifier.padding(vertical = 8.dp),
                enabled = !loading,
            ){
                Text(stringResource(R.string.login_button).uppercase())
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginFormPreview(){
    LoginForm(
        school = "Škola čas a kouzel v Bradavicích",
        username = "snapesev",
        password = "jamesiscunt",
        loading = false,
        schoolError = null,
        usernameError = null,
        passwordError = null,
        genericError = null,
        onSchoolChange = {},
        onUsernameChange = { _->},
        onPasswordChange = { _->},
        onLogin = {})
}