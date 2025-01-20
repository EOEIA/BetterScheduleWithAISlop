package cz.vitskalicky.lepsirozvrh.widget

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import cz.vitskalicky.lepsirozvrh.AppSingleton
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.ui.theme.LepsirozvrhTheme
import kotlinx.coroutines.launch

/** Account picker for widgets whose account has been logged out. Launched on tap on such a widget*/
class WidgetChangeAccountActivity : ComponentActivity() {

    companion object {
        const val EXTRA_WIDGET_ID = "WidgetChangeAccountActivity.EXTRA_WIDGET_ID"
    }

    private val viewModel: WidgetChangeAccountViewModel by viewModels()
    private var widgetID: Int? = null
    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetID = intent.extras?.takeIf { it.containsKey(EXTRA_WIDGET_ID) }?.getInt(EXTRA_WIDGET_ID)
        if (widgetID == null){
            //todo is this the right thing to do?
            finish()
        }

        setContent {
            LepsirozvrhTheme {
                // A surface container using the 'background' color from the theme
                val accounts by viewModel.accountsLD.observeAsState();
                AlertDialog(
                    this::onDismissed,
                    {},
                    dismissButton = { TextButton(this::onDismissed){ Text(stringResource(R.string.cancel)) } },
                    title = { Text(stringResource(R.string.widget_select_new_account)) },
                    text = {
                        Column {
                            for (account in accounts ?: emptyList()) {
                                ListItem(Modifier.clickable {
                                    lifecycleScope.launch {
                                        onSelected(account)
                                    }
                                }) {
                                    Text(account.fullName)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private suspend fun onSelected(account: Account){
        val sngl = AppSingleton.getInstance(this)
        val options = sngl.widgetsSettings.widgets[widgetID]
        if (options != null){
            options.accountId = account.id
            sngl.saveWidgetsSettings()
            WidgetProvider.updateAll(application as MainApplication)
        }
        finish()

    }
    private fun onDismissed(){
        finish()
    }
}

class WidgetChangeAccountViewModel( application: Application): AndroidViewModel(application){
    private val app = application as MainApplication
    private val accountRepository = app.accountRepository
    val accountsLD = accountRepository.getAccountsLD()
}