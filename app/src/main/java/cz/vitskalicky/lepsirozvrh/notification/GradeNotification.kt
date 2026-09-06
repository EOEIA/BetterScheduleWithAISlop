package cz.vitskalicky.lepsirozvrh.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import cz.vitskalicky.lepsirozvrh.KotlinUtils.FLAG_IMMUTABLE
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.SharedPrefsKt
import cz.vitskalicky.lepsirozvrh.Utils
import cz.vitskalicky.lepsirozvrh.bakaAPI.marks.MarkSubject
import cz.vitskalicky.lepsirozvrh.grades.GradePredictor
import cz.vitskalicky.lepsirozvrh.grades.PredictionState
import cz.vitskalicky.lepsirozvrh.grades.homework.HomeworkItem
import cz.vitskalicky.lepsirozvrh.grades.homework.extractHomework
import cz.vitskalicky.lepsirozvrh.grades.homework.fetchHomeworkDescriptions
import cz.vitskalicky.lepsirozvrh.mainActivity.MainActivity
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.prefs

object GradeNotification {

    private const val NOTIF_ID_GRADES = 7_200_001
    private const val NOTIF_ID_HOMEWORK = 7_200_002
    private const val SEEN_GRADE_IDS_KEY = "grade-notification-seen-ids"
    private const val SEEN_HOMEWORK_IDS_KEY = "homework-notification-seen-ids"

    fun maybeNotifyNewGrades(context: Context, subjects: List<MarkSubject>) {
        try {
            val prefs = SharedPrefsKt(context)
            if (prefs.boolean(PrefsConsts.GRADE_ALERTS_ENABLED) != true) return
            if (!PermanentNotification.areNotificationEnabled(context)) return

            val allMarks = subjects.flatMap { it.Marks }
            val seenIds = prefs.string(SEEN_GRADE_IDS_KEY)
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            val newMarks = allMarks.filter { it.Id.isNotBlank() && it.Id !in seenIds }

            val allIds = (seenIds + allMarks.map { it.Id }).filter { it.isNotBlank() }
            prefs.putOne(SEEN_GRADE_IDS_KEY, allIds.joinToString(","))

            if (newMarks.isNotEmpty()) {
                val lines = newMarks.map { mark ->
                    val subject = subjects.firstOrNull { s -> s.Marks.any { it.Id == mark.Id } }
                    val subjectAbbrev = subject?.Subject?.Abbrev ?: ""
                    val predText = buildPredictionText(context, subject)
                    "$subjectAbbrev: ${mark.MarkText} (×${mark.Weight})${if (predText.isNotEmpty()) " — $predText" else ""}"
                }
                val title = context.getString(R.string.grade_notification_title)
                val summary = context.resources.getQuantityString(R.plurals.grade_notification_summary, newMarks.size, newMarks.size)
                postNotification(context, NOTIF_ID_GRADES, title, summary, lines)
            }
        } catch (_: Exception) { }
    }

    fun maybeNotifyNewHomework(context: Context, items: List<HomeworkItem>) {
        try {
            val prefs = SharedPrefsKt(context)
            if (prefs.boolean(PrefsConsts.HOMEWORK_ALERTS_ENABLED) != true) return
            if (!PermanentNotification.areNotificationEnabled(context)) return

            val toKey: (HomeworkItem) -> String = { "${it.description.orEmpty().take(40)}|${it.date}" }
            val seenIds = prefs.string(SEEN_HOMEWORK_IDS_KEY)
                ?.split("||")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            val newItems = items.filter { toKey(it) !in seenIds }

            val allKeys = (seenIds + items.map { toKey(it) }).filter { it.isNotBlank() }
            prefs.putOne(SEEN_HOMEWORK_IDS_KEY, allKeys.joinToString("||"))

            if (newItems.isNotEmpty()) {
                val lines = newItems.map { "${it.subjectAbbrev}: ${it.description.orEmpty().take(60)}" }
                val title = context.getString(R.string.homework_notification_title)
                val summary = context.resources.getQuantityString(R.plurals.homework_notification_summary, newItems.size, newItems.size)
                postNotification(context, NOTIF_ID_HOMEWORK, title, summary, lines)
            }
        } catch (_: Exception) { }
    }

    /**
     * Fetches grades/homework for [accountId] and posts a notification for anything new. Used by
     * the periodic background check in [cz.vitskalicky.lepsirozvrh.UpdateBroadcastReciever] so
     * these alerts don't depend on the user having the Grades/Homework screen open.
     */
    suspend fun checkAndNotifyGrades(application: MainApplication, accountId: Long) {
        if (application.prefs.boolean(PrefsConsts.GRADE_ALERTS_ENABLED) != true) return
        try {
            val account = application.accountRepository.getAccount(accountId) ?: return
            val webservice = application.accountRepository.getMarksWebservice(account) ?: return
            maybeNotifyNewGrades(application, webservice.getMarks().Subjects)
        } catch (_: Exception) { }
    }

    suspend fun checkAndNotifyHomework(application: MainApplication, accountId: Long) {
        if (application.prefs.boolean(PrefsConsts.HOMEWORK_ALERTS_ENABLED) != true) return
        try {
            val monday = Utils.getDisplayWeekMonday(application)
            val rozvrh = application.repository.getRozvrh(RozvrhRecord.Key(accountId, monday), false) ?: return
            val descriptionsById = fetchHomeworkDescriptions(application, accountId)
            maybeNotifyNewHomework(application, rozvrh.extractHomework(descriptionsById))
        } catch (_: Exception) { }
    }

    data class DiagnosticInfo(
        val gradeIdCount: Int,
        val gradeIdPreview: List<String>,
        val homeworkKeyCount: Int
    )

    fun getDiagnosticInfo(context: Context): DiagnosticInfo {
        val prefs = SharedPrefsKt(context)
        val gradeRaw = prefs.string(SEEN_GRADE_IDS_KEY) ?: ""
        val gradeIds = gradeRaw.split(",").filter { it.isNotBlank() }
        val homeworkRaw = prefs.string(SEEN_HOMEWORK_IDS_KEY) ?: ""
        val homeworkKeys = homeworkRaw.split("||").filter { it.isNotBlank() }
        return DiagnosticInfo(gradeIds.size, gradeIds.takeLast(5), homeworkKeys.size)
    }

    private fun buildPredictionText(context: Context, subject: MarkSubject?): String {
        if (subject == null) return ""
        val prediction = GradePredictor.predict(subject.Marks, PredictionState())
        val improve = prediction.realImproveSuggestions.firstOrNull() ?: return ""
        val grade1 = prediction.realGrade1Suggestions.firstOrNull()

        val improveText = context.getString(R.string.grade_notif_improve,
            improve.targetRoundedGrade, "%.1f".format(improve.neededGrade), improve.addedWeight)
        val grade1Text = grade1?.let {
            context.getString(R.string.grade_notif_grade1, "%.1f".format(it.neededGrade), it.addedWeight)
        } ?: ""

        return if (grade1Text.isNotEmpty()) "$improveText; $grade1Text" else improveText
    }

    private fun postNotification(context: Context, notifId: Int, title: String, summary: String, lines: List<String>) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(notifId, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, ChangeAlertNotification.CHANGES_CHANNEL_ID)
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
