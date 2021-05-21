package cz.vitskalicky.lepsirozvrh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.vitskalicky.lepsirozvrh.model.relations.RozvrhRelated
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.widget.WidgetProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that updates notification and widgets when receives a broadcast.
 */
class UpdateBroadcastReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast received")
        val application = context.applicationContext as MainApplication
        val pendingResult = goAsync()
        if (intent.action != null && intent.action == ACTION_NEXT_PREV && intent.hasExtra(EXTRA_NEXT_PREV)) {
            val offset = intent.getIntExtra(EXTRA_NEXT_PREV, 0)
            application.notificationState.offset += offset
            application.scheduleUpdate(application.notificationState.offsetResetTime)
        }
        GlobalScope.launch {
            if (application.repository.refreshNeeded(Utils.getCurrentMonday(), false)){
                //If the rozvrh needs to be refresh, then the network call might take a long time
                // and there would be a significant delay between user clicking "next week"
                // in notification and any UI response.
                // So we display the cached one immediately.
                val cachedRozvrh = application.repository.getCachedRozvrh(Utils.getCurrentMonday());
                if (cachedRozvrh != null){
                    PermanentNotification.update(cachedRozvrh,application)
                }
            }
            val rozvrh: RozvrhRelated? = application.repository.getRozvrh(Utils.getCurrentMonday(), false)
            PermanentNotification.update(rozvrh,application)
            WidgetProvider.updateAll(rozvrh, context)
            application.updateUpdateTime()
            pendingResult.finish()
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