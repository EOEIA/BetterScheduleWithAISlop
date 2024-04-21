package cz.vitskalicky.lepsirozvrh

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification
import cz.vitskalicky.lepsirozvrh.theme.DefaultRozvrhThemes
import cz.vitskalicky.lepsirozvrh.theme.SelectedTheme

/** Shortcuts for working with shared preferences */
class SharedPrefsKt(context: Context){
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @JvmName("getString")
    fun string(key: String): String? = sharedPreferences.getString(key,null)
    @JvmName("getInt")
    fun int(key: String): Int? = if (sharedPreferences.contains(key)) sharedPreferences.getInt(key,0) else null
    @JvmName("getBoolean")
    fun boolean(key: String): Boolean? = if (sharedPreferences.contains(key)) sharedPreferences.getBoolean(key,false) else null
    @JvmName("getFloat")
    fun float(key: String): Float? = if (sharedPreferences.contains(key)) sharedPreferences.getFloat(key,0.0f) else null
    @JvmName("getLong")
    fun long(key: String): Long? = if (sharedPreferences.contains(key)) sharedPreferences.getLong(key,0) else null

    @JvmName("getStringSet")
    fun stringSet(key: String): Set<String>? = if (sharedPreferences.contains(key)) sharedPreferences.getStringSet(key,null) else null

    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: String) = sharedPreferences.edit().let { it.putString(key, value);it.apply() }
    /** Use only to save single values. For batch operations, use [edit].*/

    fun putOne(key:String, value: Int) = sharedPreferences.edit().let { it.putInt(key, value);it.apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Boolean) = sharedPreferences.edit().let { it.putBoolean(key, value);it.apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Float) = sharedPreferences.edit().let { it.putFloat(key, value);it.apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Long) = sharedPreferences.edit().let { it.putLong(key, value);it.apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Set<String>) = sharedPreferences.edit().let { it.putStringSet(key, value);it.apply() }

    /** Use only to save single values. For batch operations, use [edit].*/
    fun deleteOne(key: String) = sharedPreferences.edit().apply { remove(key) }

    /** Performs edits on the shared preferences and applies them automatically.*/
    fun edit(block: SharedPreferences.Editor.() -> Unit){
        val editor = sharedPreferences.edit();
        block(editor)
        editor.apply()
    }

    fun contains(key: String): Boolean = sharedPreferences.contains(key)
}

val Context.prefs: SharedPrefsKt
    get() = SharedPrefsKt(this)

/** Keys for various settings. more are in the old [SharedPrefs]*/
object PrefsConsts { //todo transform into some getters/setters (with livedata)
    const val ACTIVE_ACCOUNT_ID = "long_active_account_id"
    const val LAST_SCHOOLS_LIST_UPDATE = "prefs-last-schools-list-update"
    /** non-set is treated as true */
    const val SHOW_INFO_LINE = "prefs-show-info-line"
    const val SWITCH_TO_NEXT_WEEK_OPTION_INDEX = "int_switch_to_next_week_option_index"
    /** non-set is treated as true */
    const val CENTER_TO_CURRENT_LESSON = "prefs-center-to-current-lesson"
    /**
     * Account id for which persistent notification is active. Invalid id or not set => the active account has been logged out. Id == [PermanentNotification.ACCOUNT_NOTIFICATION_DISABLED] => notifications have been intentionally disabled by the user.
     */
    const val NOTIFICATION_ACCOUNT = "notification-account"
    const val NOTIFICATION_PLEASE_GRANT_PERMISSION = "notification-please-grant-permission"
    /** If set to true, user has intentionally dismissed the "please enable notifications" banner in settings */
    const val NOTIFICATION_DONT_SHOW_SETTINGS_BANNER = "notification-dont-show-settings-banner"
    /** this is where [AppSingleton] stores widgets settings */
    const val WIDGETS_SETTINGS = "widgets-settings-v2"
    /** Whether user allowed sending crash reports. Treat `null` as false and [BuildConfig.ALLOW_SENTRY] must be true as well to actually send the crashes */
    const val ENABLE_SENTRY = "prefs-send-crash-reports"
    /** If the user has seen the welcome screen */
    const val SEEN_WELCOME = "boolean_seen_welcome"
    /** The version code of the app when it was last launched*/
    const val LAST_VERSION_SEEN = "last_version_seen";
    /** The version code of the app when MainActivity was last launched. useful for showing "what's new" toast*/
    const val LAST_VERSION_SEEN_MAIN_ACTIVITY = "int_last_version_seen_main_activity";
    /** Which theme the user selected. Save [SelectedTheme] into this.*/
    const val SELECTED_THEME = "selected_theme"
    /** The data of the current theme */
    const val CUSTOM_THEME = "rozvrh_theme"

