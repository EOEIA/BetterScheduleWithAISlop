/**
 * Copied from Stack overflow: https://stackoverflow.com/a/53028546
 *
 * CC BY-SA 4.0 Abhishek Luthra, modified by Vít Skalický
 */
package cz.vitskalicky.lepsirozvrh

import android.content.SharedPreferences
import androidx.lifecycle.LiveData


abstract class SharedPrefsLiveData<T>(val sharedPrefs: SharedPreferences,
                                           val key: String,
                                           val defValue: T) : LiveData<T>() {

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == this.key) {
            value = getValueFromPreferences(key, defValue)
        }
    }

    abstract fun getValueFromPreferences(key: String, defValue: T): T

    override fun onActive() {
        super.onActive()
        value = getValueFromPreferences(key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onInactive()
    }
}

class SharedPrefsIntLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Int) :
    SharedPrefsLiveData<Int>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Int): Int = sharedPrefs.getInt(key, defValue)
}

class SharedPrefsStringLiveData(sharedPrefs: SharedPreferences, key: String, defValue: String) :
    SharedPrefsLiveData<String>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: String): String = sharedPrefs.getString(key, defValue)?: defValue
}

class SharedPrefsBooleanLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Boolean) :
    SharedPrefsLiveData<Boolean>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Boolean): Boolean = sharedPrefs.getBoolean(key, defValue)
}

class SharedPrefsFloatLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Float) :
    SharedPrefsLiveData<Float>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Float): Float = sharedPrefs.getFloat(key, defValue)
}

class SharedPrefsLongLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Long) :
    SharedPrefsLiveData<Long>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Long): Long = sharedPrefs.getLong(key, defValue)
}

class SharedPrefsStringSetLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Set<String>) :
    SharedPrefsLiveData<Set<String>>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Set<String>): Set<String> = sharedPrefs.getStringSet(key, defValue) ?: defValue
}

fun SharedPreferences.intLiveData(key: String, defValue: Int): SharedPrefsLiveData<Int> {
    return SharedPrefsIntLiveData(this, key, defValue)
}

fun SharedPreferences.stringLiveData(key: String, defValue: String): SharedPrefsLiveData<String> {
    return SharedPrefsStringLiveData(this, key, defValue)
}

fun SharedPreferences.booleanLiveData(key: String, defValue: Boolean): SharedPrefsLiveData<Boolean> {
    return SharedPrefsBooleanLiveData(this, key, defValue)
}

fun SharedPreferences.floatLiveData(key: String, defValue: Float): SharedPrefsLiveData<Float> {
    return SharedPrefsFloatLiveData(this, key, defValue)
}

fun SharedPreferences.longLiveData(key: String, defValue: Long): SharedPrefsLiveData<Long> {
    return SharedPrefsLongLiveData(this, key, defValue)
}

fun SharedPreferences.stringSetLiveData(key: String, defValue: Set<String>): SharedPrefsLiveData<Set<String>> {
    return SharedPrefsStringSetLiveData(this, key, defValue)
}