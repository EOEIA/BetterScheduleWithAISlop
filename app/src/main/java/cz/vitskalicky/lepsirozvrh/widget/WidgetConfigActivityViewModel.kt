package cz.vitskalicky.lepsirozvrh.widget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cz.vitskalicky.lepsirozvrh.AppSingleton
import cz.vitskalicky.lepsirozvrh.MainApplication

class WidgetConfigActivityViewModel(application: Application): AndroidViewModel(application) {
    private val app = application as MainApplication
    private val accountRepository = app.accountRepository
    val accountsLD = accountRepository.getAccountsLD()

    fun saveWidgetConfig(widgetId: Int, config: WidgetsSettings.Widget){
        val appSingleton: AppSingleton = AppSingleton.getInstance(app);

        appSingleton.getWidgetsSettings().widgetIds.add(widgetId);
        appSingleton.getWidgetsSettings().widgets.put(widgetId, config);

        appSingleton.saveWidgetsSettings();
    }
}