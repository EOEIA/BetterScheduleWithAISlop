package cz.vitskalicky.lepsirozvrh.theme

import android.os.Parcelable
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.jaredrummler.cyanea.Cyanea
import cz.vitskalicky.lepsirozvrh.ColorParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * Color data for all the UI including the schedule table
 * */
@Parcelize
@TypeParceler<Color, ColorParceler>()
data class RozvrhTheme (
    @get:JvmName("isLight") val isLight: Boolean,
    @get:JvmName("cPrimary") val cPrimary: Color,
    @get:JvmName("cSecondary") val cSecondary: Color,
    @get:JvmName("cSurface") val cSurface: Color,
    // my addition values
    // most of them, are for cell views
    @get:JvmName("cEmptyBg") val cEmptyBg: Color,
    @get:JvmName("cABg") val cABg: Color,
    @get:JvmName("cHBg") val cHBg: Color,
    @get:JvmName("cChngBg") val cChngBg: Color,
    @get:JvmName("cHeaderBg") val cHeaderBg: Color,

    @get:JvmName("cDivider") val cDivider: Color,
    @get:JvmName("dpDividerWidth") val dpDividerWidth: Float,
    @get:JvmName("cHighlight") val cHighlight: Color, //for current lesson

    @get:JvmName("dpHighlightWidth") val dpHighlightWidth: Float,

    @get:JvmName("cHPrimaryText") val cHPrimaryText: Color,
    @get:JvmName("cHRoomText") val cHRoomText: Color,
    @get:JvmName("cHSecondaryText") val cHSecondaryText: Color,
    @get:JvmName("cChngPrimaryText") val cChngPrimaryText: Color,
    @get:JvmName("cChngRoomText") val cChngRoomText: Color,
    @get:JvmName("cChngSecondaryText") val cChngSecondaryText: Color,
    @get:JvmName("cAPrimaryText") val cAPrimaryText: Color,
    @get:JvmName("cARoomText") val cARoomText: Color,
    @get:JvmName("cASecondaryText") val cASecondaryText: Color,
    @get:JvmName("cHeaderPrimaryText") val cHeaderPrimaryText: Color,
    @get:JvmName("cHeaderSecondaryText") val cHeaderSecondaryText: Color,
    @get:JvmName("spPrimaryText") val spPrimaryText: Float,
    @get:JvmName("spSecondaryText") val spSecondaryText: Float,

    @get:JvmName("dpPaddingLeft") val dpPaddingLeft: Float,
    @get:JvmName("dpPaddingTop") val dpPaddingTop: Float,
    @get:JvmName("dpPaddingRight") val dpPaddingRight: Float,
    @get:JvmName("dpPaddingBottom") val dpPaddingBottom: Float,
    @get:JvmName("dpTextPadding") val dpTextPadding: Float,

    // info line
    @get:JvmName("cInfolineBg") val cInfolineBg: Color,
    @get:JvmName("cInfolineText") val cInfolineText: Color,
    @get:JvmName("spInfolineTextSize") val spInfolineTextSize: Float,

    @get:JvmName("cError") val cError: Color,

    @get:JvmName("cHomework") val cHomework: Color,
    @get:JvmName("dpHomework") val dpHomework: Float,
) : Parcelable {

}

object ThemeGenerator{

