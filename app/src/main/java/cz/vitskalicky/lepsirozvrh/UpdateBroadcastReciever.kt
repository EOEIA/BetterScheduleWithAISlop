package cz.vitskalicky.lepsirozvrh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.widget.WidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Broadcast receiver that updates notification and widgets when receives a broadcast.
 */
class UpdateBroadcastReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "UpdateBroadcastReciever received intent")
        val application = context.applicationContext as MainApplication
        val pendingResult = goAsync()
        if (intent.action != null && intent.action == ACTION_NEXT_PREV && intent.hasExtra(EXTRA_NEXT_PREV)) {
            val offset = intent.getIntExtra(EXTRA_NEXT_PREV, 0)
            application.notificationState.offset += offset
            application.scheduleUpdate(application.notificationState.offsetResetTime)
        }
        CoroutineScope(SupervisorJob()).launch(EmptyCoroutineContext) {
            try{
                val rozvrhKey = Utils.getNotificationRozvrhKey(application)
                val account: Account? = rozvrhKey?.let { application.accountRepository.getAccount(it.account) }
                val isTeacher = account?.isTeacher() ?: false
                if (account != null && application.repository.refreshNeeded(rozvrhKey, false)){
                    //If the rozvrh needs to be refreshed, then the network call might take a long time
                    // and there would be a significant delay between user clicking "next week"
                    // in notification and any UI response.
                    // So we display the cached one immediately.
                    val cachedRozvrh = application.repository.getCachedRozvrh(rozvrhKey);
                    if (cachedRozvrh != null){
                        PermanentNotification.update(application, cachedRozvrh,isTeacher, account.id)
                    }
                }
                //todo move widget updating elsewhere
                if (account == null){
                    PermanentNotification.update(application, null, false,null, 0)
                }else {
                    val rozvrh: Rozvrh? = application.repository.getRozvrh(rozvrhKey, false)
                    PermanentNotification.update(application, rozvrh, isTeacher, account.id)
                }
                WidgetProvider.updateAll(application)
                application.updateUpdateTime()
            }finally {
                pendingResult.finish()
            }
        }

    }

    companion object {
        private val TAG = UpdateBroadcastReciever::class.java.simpleName
        const val REQUEST_CODE = 64857

        /**
         * +1 for next, -1 for prev
         */
        const val EXTRA_NEXT_PREV = BuildConfig.APPLICATION_ID + ".extra-next-or-prev-lesson"
        const val ACTION_NEXT_PREV = BuildConfig.APPLICATION_ID + ".action-next-or-prev-lesson"
    }
}