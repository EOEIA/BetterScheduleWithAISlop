package cz.vitskalicky.lepsirozvrh.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.vitskalicky.lepsirozvrh.KotlinUtils
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.compose.LoginScreenStatus
import cz.vitskalicky.lepsirozvrh.compose.StatefulLoginForm
import cz.vitskalicky.lepsirozvrh.model.AccountRepository
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolInfo
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
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import kotlinx.coroutines.launch

class LoginViewModel(application: MainApplication): AndroidViewModel(application) {
    private val accountRepository = application.accountRepository
    private val loginScreenStatusLD= MutableLiveData<LoginScreenStatus>(LoginScreenStatus.UNKNOWN)
    fun getLoginScreenStatusLD(): LiveData<LoginScreenStatus> = loginScreenStatusLD

    suspend fun login(schoolInfo: SchoolInfo?, username: String, password: String){
        loginScreenStatusLD.value = LOADING

        val url: String = schoolInfo?.url ?: ""

        if (url.isBlank()) {
            loginScreenStatusLD.value = NO_SCHOOL
            return
        }
        if (username.isBlank()) {
            loginScreenStatusLD.value = NO_USERNAME
            return
        }
        if (password.isBlank()) {
            loginScreenStatusLD.value = NO_PASSWORD
            return
        }
        val result = accountRepository.addAccount(url, username, password, schoolInfo?.isManual ?: false)

        when (result.status){
            AccountRepository.LoginResultStatus.WRONG_LOGIN -> {
                loginScreenStatusLD.value = WRONG_LOGIN
                return
            }
            AccountRepository.LoginResultStatus.UNREACHABLE -> {
                loginScreenStatusLD.value = if (KotlinUtils.isOnline()){
                    if (schoolInfo?.isManual == true){
                        MANUAL_URL_UNREACHABLE
                    }else{
                        SCHOOL_UNREACHABLE
                    }
                }else {
                    NO_INTERNET
                }
                return
            }
            AccountRepository.LoginResultStatus.UNEXPECTED_RESPONSE -> {
                loginScreenStatusLD.value = UNEXPECTED_RESPONSE
                return
            }
            AccountRepository.LoginResultStatus.SUCCESS -> {
                loginScreenStatusLD.value = SUCCESS
                //todo success
            }
        }
    }
}

class LoginActivity : ComponentActivity() {
    val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            LepsirozvrhTheme {
                StatefulLoginForm(
                    viewModel.getLoginScreenStatusLD(),
                    {schoolInfo, username, password -> scope.launch { viewModel.login(schoolInfo, username, password) } }
                )
            }
        }
    }
}