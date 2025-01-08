package cz.vitskalicky.lepsirozvrh.migration

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.Class
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.theme.DefaultRozvrhThemes
import cz.vitskalicky.lepsirozvrh.theme.SelectedTheme
import cz.vitskalicky.lepsirozvrh.widget.WidgetsSettings
import cz.vitskalicky.lepsirozvrh.widget.WidgetsSettings.Widget
import io.sentry.Sentry
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/** Handles migration from pre- 1.9 to 1.9 */
object v1_9 : MigrationInterface {
    const val TAG: String = "v1_9";
    override val versionCode: Int
        get() = 40;
    override suspend fun migrate(context: Context){
        account(context)
        switchToNextWeek(context)
        theme(context)
    }

    /**
     * Migrates account into the database. Also migrates persistent notification setting and widgets settings, since they
     * depend on account id. suspends for writing to disk, but no slow network things.
     */
    private suspend fun account(context: Context){
        val prefs = context.prefs;
        // extract required data from old storage
        val oldData = mapOf(
            "serverUrl" to prefs.string(URL),
            "usernameLength" to prefs.string(USERNAME)?.length,
            "accessTokenLength" to prefs.string(ACCESS_TOKEN)?.length,
            "refreshTokenLength" to prefs.string(REFRESH_TOKEN)?.length,
            "accessExpires" to prefs.string(ACCESS_EXPIRES)
            )
        val acc: Account? = with(prefs) {
            try {
                Account(
                    serverUrl = string(URL)!!,
                    username = string(USERNAME)?:"",
                    accessToken = string(ACCESS_TOKEN)!!,
                    refreshToken = string(REFRESH_TOKEN)!!,
                    accessExpires = DateTime.parse(string(ACCESS_EXPIRES)),
                    schoolName = "",
                    fullName = "",
                    userType = "",
                    userTypeText = "",
                    semesterEnd = null,
                    userUID = "",
                    clazz = Class(id = "", abbrev = "", name = ""),
                    requireRefresh = true
                )
            }catch (e: NullPointerException){
                if (!string(ACCESS_TOKEN).isNullOrBlank()){
                    //todo check what if the user has logged in and out
                    Sentry.captureException(RuntimeException("Exception while migrating account: Could not get info about old account, but there seems to be some account present. Old data from shared preferences: $oldData", e));
                }
                null
            }
        }
        // delete old tokens, name, etc.
        prefs.edit {
            remove(URL)
            remove(SCHOOL_NAME)
            remove(SCHOOL_ID)
            remove(USERNAME)
            remove(ACCESS_TOKEN)
            remove(REFRESH_TOKEN)
            remove(ACCESS_EXPIRES)
            remove(NAME)
            remove(TYPE)
            remove(TYPE_TEXT)
            remove(SEMESTER_END)
        }
        var accountvar: Account? = null;
        if (acc != null) {
            // if there was an account present

            // add account to database
            val app = context.applicationContext as MainApplication;
            val id = app.rozvrhDb.accountDao().insertAccount(acc);
            accountvar = app.rozvrhDb.accountDao().loadAccount(id);
            if (accountvar == null) {
                Sentry.captureException(IllegalStateException("Account not found after migration. User has been logged out. Old data from shared preferences: $oldData"))
                //something went terribly wrong
            }else {
                app.accountRepository.switchToAccount(accountvar.id)
            }
        }

        val account: Account? = accountvar;
        // update persistent notification setting
        prefs.edit {
            if (account != null && (prefs.boolean(PREFS_NOTIFICATION) != false)) { // logged in and notification enabled
                putLong(PrefsConsts.NOTIFICATION_ACCOUNT, account.id)
            }else if (prefs.boolean(PREFS_NOTIFICATION) == false){ // explicitly disabled
                putLong(PrefsConsts.NOTIFICATION_ACCOUNT, PermanentNotification.ACCOUNT_NOTIFICATION_DISABLED)
            }else{ // not logged in and enabled
                putLong(PrefsConsts.NOTIFICATION_ACCOUNT, PermanentNotification.ACCOUNT_NOTIFICATION_LOGGED_OUT)
            }
            remove(PREFS_NOTIFICATION)
        }
        // update widgets
        if (!prefs.string(WIDGETS_SETTINGS).isNullOrBlank()) {
            try {
                val oldWidgetSettings: OldWidgetSettings = Json.decodeFromString(prefs.string(WIDGETS_SETTINGS)!!)
                val oldWidgets = oldWidgetSettings.widgets
                val newWidgets = oldWidgets.mapValues {
                    Widget().apply {
                        this.accountId = account?.id ?: 0;
                        this.backgroundColor = it.value.backgroundColor
                        this.primaryTextColor = it.value.primaryTextColor
                        this.primaryTextSize = it.value.primaryTextSize
                        this.secondaryTextColor = it.value.secondaryTextColor
                        this.secondaryTextSize = it.value.secondaryTextSize
                    }
                }
                val newWidgetSettings = WidgetsSettings().apply {
                    widgetIds = oldWidgetSettings.widgetIds;
                    widgets = HashMap(newWidgets);
                }
                val json = ObjectMapper().writeValueAsString(newWidgetSettings);
                prefs.edit { putString(PrefsConsts.WIDGETS_SETTINGS, json) }
            } catch (e: IllegalArgumentException){
                Sentry.captureException(RuntimeException("Exception while converting widget settings", e))
            } catch (e: SerializationException){
                Sentry.captureException(RuntimeException("Exception while converting widget settings", e))
            }
            prefs.edit { remove(WIDGETS_SETTINGS) }
        }
    }

