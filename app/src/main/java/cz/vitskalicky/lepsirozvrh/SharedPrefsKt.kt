package cz.vitskalicky.lepsirozvrh

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

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
    fun putOne(key:String, value: String) = sharedPreferences.edit().apply { putString(key, value);apply() }
    /** Use only to save single values. For batch operations, use [edit].*/

    fun putOne(key:String, value: Int) = sharedPreferences.edit().apply { putInt(key, value);apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Boolean) = sharedPreferences.edit().apply { putBoolean(key, value);apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Float) = sharedPreferences.edit().apply { putFloat(key, value);apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Long) = sharedPreferences.edit().apply { putLong(key, value);apply() }
    /** Use only to save single values. For batch operations, use [edit].*/
    fun putOne(key:String, value: Set<String>) = sharedPreferences.edit().apply { putStringSet(key, value);apply() }

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

/** Keys for various settings more are in the old [SharedPrefs]*/
object PrefsConsts {
    const val ACTIVE_ACCOUNT_ID = "long_active_account_id"
    const val LAST_SCHOOLS_LIST_UPDATE = "prefs-last-schools-list-update"
    const val SHOW_INFO_LINE = "prefs-show-info-line"
    const val SWITCH_TO_NEXT_WEEK_OPTION_INDEX = "prefs-switch-to-next-week"
    const val CENTER_TO_CURRENT_LESSON = "prefs-center-to-current-lesson"
    /**
     * Account id for which persistent notification is active. Invalid id => persistent notification disabled
     */
    const val NOTIFICATION_ACCOUNT = "notification-account"
    const val NOTIFICATION_PLEASE_GRANT_PERMISSION = "notification-please-grant-permission"
    /** this is where [AppSingleton] stores widgets settings */
    const val WIDGETS_SETTINGS = "widgets-settings-v2"
}