    /**
     * Generates part of the color so that user can specify only primary and accent (or more) and the rest will be generated in a half-decent-looking way.
     *
     * @param customizationLevel how many colors should be generated. The lower, the more is generated. 3 none (everything is specified by the user); 2 generate text colors and size; 1 generate cell colors based on primary and accent colors (text colors too); 0 one of the pre-set themes is used (do not touch anything)
     */
    fun regenerateColors(theme: RozvrhTheme, customizationLevel: Int): RozvrhTheme {
        val primary = theme.cPrimary
        val accent = theme.cSecondary
        val background = theme.cSurface
        if (customizationLevel < 1) {
            return theme
        }
        var newTheme = theme;
        if (customizationLevel < 2) {
            //cell colors
            newTheme = newTheme.copy(
                cDivider = background,
            cEmptyBg = generateEmptyCellColor(primary, accent, background),
            cHBg = generateHodinaColor(primary, accent, background),
            cABg = generateChangeColor(primary, accent, background),
            cChngBg = generateChangeColor(primary, accent, background),
            cHeaderBg = generateHeaderColor(primary, accent, background),
            cHighlight = generateHighlightColor(primary, accent, background),
            cHomework = generateHomeworkColor(primary, accent, background),
            dpDividerWidth =1f,
            dpHighlightWidth =1f,
            dpHomework =5f,
            cInfolineBg =Color(0xff424242),
            )
        }
        if (customizationLevel < 3) {
            //generate text colors and sizes
            val colorsH = generateTextColors(primary, accent, background, newTheme.cHBg)
            val colorsChng = generateTextColors(primary, accent, background, newTheme.cChngBg)
            val colorsA = generateTextColors(primary, accent, background, newTheme.cABg)
            val colorsHeader = generateTextColors(primary, accent, background, newTheme.cHeaderBg)
            newTheme = newTheme.copy(
            spPrimaryText = 18f,
            spSecondaryText = 12f,
            spInfolineTextSize = 12f,
            cHPrimaryText = colorsH[0],
            cHSecondaryText = colorsH[1],
            cHRoomText = colorsH[2],
            cChngPrimaryText = colorsChng[0],
            cChngSecondaryText = colorsChng[1],
            cChngRoomText = colorsChng[2],
            cAPrimaryText = colorsA[0],
            cASecondaryText = colorsA[1],
            cARoomText = colorsA[2],
            cHeaderPrimaryText = colorsHeader[0],
            cHeaderSecondaryText = colorsHeader[1],
            cInfolineText = textColorFor(newTheme.cInfolineBg),
            )
        }
        return newTheme
    }

    /**
     * [0] primary text.
     * [1] secondary text,
     * [2] third (accent of accent color) text
     */
    fun generateTextColors(primaryColor: Color, accentColor: Color, backgroundColor: Color, cellBackground: Color): Array<Color> {
        val ret = Array<Color>(3){Color.Black}
        val textColor = textColorFor(cellBackground)
        ret[0] = textColor
        ret[1] = textColor
        var third = mix(accentColor, textColor, -0.3f)
        if (!isLegible(third, backgroundColor)) {
            third = mix(primaryColor, textColor, -0.3f)
            if (!isLegible(third, backgroundColor)) {
                third = textColor
            }
        }
        ret[2] = third
        return ret
    }

    fun generateEmptyCellColor(primaryColor: Color, accentColor: Color, backgroundColor: Color): Color {
        return backgroundColor
    }

    fun darker(color: Color, @FloatRange(from = 0.0, to = 1.0) factor: Float = 0.85f): Color {
        val rgba = color.convert(ColorSpaces.Srgb)
        return Color(Math.max(((rgba.red *255).toInt() * factor).toInt(), 0),
            Math.max(((rgba.green *255).toInt() * factor).toInt(), 0),
            Math.max(((rgba.blue *255).toInt() * factor).toInt(), 0),
            (rgba.alpha *255).toInt())
    }

    @JvmName("extensionDarker")
    fun Color.darker(@FloatRange(from = 0.0, to = 1.0) factor: Float = 0.85f): Color = ThemeGenerator.darker(this, factor)

    fun lighter(color: Color, @FloatRange(from = 0.0, to = 1.0) factor: Float = 0.15f): Color {
        val rgba = color.convert(ColorSpaces.Srgb)
        val alpha = (rgba.alpha *255).toInt()
        val red = (((rgba.red *255).toInt() * (1 - factor) / 255 + factor) * 255).toInt()
        val green = (((rgba.green *255).toInt() * (1 - factor) / 255 + factor) * 255).toInt()
        val blue = (((rgba.blue *255).toInt() * (1 - factor) / 255 + factor) * 255).toInt()
        return Color(red, green, blue,alpha)
    }

