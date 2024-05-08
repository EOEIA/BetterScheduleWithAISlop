package cz.vitskalicky.lepsirozvrh.notification

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.KotlinUtils.FLAG_IMMUTABLE
import cz.vitskalicky.lepsirozvrh.mainActivity.MainActivity
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhBlock
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import org.joda.time.format.DateTimeFormat

/** Logic and stuff for the persistent notification */
object PermanentNotification {
    const val PERMANENT_NOTIFICATION_ID = 7055713
    const val PERMANENT_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".permanentNotificationChannel"
    const val PREF_DONT_SHOW_INFO_DIALOG = "dont-show-notification-info-dialog-again"
    public val EXTRA_NOTIFICATION = PermanentNotification::class.java.canonicalName + "-extra-notification"

    /** Special value for accountId which symbolizes that permanent notification have been disabled by the user. */
    public const val ACCOUNT_NOTIFICATION_DISABLED = -1L;
    /** Special value for the accountId which symbolizes that permanent notification was enabled, but that account has been logged out. */
    public const val ACCOUNT_NOTIFICATION_LOGGED_OUT = -2L;

    /** Does not guarantee the account is actually valid, just checks for special values (it still might be set to account which has been removed and it was not updated to [ACCOUNT_NOTIFICATION_LOGGED_OUT] by accident) */
    public fun isNotificationAccountValid(id: Long): Boolean{
        return id != ACCOUNT_NOTIFICATION_LOGGED_OUT && id != ACCOUNT_NOTIFICATION_DISABLED;
    }

    suspend fun update(app: MainApplication) {
        val accountId: Long? = if (SharedPrefs.contains(app, PrefsConsts.NOTIFICATION_ACCOUNT))
            SharedPrefs.getLong(app, PrefsConsts.NOTIFICATION_ACCOUNT).takeIf { isNotificationAccountValid(it) }
        else null;
        val account = accountId?.let{ app.accountRepository.getAccount(it) }
        if (account == null){
            update(app, null as Rozvrh?, false, null)
            return
        }
        val key = RozvrhRecord.Key(accountId, Utils.getCurrentMonday());
        val rozvrh = app.repository.getRozvrh(key, false)
        update(app, rozvrh, account.isTeacher(), accountId)
    }

    /**
     * Same as [update], but gets the RozvrhHodina for you.
     */
    fun update(application: MainApplication, rozvrh: Rozvrh?, isTeacher: Boolean, accountId: Long?) {
        val context: Context = application
        if (rozvrh != null) {
            val blockIndexes = rozvrh.getHighlightBlockIndexes(true)

            val offset = application.notificationState.offset
            if (blockIndexes == null) {
                update(context, null, isTeacher, accountId)
            } else {
                val offsetBlock: RozvrhBlock? = rozvrh.getAsRozvrhBlock(blockIndexes.first, blockIndexes.second + offset)
                update(context, offsetBlock, isTeacher, accountId, offset)
            }
        } else {
            update(context, null, isTeacher, accountId)
        }
    }

    /**
     * Updates the notification with the data of the first lesson of supplied [BlockRelated]. If there are no lesson "no lesson" text in notification is showed. If [block] is `null` and offset is 0, the notification is hidden.
     */
    fun update(context: Context, block: RozvrhBlock?, isTeacher: Boolean, accountId: Long?, offset: Int = 0) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (block == null && offset == 0) {
            notificationManager.cancel(PERMANENT_NOTIFICATION_ID)
            return
        }
        var offsetText = ""
        var predmet: String = ""
        var mistnost: String = ""
        var ucitel: String = ""
        var skupina: String = ""
        var cas = ""

