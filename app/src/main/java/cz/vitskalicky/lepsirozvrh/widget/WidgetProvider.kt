package cz.vitskalicky.lepsirozvrh.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.mainActivity.MainActivity
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.RozvrhRecord
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhBlock
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import cz.vitskalicky.lepsirozvrh.widget.WidgetsSettings.Widget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Takes care of updating widgets with data */
open class WidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "Updating widgets")
        val widgetsSettings = AppSingleton.getInstance(context).widgetsSettings
        var somethingAdded = false
        for (id in appWidgetIds) {
            if (widgetsSettings.widgetIds.add(id)) {
                somethingAdded = true
                val settings = Widget()
                settings.primaryTextColor = ContextCompat.getColor(context, R.color.widgetLightText)
                settings.secondaryTextColor = WidgetConfigActivity.calculateSecondaryTextColor(settings.primaryTextColor)
                settings.primaryTextSize = KotlinUtils.pxToSp(context.resources.getDimension(R.dimen.widgetTextPrimary), context)
                settings.secondaryTextSize = KotlinUtils.pxToSp(context.resources.getDimension(R.dimen.widgetTextSecondary), context)
                settings.backgroundColor = ContextCompat.getColor(context, R.color.widgetLightBackground)
                widgetsSettings.widgets[id] = settings
            }
        }
        if (somethingAdded) {
            AppSingleton.getInstance(context).saveWidgetsSettings()
        }
        // this will update data
        //todo let livedata handle the updates
        val updateIntent = Intent(context, UpdateBroadcastReciever::class.java)
        context.sendBroadcast(updateIntent)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val widgetsSettings = AppSingleton.getInstance(context).widgetsSettings
        for (id in appWidgetIds) {
            widgetsSettings.widgets.remove(id)
            widgetsSettings.widgetIds.remove(id)
        }
        AppSingleton.getInstance(context).saveWidgetsSettings()
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        Log.d(TAG, "Updating widget options")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val widgetsSettings = AppSingleton.getInstance(context).widgetsSettings
        if (widgetsSettings.widgetIds.add(appWidgetId)) {
            val settings = Widget()
            settings.primaryTextColor = ContextCompat.getColor(context, R.color.widgetLightText)
            settings.secondaryTextColor = WidgetConfigActivity.calculateSecondaryTextColor(settings.primaryTextColor)
            settings.primaryTextSize = KotlinUtils.pxToSp(context.resources.getDimension(R.dimen.widgetTextPrimary), context)
            settings.secondaryTextSize = KotlinUtils.pxToSp(context.resources.getDimension(R.dimen.widgetTextSecondary), context)
            settings.backgroundColor = ContextCompat.getColor(context, R.color.widgetLightBackground)
            widgetsSettings.widgets[appWidgetId] = settings
            AppSingleton.getInstance(context).saveWidgetsSettings()
        }
        val updateIntent = Intent(context, UpdateBroadcastReciever::class.java)
        context.sendBroadcast(updateIntent)
    }

    companion object {
        val TAG = WidgetProvider::class.java.simpleName
        const val PENDING_INTENT_REQUEST_CODE = 802000
        const val WIDGET_LENGTH = 5
        suspend fun updateAll(app: MainApplication){
            val widgetsSettings = AppSingleton.getInstance(app).widgetsSettings
            val scope = CoroutineScope(coroutineContext) //parallelize requests
            val jobs = ArrayList<Job>()
            for (item in widgetsSettings.widgets){
                jobs.add(scope.launch {
                    val account = app.accountRepository.getAccount(item.value.accountId) ?: run {updateLoggedOut(app, item.key); null} ?: return@launch
                    val rozvrh = app.repository.getRozvrh(RozvrhRecord.Key(account.id, Utils.getCurrentMonday()), false)
                    val display = rozvrh?.getWidgetDisplayBlocks(WIDGET_LENGTH)
                    update(app, item.key, display?.first, account.isTeacher(), display?.second)
                })
            }
            for (item in jobs) {
                item.join()
            }
        }
        fun updateAllForAccount(account: Account, rozvrh: Rozvrh?, context: Context) {
            val widgetsSettings = AppSingleton.getInstance(context).widgetsSettings
            val display: Pair<List<RozvrhBlock>?,String?>? = rozvrh?.getWidgetDisplayBlocks(WIDGET_LENGTH)
            val widgetIds = widgetsSettings.widgetIds
            for (id in widgetIds) {
                val settings: Widget = widgetsSettings.widgets[id] ?: Widget() //fail-safe
                if (settings.accountId == account.id) {
                    update(context, id, display?.first, account.isTeacher(), display?.second)
                }
            }
        }

        fun updateAccountLoggedOut(context: Context, accountId: Long){
            val widgetsSettings = AppSingleton.getInstance(context).widgetsSettings
            val widgetIds = widgetsSettings.widgetIds
            for (id in widgetIds) {
                val settings: Widget = widgetsSettings.widgets[id] ?: Widget() //fail-safe
                if (settings.accountId == accountId) {
                    updateLoggedOut(context, id)
                }
            }
        }

        /** The account for the widget has logged out - show a message and configure another account on tap */
        fun updateLoggedOut(context: Context, widgetID: Int){

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetsSettings = AppSingleton.getInstance(context).widgetsSettings;
            var widgetSettings:Widget = widgetsSettings.widgets[widgetID] ?: /*fail-safe*/run {
                // reports error and fails safe
                val mapper = ObjectMapper()
                Log.e(TAG, "There are widget settings missing for widget with id $widgetID")
                (context.applicationContext as MainApplication).sendReport(RuntimeException("There are widget settings missing for widget with id $widgetID. Widget settings: ${mapper.writeValueAsString(widgetsSettings)}"))
                Widget()
            }
            val views: RemoteViews = RemoteViews(context.packageName, R.layout.small_widget)
            views.setTextViewText(R.id.textViewZkrpr, "")
            views.setViewVisibility(R.id.textViewZkrpr, View.GONE)
            views.setTextViewText(R.id.textViewSecondary, context.getString(R.string.widget_logged_out))
            views.setInt(R.id.textViewZkrpr, "setTextColor", widgetSettings.primaryTextColor)
            views.setInt(R.id.textViewSecondary, "setTextColor", widgetSettings.secondaryTextColor)
            views.setFloat(R.id.textViewZkrpr, "setTextSize", widgetSettings.primaryTextSize)
            views.setFloat(R.id.textViewSecondary, "setTextSize", widgetSettings.secondaryTextSize)
            views.setInt(R.id.bgcolor, "setImageAlpha", widgetSettings.backgroundColor and -0x1000000 shr 24)
            views.setInt(R.id.bgcolor, "setColorFilter", widgetSettings.backgroundColor or -0x1000000)

            val intent = Intent(context, WidgetChangeAccountActivity::class.java)
            intent.putExtra(WidgetChangeAccountActivity.EXTRA_WIDGET_ID, widgetID)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent = PendingIntent.getActivity(context, PENDING_INTENT_REQUEST_CODE + widgetID /*must be unique for each widget*/, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.root, pendingIntent)
            appWidgetManager.updateAppWidget(widgetID, views)

        }

        /**
         * Updates the widget with the given id. If there is an event today, leave [hodiny] `null` and put the event name into [event]. Otherwise leave [event] `null`.
         */
        fun update(context: Context, widgetID: Int, hodiny: List<RozvrhBlock>?,isTeacher: Boolean, event: String? = null ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            var widgetSettings = AppSingleton.getInstance(context).widgetsSettings.widgets[widgetID]

            // failsafe
            if (widgetSettings == null) {
                widgetSettings = Widget()
                //todo report
                Log.e(TAG, "There are widget settings missing for widget with id $widgetID")
            }
            val allEmpty:Boolean = hodiny.isNullOrEmpty()

            val options = appWidgetManager.getAppWidgetOptions(widgetID)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val views: RemoteViews
            if (width < 250 || allEmpty || !event.isNullOrBlank()) {
                views = RemoteViews(context.packageName, R.layout.small_widget)
                val hodina: RozvrhLesson? = hodiny?.firstOrNull()?.lessons?.firstOrNull()
                updateCell(views, R.id.textViewZkrpr, R.id.textViewSecondary, hodina,isTeacher, event, widgetSettings, false, context)
            } else {
                views = RemoteViews(context.packageName, R.layout.wide_widget)

                val lesson: List<RozvrhLesson?> = hodiny!!.map { it.lessons.firstOrNull() }

                updateCell(views, R.id.textViewZkrpr0, R.id.textViewSecondary0, lesson.getOrNull(0), isTeacher, null,widgetSettings, true, context)
                updateCell(views, R.id.textViewZkrpr1, R.id.textViewSecondary1, lesson.getOrNull(1), isTeacher, null,widgetSettings, true, context)
                updateCell(views, R.id.textViewZkrpr2, R.id.textViewSecondary2, lesson.getOrNull(2), isTeacher, null,widgetSettings, true, context)
                updateCell(views, R.id.textViewZkrpr3, R.id.textViewSecondary3, lesson.getOrNull(3), isTeacher, null,widgetSettings, true, context)
                updateCell(views, R.id.textViewZkrpr4, R.id.textViewSecondary4, lesson.getOrNull(4), isTeacher, null,widgetSettings, true, context)
                views.setInt(R.id.imageViewDivider, "setImageAlpha", 255)
                views.setInt(R.id.imageViewDivider, "setColorFilter", widgetSettings.primaryTextColor)
            }
            views.setInt(R.id.bgcolor, "setImageAlpha", widgetSettings.backgroundColor and -0x1000000 shr 24)
            views.setInt(R.id.bgcolor, "setColorFilter", widgetSettings.backgroundColor or -0x1000000)

            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_JUMP_TO_TODAY, true)
            intent.putExtra(MainActivity.EXTRA_SWITCH_TO_ACCOUNT, widgetSettings.accountId)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(context, PENDING_INTENT_REQUEST_CODE + widgetID /*must be unique for each widget*/, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetID, views)
        }

        private fun updateCell(views: RemoteViews, primaryTextId: Int, secondaryTextId: Int, hodina: RozvrhLesson?, isTeacher: Boolean, event: String?, settings: Widget, allowEmpty: Boolean, context: Context) {
            if (!event.isNullOrBlank()){
                views.setTextViewText(primaryTextId, "")
                views.setViewVisibility(primaryTextId, View.GONE)
                views.setTextViewText(secondaryTextId, event)

                views.setInt(primaryTextId, "setTextColor", settings.primaryTextColor)
                views.setInt(secondaryTextId, "setTextColor", settings.primaryTextColor) //on event the secondary text has color of primary text
                views.setFloat(primaryTextId, "setTextSize", settings.primaryTextSize)
                views.setFloat(secondaryTextId, "setTextSize", settings.secondaryTextSize)
            }else {
                if (hodina == null) {
                    views.setTextViewText(primaryTextId, "")
                    views.setViewVisibility(primaryTextId, View.GONE)
                    if (allowEmpty) {
                        views.setTextViewText(secondaryTextId, "")
                    } else {
                        views.setTextViewText(secondaryTextId, context.getString(R.string.nothing))
                    }
                } else {
                    var tchr = hodina.teacherAbbrev
                    if (isTeacher) {
                        // to teacher's we want to show the class, not the teacher
                        // the class name is saved in zkrskup and skup
                        tchr = hodina.groups.joinToString(", ") { it.abbrev.ifBlank { it.name } }
                    }
                    if (hodina.subjectAbbrev.isBlank() && hodina.teacherAbbrev.isBlank() && hodina.changeType != RozvrhLesson.NO_CHANGE) {
                        views.setTextViewText(primaryTextId, "")
                        views.setViewVisibility(primaryTextId, View.GONE)
                        views.setTextViewText(secondaryTextId, context.getString(R.string.lesson_cancelled))
                    } else {
                        views.setTextViewText(primaryTextId, hodina.subjectAbbrev)
                        views.setViewVisibility(primaryTextId, View.VISIBLE)
                        views.setTextViewText(secondaryTextId, buildSpannedString {
                            append(tchr)
                            append(" ")
                            bold { append(hodina.roomAbbrev) }
                        })
                    }
                }
                views.setInt(primaryTextId, "setTextColor", settings.primaryTextColor)
                views.setInt(secondaryTextId, "setTextColor", settings.secondaryTextColor)
                views.setFloat(primaryTextId, "setTextSize", settings.primaryTextSize)
                views.setFloat(secondaryTextId, "setTextSize", settings.secondaryTextSize)
            }
        }
    }
}