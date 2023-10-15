package cz.vitskalicky.lepsirozvrh.settings

import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.KotlinUtils.str
import cz.vitskalicky.lepsirozvrh.KotlinUtils.icon
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme
import cz.vitskalicky.lepsirozvrh.theme.ThemeGenerator
import cz.vitskalicky.lepsirozvrh.view.preferences.*

class ThemeSettingsActivity {
}

/** Must match [R.array.themes_entries] and [R.array.themes_values] */
enum class SelectedTheme(val index: Int){
    FOLLOW_SYSTEM_THEME(0),
    LIGHT(1),
    DARK(2),
    BLACK(3),
    CUSTOM(4),
}

@Composable
private fun generalStateless(
    selectedTheme: SelectedTheme,
    isSupporter: Boolean,
    onThemeChange: (newValue: SelectedTheme) -> Unit,
    onSupportClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onExportClicked: () -> Unit,
    onGetMoreThemesClicked: () -> Unit
) {
    PreferenceGroupHeader(R.string.theme_general_settings.str)

    val themeEntries = stringArrayResource(R.array.themes_entries);
    val selectedThemeName = themeEntries[selectedTheme.index]
    RadioPreference(
        title = R.string.app_theme.str,
        description = selectedThemeName,
        options = themeEntries.toList(),
        selectedOptionIndex = selectedTheme.index,
        dialogTitle = { Text(R.string.app_theme.str)},
        icon = Icons.Default.Palette.icon,
    ){ newValue: Int -> onThemeChange(SelectedTheme.values().first { it.index == newValue }) }

    Preference(R.string.more_themes.str, null, Icons.Default.Add.icon,){onGetMoreThemesClicked()}

    if (!isSupporter){
        Preference(
            title = R.string.donate_title.str,
            description = R.string.donate_text1.str,
            icon = { Icon(Icons.Default.AttachMoney, null) },
        ){
            onSupportClicked()
        }
    }

    Preference(
        title = R.string.export_theme.str,
        description = R.string.export_theme_desc.str,
        icon = Icons.Default.Share.icon,
        enabled = isSupporter,
        onClicked = onExportClicked
    )
    Preference(
        title = R.string.import_theme.str,
        description = R.string.import_theme_desc.str,
        icon = Icons.Default.ExitToApp.icon, // this symbol is the closest I could find for import
        enabled = isSupporter,
        onClicked = onImportClicked
    )
}

@Composable
fun customizationsStateless(
    theme: RozvrhTheme,
    isSupporter: Boolean,
    onChange: (newValues: RozvrhTheme) -> Unit
){
    val t = theme
    val chng = { newTheme: RozvrhTheme ->
        val regen = ThemeGenerator.regenerateColors(newTheme, theme.customizationLevel);
        onChange(regen)
    }

    PreferenceGroupHeader(R.string.theme_custom_settings.str)
    ColorPreference(R.string.primary_color.str, null, enabled = isSupporter, color = t.cPrimary){chng(t.copy(cPrimary = it))}
    ColorPreference(R.string.accent_color.str, null, enabled = isSupporter, color = t.cSecondary){chng(t.copy(cSecondary = it))}
    ColorPreference(R.string.background_color.str, null, enabled = isSupporter, color = t.cSurface){chng(t.copy(cSurface = it))}

    if (t.customizationLevel == 2){
        PreferenceGroupHeader(R.string.cells_background.str)
        ColorPreference(R.string.type_normal_lesson.str, null, color = t.cHBg){chng(t.copy(cHBg = it))}
        ColorPreference(R.string.type_change.str, null, color = t.cChngBg){chng(t.copy(cChngBg = it))}
        ColorPreference(R.string.type_no_school.str, R.string.type_no_school_desc.str, color = t.cABg){chng(t.copy(cABg = it))}
        ColorPreference(R.string.type_empty.str, null, color = t.cEmptyBg){chng(t.copy(cEmptyBg = it))}
        ColorPreference(R.string.type_header.str, R.string.type_header_desc.str, color = t.cHeaderBg){chng(t.copy(cHeaderBg = it))}

        PreferenceGroupHeader(R.string.other.str)
        ColorPreference(R.string.divider_color.str, null, color = t.cDivider){chng(t.copy(cDivider = it))}
        FloatPreference(R.string.divider_width.str, value = t.dpDividerWidth, validator = { if (it<0) R.string.float_cannot_be_negative.str else null}){chng(t.copy(dpDividerWidth = it))}
        //todo went for lunch, continue here...
        // finish field for this level of detail, do the closest level of detail and add detail adjustment buttons
    }

}
