package cz.vitskalicky.lepsirozvrh.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.theme.DefaultRozvrhThemes
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme
import cz.vitskalicky.lepsirozvrh.theme.SelectedTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ThemeViewModel(application: Application): AndroidViewModel(application) {
    private val prefs = application.prefs

    public val selectedThemeLD: LiveData<SelectedTheme> =
        SharedPrefsIntLiveData(prefs.sharedPreferences, PrefsConsts.SELECTED_THEME, SelectedTheme.FOLLOW_SYSTEM_THEME.index)
            .map {savedIndex-> SelectedTheme.values().firstOrNull { it.index == savedIndex }?:SelectedTheme.FOLLOW_SYSTEM_THEME }

    public var selectedTheme: SelectedTheme
        get() = prefs.int(PrefsConsts.SELECTED_THEME)?.let {savedIndex-> SelectedTheme.values().firstOrNull{ savedIndex == it.index } }?:SelectedTheme.FOLLOW_SYSTEM_THEME;
        set(value) {
            prefs.putOne(PrefsConsts.SELECTED_THEME,value.index);
        }

    public val customThemeLD: LiveData<RozvrhTheme> =
        SharedPrefsStringLiveData(prefs.sharedPreferences, PrefsConsts.CUSTOM_THEME,"")
            .map { if (it.isBlank()) {null} else {Json.decodeFromString(it)}?: DefaultRozvrhThemes.UNSPECIFIED }

    public var customTheme: RozvrhTheme
        get() = prefs.string(PrefsConsts.CUSTOM_THEME)?.let { Json.decodeFromString(it) } ?: DefaultRozvrhThemes.UNSPECIFIED
        set(value) {
            prefs.putOne(PrefsConsts.CUSTOM_THEME, Json.encodeToString(value))
        }
}