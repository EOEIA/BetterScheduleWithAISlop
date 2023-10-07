package cz.vitskalicky.lepsirozvrh.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.vitskalicky.lepsirozvrh.KotlinUtils
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.model.AccountRepository
import cz.vitskalicky.lepsirozvrh.schoolPicker.SchoolInfo

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
        loginScreenStatusLD.value = LoginScreenStatus.LOADING

        val url: String = schoolInfo?.url ?: ""

        if (url.isBlank()) {
            loginScreenStatusLD.value = LoginScreenStatus.NO_SCHOOL
            return null
        }
        if (username.isBlank()) {
            loginScreenStatusLD.value = LoginScreenStatus.NO_USERNAME
            return null
        }
        if (password.isBlank()) {
            loginScreenStatusLD.value = LoginScreenStatus.NO_PASSWORD
            return null
        }
        val result = accountRepository.addAccount(url, username, password, schoolInfo?.isManual ?: false)

        when (result.status){
            AccountRepository.LoginResultStatus.WRONG_LOGIN -> {
                loginScreenStatusLD.value = LoginScreenStatus.WRONG_LOGIN
                return null
            }
            AccountRepository.LoginResultStatus.UNREACHABLE -> {
                loginScreenStatusLD.value = if (KotlinUtils.isOnline()){
                    if (schoolInfo?.isManual == true){
                        LoginScreenStatus.MANUAL_URL_UNREACHABLE
                    }else{
                        LoginScreenStatus.SCHOOL_UNREACHABLE
                    }
                }else {
                    LoginScreenStatus.NO_INTERNET
                }
                return null
            }
            AccountRepository.LoginResultStatus.UNEXPECTED_RESPONSE -> {
                loginScreenStatusLD.value = LoginScreenStatus.UNEXPECTED_RESPONSE
                return null
            }
            AccountRepository.LoginResultStatus.SUCCESS -> {
                //set active account and start main activity
                accountRepository.switchToAccount(result.account!!.id)
                loginScreenStatusLD.value = LoginScreenStatus.SUCCESS
                return result.account.id //the activity must start the main activity
            }
        }
    }
}