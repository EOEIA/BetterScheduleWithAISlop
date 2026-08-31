package cz.vitskalicky.lepsirozvrh.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.KotlinUtils.FLAG_IMMUTABLE
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.SharedPrefsKt
import cz.vitskalicky.lepsirozvrh.mainActivity.MainActivity
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption
import cz.vitskalicky.lepsirozvrh.model.rozvrh.labelRes
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object ChangeAlertNotification {

    const val CHANGES_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".changeAlertsChannel"
    private const val NOTIFICATION_ID_BASE = 7_100_000

    /**
     * Registers the change-alerts notification channel.
     * Safe to call repeatedly — Android ignores duplicate registrations.
     * Call from [cz.vitskalicky.lepsirozvrh.MainApplication.onCreate].
     */
    fun registerChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.change_notification_channel_name)
            val desc = context.getString(R.string.change_notification_channel_desc)
            val channel = NotificationChannel(CHANGES_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = desc
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /**
     * Posts a change-alert notification if the user enabled change alerts and the [diff] contains
     * relevant changes.  Duplicate alerts for the same timetable state are suppressed via a
     * per-(account, monday) fingerprint stored in SharedPreferences.
     *
     * @param captions  Timetable caption list for displaying lesson times in the notification body.
     */
    fun maybeNotify(
        context: Context,
        accountId: Long,
        monday: LocalDate,
        diff: TimetableDiff,
        captions: List<RozvrhCaption>,
        newFingerprint: Long
    ) {
        val prefs = SharedPrefsKt(context)

        if (prefs.boolean(PrefsConsts.CHANGE_ALERTS_ENABLED) != true) return

        val alertLessons = prefs.boolean(PrefsConsts.CHANGE_ALERT_LESSONS) != false
        val alertNoSchool = prefs.boolean(PrefsConsts.CHANGE_ALERT_NO_SCHOOL) != false

        val lessons = if (alertLessons) diff.changedLessons else emptyList()
        val events  = if (alertNoSchool) diff.noSchoolEvents  else emptyList()

        if (lessons.isEmpty() && events.isEmpty()) return

        // Suppress if we already alerted for this exact timetable state
        val fpKey = "change-alert-fp-$accountId-$monday"
        if (prefs.long(fpKey) == newFingerprint) return
        prefs.putOne(fpKey, newFingerprint)

        if (!PermanentNotification.areNotificationEnabled(context)) return

        val timeFormatter = DateTimeFormat.shortTime()
        val lines = mutableListOf<String>()
        for (ev in events) {
            lines.add(context.getString(R.string.change_alert_event_line, ev.event))
        }
        for (cl in lessons) {
            val caption = captions.getOrNull(cl.captionIndex)
            val timeStr = caption?.let {
                "${it.beginTime.toString(timeFormatter)}–${it.endTime.toString(timeFormatter)}"
            } ?: ""
            val subject = cl.lesson.subjectName.ifBlank { cl.lesson.subjectAbbrev }
            val kindStr = context.getString(cl.changeKind.labelRes())
            lines.add(if (timeStr.isNotBlank()) "$timeStr $subject ($kindStr)" else "$subject ($kindStr)")
        }

        val title = context.getString(R.string.change_notification_title)
        val summary = context.resources.getQuantityString(
            R.plurals.change_notification_summary, lines.size, lines.size
        )

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

        val notifId = NOTIFICATION_ID_BASE + ((accountId.hashCode() xor monday.toString().hashCode()) and 0x0F_FFFF)

        // Persist to change history before posting the notification
        ChangeHistory.addEntry(
            context,
            ChangeHistoryEntry(
                timestamp = System.currentTimeMillis(),
                monday = monday.toString(),
                lines = lines
            )
        )

        val notification = NotificationCompat.Builder(context, CHANGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}