    /** Sets up some defaults. Do not rely on this however */
    fun setupDefaults(context: Context){
        with(context.prefs){ edit {
            if (!contains(SHOW_INFO_LINE)) putBoolean(SHOW_INFO_LINE, true)
            if (!contains(CENTER_TO_CURRENT_LESSON)) putBoolean(CENTER_TO_CURRENT_LESSON, true)
            if (!contains(SWITCH_TO_NEXT_WEEK_OPTION_INDEX)) putInt(SWITCH_TO_NEXT_WEEK_OPTION_INDEX, 2)
            if (!contains(SELECTED_THEME)) putInt(SELECTED_THEME, SelectedTheme.FOLLOW_SYSTEM_THEME.index)
            if (!contains(NOTIFICATION_ACCOUNT)) putLong(NOTIFICATION_ACCOUNT, PermanentNotification.ACCOUNT_NOTIFICATION_LOGGED_OUT)
            DefaultRozvrhThemes.LIGHT //todo wtf is this

            if (!contains(CUSTOM_THEME)) sharedPreferences.edit().apply { } //todo this seems unfinished
        } }
    }
}

// <editor-fold summary="experiment">
// todo experiment - this would make it even more safe
//class Prefs(context: Context) {
//    private val sp: SharedPrefsKt = SharedPrefsKt(context);
//
//    inner class StringPreference(val key: String, val defaultValue: String){
//        var walue: String
//            get() { return sp.string(key) ?: defaultValue }
//            set(value) = sp.putOne(key, value);
//        val liveData: SharedPrefsStringLiveData by lazy { SharedPrefsStringLiveData(sp.sharedPreferences, key, defaultValue) }
//    }
//    inner class IntPreference(val key: String, val defaultValue: Int){
//        var walue: Int
//            get() { return sp.int(key) ?: defaultValue }
//            set(value) = sp.putOne(key, value);
//        val liveData: SharedPrefsIntLiveData by lazy { SharedPrefsIntLiveData(sp.sharedPreferences, key, defaultValue) }
//    }
//    inner class BooleanPreference(val key: String, val defaultValue: Boolean){
//        var walue: Boolean
//            get() { return sp.boolean(key) ?: defaultValue }
//            set(value) = sp.putOne(key, value);
//        val liveData: SharedPrefsBooleanLiveData by lazy { SharedPrefsBooleanLiveData(sp.sharedPreferences, key, defaultValue) }
//    }
//    inner class FloatPreference(val key: String, val defaultValue: Float){
//        var walue: Float
//            get() { return sp.float(key) ?: defaultValue }
//            set(value) = sp.putOne(key, value);
//        val liveData: SharedPrefsFloatLiveData by lazy { SharedPrefsFloatLiveData(sp.sharedPreferences, key, defaultValue) }
//    }
//    inner class LongPreference(val key: String, val defaultValue: Long){
//        var walue: Long
//            get() { return sp.long(key) ?: defaultValue }
//            set(value) = sp.putOne(key, value);
//        val liveData: SharedPrefsLongLiveData by lazy { SharedPrefsLongLiveData(sp.sharedPreferences, key, defaultValue) }
//    }
//
//    val activeAccount = IntPreference("long_active_account_id", null)
//}
// </editor-fold>
