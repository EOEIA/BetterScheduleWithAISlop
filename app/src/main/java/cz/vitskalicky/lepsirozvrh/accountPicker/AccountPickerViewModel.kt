package cz.vitskalicky.lepsirozvrh.accountPicker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.model.Account

class AccountPickerViewModel(application: Application): AndroidViewModel(application) {
    private val app = application.applicationContext as MainApplication
    private val accountRepository = app.accountRepository

    val accountsLD: LiveData<List<Account>>
        get() = accountRepository.getAccountsLD()

    val currentAccountIdLD: LiveData<Long?> = SharedPrefsLongLiveData(app.prefs.sharedPreferences,PrefsConsts.ACTIVE_ACCOUNT_ID, -1L).map { it.takeUnless { it == -1L } }

    suspend fun switchToAccount(id: Long){
        accountRepository.switchToAccount(id);
    }
}