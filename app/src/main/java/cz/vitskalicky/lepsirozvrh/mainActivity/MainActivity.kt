package cz.vitskalicky.lepsirozvrh.mainActivity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.SharedPrefsKt
import cz.vitskalicky.lepsirozvrh.accountPicker.AccountPickerActivity
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.prefs
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import cz.vitskalicky.lepsirozvrh.welcome.WelcomeActivity
import kotlinx.coroutines.launch

/** The main activity with schedule table*/
class MainActivity : ComponentActivity() {

    companion object {
        /** Jump to current lesson on open */ //todo implement this
        const val EXTRA_JUMP_TO_TODAY = "MainActivity.jump_to_today"
        /** switch to this account on open (extra is a long with the id). useful for persistent notification*/
        const val EXTRA_SWITCH_TO_ACCOUNT = "MainActivity.switch_to_account"
    }

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            // show welcome activity if the user hasn't seen it yet
            if (prefs.boolean(PrefsConsts.SEEN_WELCOME) != true){
                val intent = Intent(this@MainActivity, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
                return@launch
            }
            // if coming from notification, switch to the account used in the notification
            if (intent.hasExtra(EXTRA_SWITCH_TO_ACCOUNT)){
                val newAccount = intent.getLongExtra(EXTRA_SWITCH_TO_ACCOUNT, -1)
                val currentAccount = SharedPrefsKt(this@MainActivity).long(PrefsConsts.ACTIVE_ACCOUNT_ID)
                if (newAccount != currentAccount) {
                    (application as MainApplication).accountRepository.switchToAccount(newAccount);
                }
            }
            viewModel.getAccountIdLD().observe(this@MainActivity){
                if (it == null){
                    val intent = Intent(this@MainActivity, AccountPickerActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            setContent {
                LepsirozvrhTheme(hasAppBar = false, isRozvrhScreen = true) {
                    RozvrhWithControls(viewModel)

                    //if notifications have been suddenly disabled
                    var showNotiPermissionDialog: Boolean by rememberSaveable{
                        //if there is this key in shared preferences, show the dialog and delete the key
                        val prefs = SharedPrefsKt(this@MainActivity)
                        mutableStateOf(
                            prefs.boolean(PrefsConsts.NOTIFICATION_PLEASE_GRANT_PERMISSION)
                                .also { if (it == true) prefs.edit { remove(PrefsConsts.NOTIFICATION_PLEASE_GRANT_PERMISSION) } }
                                ?:false)
                    }
                    if (showNotiPermissionDialog){
                        PermanentNotification.ShowNoPermissionDialog { showNotiPermissionDialog = false }
                    }
                }
            }
        }
    }
}