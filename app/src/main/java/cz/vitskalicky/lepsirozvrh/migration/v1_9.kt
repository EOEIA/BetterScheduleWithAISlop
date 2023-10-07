package cz.vitskalicky.lepsirozvrh.migration

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.Class
import cz.vitskalicky.lepsirozvrh.theme.DefaultRozvrhThemes
import cz.vitskalicky.lepsirozvrh.widget.WidgetsSettings.Widget
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/** Handles migration from pre- 1.9 to 1.9 */
object v1_9 {
    suspend fun migrate(context: Context){
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
        val acc: Account? = with(prefs) {
            try {
                Account(
                    serverUrl = string(URL)!!,
                    username = string(USERNAME)?:"",
                    accessToken = string(ACCEESS_TOKEN)!!,
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
                null
            }
        }
        // delete old tokens, name, etc.
        prefs.edit {
            remove(URL)
            remove(SCHOOL_NAME)
            remove(SCHOOL_ID)
            remove(USERNAME)
            remove(ACCEESS_TOKEN)
            remove(REFRESH_TOKEN)
            remove(ACCESS_EXPIRES)
            remove(NAME)
            remove(TYPE)
            remove(TYPE_TEXT)
            remove(SEMESTER_END)
        }
        if (acc == null){
            // account could not be migrated
            //todo report
            //todo full logout and cleanup
            return
        }
        // add account to database
        val app = context.applicationContext as MainApplication;
        val id = app.rozvrhDb.accountDao().insertAccount(acc);
        val account = app.rozvrhDb.accountDao().loadAccount(id);
        if (account == null){
            //todo something went terribly wrong
            return
        }
        app.accountRepository.switchToAccount(account.id)

        // update persistent notification setting
        prefs.edit {
            if (prefs.boolean(PREFS_NOTIFICATION) ?: false) {
                putLong(PrefsConsts.NOTIFICATION_ACCOUNT, account.id)
            }else{
                remove(PrefsConsts.NOTIFICATION_ACCOUNT)
            }
            remove(PREFS_NOTIFICATION)
        }
        // update widgets
        if (!prefs.string(WIDGETS_SETTINGS).isNullOrBlank()) {
            try {
                val oldWidgets: List<OldWidget> = Json.decodeFromString(prefs.string(WIDGETS_SETTINGS)!!)
                val newWidgets = oldWidgets.map {
                    Widget().apply {
                        this.accountId = account.id;
                        this.backgroundColor = it.backgroundColor
                        this.primaryTextColor = it.primaryTextColor
                        this.primaryTextSize = it.primaryTextSize
                        this.secondaryTextColor = it.secondaryTextColor
                        this.secondaryTextSize = it.secondaryTextSize
                    }
                }
                val json = ObjectMapper().writeValueAsString(newWidgets);
                prefs.edit { putString(PrefsConsts.WIDGETS_SETTINGS, json) }
            } catch (_: IllegalArgumentException){} catch (_: SerializationException){}
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
        )
        //todo save theme
    }

    // constants used for keys in old versions
    private const val URL = "url"
    private const val SCHOOL_NAME = "school_name"
    private const val SCHOOL_ID = "school_id"
    private const val USERNAME = "username"
    private const val ACCEESS_TOKEN = "access_token"
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