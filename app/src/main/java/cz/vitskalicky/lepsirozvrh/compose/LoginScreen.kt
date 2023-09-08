package cz.vitskalicky.lepsirozvrh.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolInfo
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.UNKNOWN
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.LOADING
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.SUCCESS
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.NO_SCHOOL
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.NO_USERNAME
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.NO_PASSWORD
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.WRONG_LOGIN
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.SCHOOL_UNREACHABLE
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.MANUAL_URL_UNREACHABLE
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.NO_INTERNET
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus.UNEXPECTED_RESPONSE
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolList

/** Adds a line for error text, but does not implement all features of TextField*/
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
    textFieldInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }
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

/** The UI of login form, but it does not keep any data in state. Just the UI*/
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

    // detect click on school text field
    val schoolTextFieldInteractionSource = remember { MutableInteractionSource() }
    if (schoolTextFieldInteractionSource.collectIsPressedAsState().value && !loading){
        onSchoolChange()
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxHeight()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
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
                enabled = !loading,
                textFieldInteractionSource = schoolTextFieldInteractionSource,
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!loading && genericError != null){
                Text(genericError, color = MaterialTheme.colors.error)
            }
            if(loading){
                CircularProgressIndicator()
            }
            Spacer(Modifier.size(16.dp))
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

enum class LoginScreenStatus{
    UNKNOWN,
    LOADING,
    SUCCESS,
    NO_SCHOOL,
    NO_USERNAME,
    NO_PASSWORD,
    WRONG_LOGIN,
    SCHOOL_UNREACHABLE,
    MANUAL_URL_UNREACHABLE,
    NO_INTERNET,
    UNEXPECTED_RESPONSE
}

/** Extends [LoginForm] and adds some data state handling.*/
@Composable
fun StatefulLoginForm(
    loginScreenStatus: LiveData<LoginScreenStatus>,
    onSubmit: (schoolInfo: SchoolInfo?, username: String, password:String) -> Unit,
    defaultSchoolInfo: SchoolInfo? = null,
    defaultUsername: String = "",
    defaultPassword:String = "",
){
    var schoolInfo by rememberSaveable { mutableStateOf(defaultSchoolInfo) }
    var username by rememberSaveable { mutableStateOf(defaultUsername) }
    var password by rememberSaveable { mutableStateOf(defaultPassword) }
    val status: LoginScreenStatus? by loginScreenStatus.observeAsState()
    var hideErrors by rememberSaveable { mutableStateOf(false) }
    var showSchoolPicker by rememberSaveable{ mutableStateOf(false) }

    if (showSchoolPicker){
        Dialog(
            onDismissRequest = {showSchoolPicker = false},
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ), )
        {
            Surface(
                color = MaterialTheme.colors.background
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(stringResource(R.string.schools_dialog_title))
                        },
                        navigationIcon = {
                            IconButton(onClick = {showSchoolPicker = false}){
                                Icon(Icons.Default.Close, stringResource(R.string.close))
                            }
                        } )
                    SchoolList({ pickedSchoolInfo ->
                        showSchoolPicker = false
                        schoolInfo = pickedSchoolInfo
                    })
                }
            }
        }
    }
    LoginForm(
        schoolInfo?.name ?: "",
        username,
        password,
        status == LOADING,
        if (hideErrors) null else when (status){
            NO_SCHOOL -> stringResource(R.string.no_school_selected)
            SCHOOL_UNREACHABLE -> stringResource(R.string.school_unreachable)
            MANUAL_URL_UNREACHABLE -> stringResource(R.string.unreachable)
            else -> null
        },
        if (hideErrors) null else when (status){
            NO_USERNAME -> stringResource(R.string.enter_username)
            WRONG_LOGIN -> stringResource(R.string.invalid_login)
            else -> null
        },
        if (hideErrors) null else when (status){
            NO_PASSWORD -> stringResource(R.string.enter_password)
            WRONG_LOGIN -> stringResource(R.string.invalid_login)
            else -> null
        },
        if (hideErrors) null else when (status){
            NO_INTERNET -> stringResource(R.string.no_internet)
            UNEXPECTED_RESPONSE -> stringResource(R.string.unexpected_response)
            else -> null
        },
        onSchoolChange = {showSchoolPicker = true; hideErrors = true},
        onUsernameChange = {value->username = value; hideErrors = true},
        onPasswordChange = {value -> password = value; hideErrors = true},
        onLogin = {hideErrors = false; onSubmit(schoolInfo, username, password)}
    )
}

@Preview(showBackground = true)
@Composable
fun LoginFormPreview(){
    StatefulLoginForm(
        MutableLiveData(UNKNOWN),
        {_,_,_ ->}
    )
}