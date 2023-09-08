package cz.vitskalicky.lepsirozvrh.activity

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.vitskalicky.lepsirozvrh.*
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

class LoginViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as MainApplication
    private val accountRepository = app.accountRepository
    private val loginScreenStatusLD= MutableLiveData<LoginScreenStatus>(LoginScreenStatus.UNKNOWN)
    fun getLoginScreenStatusLD(): LiveData<LoginScreenStatus> = loginScreenStatusLD

    /** Tries to log in. [loginScreenStatusLD] is updated with the result, `null` is returned if it is fail or the new
     * account's ID, if successful. */
    suspend fun login(schoolInfo: SchoolInfo?, username: String, password: String): Long?{
        loginScreenStatusLD.value = LOADING

        val url: String = schoolInfo?.url ?: ""

        if (url.isBlank()) {
            loginScreenStatusLD.value = NO_SCHOOL
            return null
        }
        if (username.isBlank()) {
            loginScreenStatusLD.value = NO_USERNAME
            return null
        }
        if (password.isBlank()) {
            loginScreenStatusLD.value = NO_PASSWORD
            return null
        }
        val result = accountRepository.addAccount(url, username, password, schoolInfo?.isManual ?: false)

        when (result.status){
            AccountRepository.LoginResultStatus.WRONG_LOGIN -> {
                loginScreenStatusLD.value = WRONG_LOGIN
                return null
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
                return null
            }
            AccountRepository.LoginResultStatus.UNEXPECTED_RESPONSE -> {
                loginScreenStatusLD.value = UNEXPECTED_RESPONSE
                return null
            }
            AccountRepository.LoginResultStatus.SUCCESS -> {
                //set active account and start main activity
                accountRepository.switchToAccount(result.account!!.id)
                loginScreenStatusLD.value = SUCCESS
                return result.account.id //the activity must start the main activity
            }
        }
    }
}

class LoginActivity : ComponentActivity() {
    val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appTasks = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).appTasks
        val activitiesCount: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            appTasks.map { it.taskInfo.numActivities}.sum()
        } else {
            // did not find a reliable way to count previous activities on older API.
            // the few people with old phones will have to cope with missing back arrow
            1
        }
        val showAppBar = activitiesCount != 1

        setContent {
            val scope = rememberCoroutineScope()
            LepsirozvrhTheme(hasAppBar = showAppBar) {
                Scaffold(
                    topBar = if (!showAppBar) {{}} else {
                        {
                            TopAppBar(
                            title = { Text(stringResource(R.string.login_add_title)) },
                            navigationIcon = {
                                    IconButton({
                                        finish()
                                    }) {
                                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                                    }
                                }
                        ) }
                    }
                ) {paddingValues->
                    Box(Modifier.padding(paddingValues)) {
                        StatefulLoginForm(
                            viewModel.getLoginScreenStatusLD(),
                            { schoolInfo, username, password ->
                                scope.launch {
                                    val accountId: Long? = viewModel.login(schoolInfo, username, password)
                                    if (accountId != null) {
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        startActivity(intent)
                                        finishAffinity()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

    }
}