    /** migrates the switch to next week setting */
    private fun switchToNextWeek(context: Context){
        val prefs = context.prefs
        val oldValue: String? = prefs.string(PREFS_WEEK_SWITCH);
        val parsed: Int? = oldValue?.let { try {it.toInt() } catch (_:NumberFormatException) { null } }
        prefs.edit {
            if (parsed != null){
                putInt(PrefsConsts.SWITCH_TO_NEXT_WEEK_OPTION_INDEX, parsed)
            }
            remove(PREFS_WEEK_SWITCH)
        }
    }

    private fun theme(context: Context){
        val sp = context.prefs;

        // Migrate general theme preference
        val oldThemePref = sp.string("prefs-app-theme")?.toIntOrNull() ?: SelectedTheme.FOLLOW_SYSTEM_THEME.index;
        sp.edit {
            putInt(PrefsConsts.SELECTED_THEME, oldThemePref)
        }

        // todo load primary, accent adn background colors from Cyanea

        // Load old custom theme settings and save into new ones
        var theme = DefaultRozvrhThemes.LIGHT
        theme = theme.copy(
            cEmptyBg = sp.int("PREFS-THEME-cEmptyBg")?.let { Color(it) } ?: theme.cEmptyBg,
            cABg = sp.int("PREFS-THEME-cABg")?.let { Color(it) } ?: theme.cABg,
            cHBg = sp.int("PREFS-THEME-cHBg")?.let { Color(it) } ?: theme.cHBg,
            cChngBg = sp.int("PREFS-THEME-cChngBg")?.let { Color(it) } ?: theme.cChngBg,
            cHeaderBg = sp.int("PREFS-THEME-cHeaderBg")?.let { Color(it) } ?: theme.cHeaderBg,
            cDivider = sp.int("PREFS-THEME-cDivider")?.let { Color(it) } ?: theme.cDivider,
            dpDividerWidth = sp.float("PREFS-THEME-dpDividerWidth") ?: theme.dpDividerWidth,
            cHighlight = sp.int("PREFS-THEME-cHighlight")?.let { Color(it) } ?: theme.cHighlight,
            dpHighlightWidth = sp.float("PREFS-THEME-dpHighlightWidth") ?: theme.dpHighlightWidth,
            cHPrimaryText = sp.int("PREFS-THEME-cHPrimaryText")?.let { Color(it) } ?: theme.cHPrimaryText,
            cHRoomText = sp.int("PREFS-THEME-cHRoomText")?.let { Color(it) } ?: theme.cHRoomText,
            cHSecondaryText = sp.int("PREFS-THEME-cHSecondaryText")?.let { Color(it) } ?: theme.cHSecondaryText,
            cChngPrimaryText = sp.int("PREFS-THEME-cChngPrimaryText")?.let { Color(it) } ?: theme.cChngPrimaryText,
            cChngRoomText = sp.int("PREFS-THEME-cChngRoomText")?.let { Color(it) } ?: theme.cChngRoomText,
            cChngSecondaryText = sp.int("PREFS-THEME-cChngSecondaryText")?.let { Color(it) } ?: theme.cChngSecondaryText,
            cAPrimaryText = sp.int("PREFS-THEME-cAPrimaryText")?.let { Color(it) } ?: theme.cAPrimaryText,
            cARoomText = sp.int("PREFS-THEME-cARoomText")?.let { Color(it) } ?: theme.cARoomText,
            cASecondaryText = sp.int("PREFS-THEME-cASecondaryText")?.let { Color(it) } ?: theme.cASecondaryText,
            cHeaderPrimaryText = sp.int("PREFS-THEME-cHeaderPrimaryText")?.let { Color(it) } ?: theme.cHeaderPrimaryText,
            cHeaderSecondaryText = sp.int("PREFS-THEME-cHeaderSecondaryText")?.let { Color(it) } ?: theme.cHeaderSecondaryText,
            spPrimaryText = sp.float("PREFS-THEME-spPrimaryText") ?: theme.spPrimaryText,
            spSecondaryText = sp.float("PREFS-THEME-spSecondaryText") ?: theme.spSecondaryText,
            dpPaddingLeft = sp.float("PREFS-THEME-dpPaddingLeft") ?: theme.dpPaddingLeft,
            dpPaddingTop = sp.float("PREFS-THEME-dpPaddingTop") ?: theme.dpPaddingTop,
            dpPaddingRight = sp.float("PREFS-THEME-dpPaddingRight") ?: theme.dpPaddingRight,
            dpPaddingBottom = sp.float("PREFS-THEME-dpPaddingBottom") ?: theme.dpPaddingBottom,
            dpTextPadding = sp.float("PREFS-THEME-dpTextPadding") ?: theme.dpTextPadding,
            cInfolineBg = sp.int("PREFS-THEME-cInfolineBg")?.let { Color(it) } ?: theme.cInfolineBg,
            cInfolineText = sp.int("PREFS-THEME-cInfolineText")?.let { Color(it) } ?: theme.cInfolineText,
            spInfolineTextSize = sp.float("PREFS-THEME-spInfolineTextSize") ?: theme.spInfolineTextSize,
            cError = sp.int("PREFS-THEME-cError")?.let { Color(it) } ?: theme.cError,
            cHomework = sp.int("PREFS-THEME-cHomework")?.let { Color(it) } ?: theme.cHomework,
            dpHomework = sp.float("PREFS-THEME-dpHomework") ?: theme.dpHomework,
            customizationLevel = sp.int("prefs-detail-level") ?: theme.customizationLevel,
        )
        sp.putOne(PrefsConsts.CUSTOM_THEME, Json.encodeToString(theme))
    }