        val lesson = block?.lessons?.firstOrNull();
        if (block == null || lesson == null) {
            predmet = context.getString(R.string.nothing)
        } else {
            predmet = lesson.subjectName.ifBlank { lesson.subjectAbbrev }

            if (predmet.isBlank()) {
                if (lesson.changeType != RozvrhLesson.NO_CHANGE) {
                    predmet = context.getString(R.string.lesson_cancelled)
                } else {
                    predmet = context.getString(R.string.nothing)
                }
            }
            mistnost = lesson.roomName.ifBlank { lesson.roomAbbrev }
            ucitel = lesson.teacherName.ifBlank { lesson.teacherAbbrev }
            skupina = lesson.groups.joinToString(", ") { it.name.ifBlank { it.abbrev } }
            if (isTeacher) {
                // in teacher's schedule the class name is saved in skup and zkrskup
                // and we want to display it in the place where the teacher's name would usually be.
                ucitel = skupina
                skupina = ""
            }
            if (skupina.isNotBlank()) {
                skupina = context.getString(R.string.group_in_notification, skupina)
            }
            val beginTime = block.caption.beginTime.toString(DateTimeFormat.shortTime())
            val endTime = block.caption.endTime.toString(DateTimeFormat.shortTime())
            if (beginTime.isNotBlank() && endTime.isNotBlank()) {
                cas = "$beginTime - $endTime"
            }
        }
        if (offset != 0) {
            offsetText = "$offset: "
            if (offset > 0) {
                offsetText = "+$offsetText"
            }
        }
        var title: CharSequence = ""
        title = if (!predmet.isBlank() && !mistnost.isBlank()) {
            buildSpannedString {
                append("$offsetText$predmet ${context.getString(R.string.`in`)} ")
                bold { append(mistnost) }
            }
        } else {
            buildSpannedString {
                append("$offsetText$predmet")
                bold { append(mistnost) }
            }
        }
        /*if (!offsetText.isEmpty()){
            title = offsetText + title;
        }*/
        var content: CharSequence? = ""
        var contentString = ""
        contentString = if (!ucitel!!.isEmpty() && !skupina!!.isEmpty()) {
            "$ucitel, $skupina"
        } else {
            ucitel + skupina
        }
        contentString = if (!contentString.isEmpty() && !cas.isEmpty()) {
            "$contentString, $cas"
        } else {
            contentString + cas
        }
        content = contentString
        var expanded: CharSequence = content
        if (!mistnost!!.isEmpty()) {
            expanded = expanded.toString() + ", " + context.getString(R.string.room) + " " + mistnost
        }
        val nextIntent = Intent(context, UpdateBroadcastReciever::class.java)
        nextIntent.action = UpdateBroadcastReciever.ACTION_NEXT_PREV
        nextIntent.putExtra(UpdateBroadcastReciever.EXTRA_NEXT_PREV, 1)
        val nextPendingIntent = PendingIntent.getBroadcast(context, 458631, nextIntent, FLAG_IMMUTABLE)
        val prevIntent = Intent(context, UpdateBroadcastReciever::class.java)
        prevIntent.action = UpdateBroadcastReciever.ACTION_NEXT_PREV
        prevIntent.putExtra(UpdateBroadcastReciever.EXTRA_NEXT_PREV, -1)
        val prevPendingIntent = PendingIntent.getBroadcast(context, 4586, prevIntent, FLAG_IMMUTABLE)
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_JUMP_TO_TODAY, true)
        intent.putExtra(EXTRA_NOTIFICATION, true)
        intent.putExtra(MainActivity.EXTRA_SWITCH_TO_ACCOUNT, accountId)
        //todo is this the correct way to do it?
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addNextIntentWithParentStack(intent)
        val pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

        //create notification
        val builder = NotificationCompat.Builder(context, PERMANENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setWhen(Long.MAX_VALUE)
                .setShowWhen(false)
                .setSound(Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.silence))
                .setVibrate(longArrayOf())
                .setOnlyAlertOnce(true)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(expanded))
                .addAction(R.drawable.ic_navigate_before_black_24dp, context.getString(R.string.prev_lesson), prevPendingIntent)
                .addAction(R.drawable.ic_navigate_next_black_24dp, context.getString(R.string.next_lesson), nextPendingIntent)
        val ntf = builder.build()

        if (!areNotificationEnabled(context)) {
            SharedPrefsKt(context).edit {
                putLong(PrefsConsts.NOTIFICATION_ACCOUNT, ACCOUNT_NOTIFICATION_DISABLED)
                putBoolean(PrefsConsts.NOTIFICATION_PLEASE_GRANT_PERMISSION, true)
            }
            notificationManager.cancel(PERMANENT_NOTIFICATION_ID)
        }else{
            notificationManager.notify(PERMANENT_NOTIFICATION_ID, ntf)
        }
    }

    @Composable
    fun ShowNoPermissionDialog(onDismissed: () -> Unit){
        AlertDialog(
            onDismissed,
            { Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.End, ) {
                TextButton(onDismissed){
                    Text(stringResource(R.string.ok))
                }
            } },
            title = { Text(stringResource(R.string.notification_no_permission_title)) },
            text = { Text(stringResource(R.string.notification_no_permission)) }
        )
    }

    fun isApiLevelBeforeNotiPerm() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    fun shouldShowNotificationRationale(activity: Activity): Boolean{
        return !isApiLevelBeforeNotiPerm()
                && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
    }
    /** Returns true if you need to ask for notification permission (it is not granted) */
    fun areNotificationEnabled(ctx: Context): Boolean{
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
                && (
                    isApiLevelBeforeNotiPerm()
                    || ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                );
    }

    @Composable
    fun areNotificationEnabled(): Boolean = areNotificationEnabled(LocalContext.current)
}