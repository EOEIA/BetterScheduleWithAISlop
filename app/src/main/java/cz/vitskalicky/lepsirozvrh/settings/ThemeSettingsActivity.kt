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

    val float_error_text = R.string.float_cannot_be_negative.str;

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

        //todo went for lunch, continue here...
        // finish field for this level of detail, do the closest level of detail and add detail adjustment buttons
    }else if (t.customizationLevel == 3){
        PreferenceGroupHeader(R.string.type_normal_lesson.str)
        ColorPreference(R.string.cell_primary_t.str, R.string.cell_primary_t_desc.str, color = t.cHPrimaryText){chng(t.copy(cHPrimaryText = it))}
        ColorPreference(R.string.cells_background.str, null, color = t.cHBg){chng(t.copy(cHBg = it))}
        ColorPreference(R.string.cell_secondary_t.str, R.string.cell_secondary_t_desc.str, color = t.cHSecondaryText){chng(t.copy(cHSecondaryText = it))}
        ColorPreference(R.string.cell_room_t.str, R.string.cell_room_t_desc.str, color = t.cHRoomText){chng(t.copy(cHRoomText = it))}

        PreferenceGroupHeader(R.string.type_change.str, R.string.type_change_desc.str)
        ColorPreference(R.string.cell_primary_t.str, R.string.cell_primary_t_desc.str, color = t.cChngPrimaryText){chng(t.copy(cChngPrimaryText = it))}
        ColorPreference(R.string.cells_background.str, null, color = t.cChngBg){chng(t.copy(cChngBg = it))}
        ColorPreference(R.string.cell_secondary_t.str, R.string.cell_secondary_t_desc.str, color = t.cChngSecondaryText){chng(t.copy(cChngSecondaryText = it))}
        ColorPreference(R.string.cell_room_t.str, R.string.cell_room_t_desc.str, color = t.cChngRoomText){chng(t.copy(cChngRoomText = it))}


        PreferenceGroupHeader(R.string.type_no_school.str, R.string.type_no_school_desc.str)
        ColorPreference(R.string.cell_primary_t.str, R.string.cell_primary_t_desc.str, color = t.cAPrimaryText){chng(t.copy(cAPrimaryText = it))}
        ColorPreference(R.string.cells_background.str, null, color = t.cABg){chng(t.copy(cABg = it))}
        ColorPreference(R.string.cell_secondary_t.str, R.string.cell_secondary_t_desc.str, color = t.cASecondaryText){chng(t.copy(cASecondaryText = it))}
        ColorPreference(R.string.cell_room_t.str, R.string.cell_room_t_desc.str, color = t.cARoomText){chng(t.copy(cARoomText = it))}

        PreferenceGroupHeader(R.string.type_empty.str)
        ColorPreference(R.string.cells_background.str, null, color = t.cEmptyBg){chng(t.copy(cEmptyBg = it))}

        PreferenceGroupHeader(R.string.type_header.str, R.string.type_header_desc.str)
        ColorPreference(R.string.cell_primary_t.str, R.string.cell_primary_t_desc.str, color = t.cHeaderPrimaryText){chng(t.copy(cHeaderPrimaryText = it))}
        ColorPreference(R.string.cells_background.str, null, color = t.cHeaderBg){chng(t.copy(cHeaderBg = it))}
        ColorPreference(R.string.cell_secondary_t.str, R.string.cell_secondary_t_desc.str, color = t.cHeaderSecondaryText){chng(t.copy(cHeaderSecondaryText = it))}
    }
    if (t.customizationLevel >= 2){
        PreferenceGroupHeader(R.string.other.str)
        ColorPreference(R.string.divider_color.str, null, color = t.cDivider){chng(t.copy(cDivider = it))}
        FloatPreference(R.string.divider_width.str, value = t.dpDividerWidth, validator = { if (it<0) float_error_text else null}){chng(t.copy(dpDividerWidth = it))}
        ColorPreference(R.string.highlight_color.str, R.string.highlight_color_desc.str, color = t.cHighlight){chng(t.copy(cHighlight = it))}
        FloatPreference(R.string.highlight_width.str, value = t.dpHighlightWidth, validator = { if (it<0) float_error_text else null}){chng(t.copy(dpHighlightWidth = it))}
        ColorPreference(R.string.homework_color.str, R.string.homework_color_desc.str, color = t.cHomework){chng(t.copy(cHomework = it))}
        FloatPreference(R.string.homework_size.str, value = t.dpHomework, validator = { if (it<0) float_error_text else null}){chng(t.copy(dpHomework = it))}
        ColorPreference(R.string.infoline_fill.str, R.string.infoline_fill_desc.str, color = t.cInfolineBg){chng(t.copy(cInfolineBg = it))}
    }

    if (t.customizationLevel > 1){
        Preference(R.string.customize_less.str, R.string.customize_less_desc.str, { Icons.Default.UnfoldLess.icon }){
            chng(t.copy(customizationLevel = t.customizationLevel-1))
        }
    }
    if (t.customizationLevel < 3){
        Preference(R.string.customize_more.str, R.string.customize_more_desc.str, { Icons.Default.ExpandMore.icon }){
            chng(t.copy(customizationLevel = t.customizationLevel+1))
        }
    }
}