    @JvmName("extensionLighter")
    fun Color.lighter( @FloatRange(from = 0.0, to = 1.0) factor: Float = 0.15f): Color = ThemeGenerator.lighter(this, factor)

    fun generateHodinaColor(primaryColor: Color, accentColor: Color, backgroundColor: Color): Color {
        val lightSumBg = backgroundColor.luminance()
        val maxColor: Color = Color.White.darker()
        val lightSumMax = maxColor.luminance()
        return if (lightSumBg > lightSumMax) {
            backgroundColor.darker( 0.9f)
        } else {
            backgroundColor.lighter( 0.1f)
        }
    }

    fun generateChangeColor(primaryColor: Color, accentColor: Color, backgroundColor: Color): Color {
        return mix(accentColor, backgroundColor, 0.2f)
    }

    fun generateHeaderColor(primaryColor: Color, accentColor: Color, backgroundColor: Color): Color {
        return mix(backgroundColor, primaryColor, 0.9f)
    }

    fun generateHighlightColor(primaryColor: Color, accentColor: Color, backgroundColor: Color): Color {
        return primaryColor
    }

    fun generateHomeworkColor(primaryColor: Color, accentColor: Color, backgroundColor: Color): Color {
        return accentColor
    }

    /**
     * Actually only averages the two colors. Mixes alpha too
     *
     * @param baseColor   one color
     * @param addedColor  other color
     * @param addedWeight from -1.0 to 1.0 - if 0, both colors are mixed equally, if 1 the base color is completely ignored.
     * @return resulting color
     */
    fun mix(baseColor: Color, addedColor: Color, addedWeight: Float): Color {
        val baseARGB = baseColor.toArgb()
        val base = IntArray(4)
        base[0] = android.graphics.Color.red(baseARGB)
        base[1] = android.graphics.Color.green(baseARGB)
        base[2] = android.graphics.Color.blue(baseARGB)
        base[3] = android.graphics.Color.alpha(baseARGB)
        val addedARGB = addedColor.toArgb()
        val added = IntArray(4)
        added[0] = android.graphics.Color.red(addedARGB)
        added[1] = android.graphics.Color.green(addedARGB)
        added[2] = android.graphics.Color.blue(addedARGB)
        added[3] = android.graphics.Color.alpha(addedARGB)
        val mix = IntArray(4)
        mix[0] = Math.round((base[0] * (1 - addedWeight) + added[0] * (1 + addedWeight)) / 2f)
        mix[1] = Math.round((base[1] * (1 - addedWeight) + added[1] * (1 + addedWeight)) / 2f)
        mix[2] = Math.round((base[2] * (1 - addedWeight) + added[2] * (1 + addedWeight)) / 2f)
        mix[3] = Math.round((base[3] * (1 - addedWeight) + added[3] * (1 + addedWeight)) / 2f)
        return Color(mix[0], mix[1], mix[2],mix[3])
    }

    /**
     * Returns white or black whichever is more legible on the background
     * @param background
     * @return
     */
    fun textColorFor(background: Color): Color {
        return whichTextColor(background, listOf(Color.Black, Color.White))
    }

    /**
     * Return the most legible color for the background;
     */
    fun whichTextColor(background: Color, textColors: List<Color>): Color {
        var bestColor = Color.Black
        var bestContrast = 0.0
        for (i in textColors.indices) {
            val currentContrast = calculateContrast(textColors[i], background)
            if (currentContrast > bestContrast) {
                bestContrast = currentContrast
                bestColor = textColors[i]
            }
        }
        return bestColor
    }

    /**
     * Returns `false` if the contrast between he text and background is too low
     */
    fun isLegible(textColor: Color, backgroundColor: Color): Boolean {
        return calculateContrast(textColor, backgroundColor) > 4.5
    }

    /**
     * Returns `false` if the contrast between he text and background is too low
     */
    fun isLegible(textColor: Color, backgroundColor: Color, minContrast: Double): Boolean {
        return calculateContrast(textColor, backgroundColor) > minContrast
    }

    fun calculateContrast(a: Color, b: Color) = ColorUtils.calculateContrast(a.toArgb(), b.toArgb())
}