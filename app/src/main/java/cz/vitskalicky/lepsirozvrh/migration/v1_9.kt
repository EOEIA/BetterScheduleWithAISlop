package cz.vitskalicky.lepsirozvrh.migration

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vitskalicky.lepsirozvrh.AppSingleton
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.model.Account
import cz.vitskalicky.lepsirozvrh.model.Class
import cz.vitskalicky.lepsirozvrh.prefs
import cz.vitskalicky.lepsirozvrh.widget.WidgetsSettings.Widget
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/** Handles migration from pre- 1.9 to 1.9 */
object v1_9 {
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
                    username = string(USERNAME)!!,
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

    fun theme(){
        //todo
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

}