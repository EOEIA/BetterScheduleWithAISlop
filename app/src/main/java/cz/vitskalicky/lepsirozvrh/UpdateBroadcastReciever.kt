package cz.vitskalicky.lepsirozvrh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.notification.GradeNotification
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
                    // Grades/homework have no periodic check of their own (unlike the schedule
                    // fetch above, which already triggers change alerts) - piggyback on every
                    // wakeup of this receiver so they don't depend on the user opening those screens.
                    GradeNotification.checkAndNotifyGrades(application, account.id)
                    GradeNotification.checkAndNotifyHomework(application, account.id)
                }
                WidgetProvider.updateAll(application)
                application.updateUpdateTime()
                // Keep the periodic check alive regardless of what triggered this particular wakeup.
                application.schedulePeriodicCheck()
            }finally {
                pendingResult.finish()
            }
        }

    }

    companion object {
        private val TAG = UpdateBroadcastReciever::class.java.simpleName
        const val REQUEST_CODE = 64857
        const val PERIODIC_REQUEST_CODE = 64858

        /**
         * +1 for next, -1 for prev
         */
        const val EXTRA_NEXT_PREV = BuildConfig.APPLICATION_ID + ".extra-next-or-prev-lesson"
        const val ACTION_NEXT_PREV = BuildConfig.APPLICATION_ID + ".action-next-or-prev-lesson"
        /** Fired by the self-rescheduling periodic alarm set up in [MainApplication.schedulePeriodicCheck]. */
        const val ACTION_PERIODIC_CHECK = BuildConfig.APPLICATION_ID + ".action-periodic-check"
    }
}