    // constants used for keys in old versions
    private const val URL = "url"
    private const val SCHOOL_NAME = "school_name"
    private const val SCHOOL_ID = "school_id"
    private const val USERNAME = "username"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"

    /**
     * ISO formatted date time on which access token expires.
     * @see ISODateTimeFormat.dateTime
     */
    private const val ACCESS_EXPIRES = "access_expires"
    private const val NAME = "name"
    private const val TYPE = "type"
    private const val TYPE_TEXT = "type_text"

    /**
     * ISO formatted date time when the semester ends and when it is a good idea to refresh user info.
     * @see ISODateTimeFormat.dateTime
     */
    private const val SEMESTER_END = "semester_end"

    private const val PREFS_NOTIFICATION = "prefs-notification"
    /** String indication the option index */
    private const val PREFS_WEEK_SWITCH = "prefs-switch-to-next-week"

    /**
     * Access this one only using [AppSingleton.getWidgetsSettings].
     */
    const val WIDGETS_SETTINGS = "widgets-settings"

    @Serializable
    data class OldWidgetSettings (
        val widgetIds: HashSet<Int>,
        val widgets: HashMap<Int,OldWidget>
    )

    /** Old format of widget's settings */
    @Serializable
    data class OldWidget (
        val backgroundColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int,
        val primaryTextSize: Float,
        val secondaryTextSize: Float,
    )

    // theme keys

}