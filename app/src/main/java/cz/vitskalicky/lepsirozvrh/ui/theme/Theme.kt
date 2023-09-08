package cz.vitskalicky.lepsirozvrh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import cz.vitskalicky.lepsirozvrh.theme.DefaultRozvrhThemes
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme
import cz.vitskalicky.lepsirozvrh.theme.ThemeGenerator.darker
import cz.vitskalicky.lepsirozvrh.theme.ThemeGenerator.textColorFor

private val DarkColorPalette = darkColors(
    primary = Yellow800,
    primaryVariant = Yellow900,
    secondary = BlueGrey700,
    onPrimary = Color.Black,
    onSecondary = Color.White
)

private val LightColorPalette = lightColors(
    primary = Yellow800,
    primaryVariant = Yellow900,
    secondary = BlueGrey700,
    onPrimary = Color.Black,
    onSecondary = Color.White

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

private fun RozvrhTheme.colors(isLight: Boolean = this.isLight) = Colors(
    surface = cSurface,
    primary = cPrimary,
    primaryVariant = cPrimary.darker(),
    secondary = cSecondary,
    secondaryVariant = cSecondary.darker(),
    background = cSurface,
    error = cError,
    onPrimary = textColorFor(cPrimary),
    onSecondary = textColorFor(cSecondary),
    onBackground = textColorFor(cSurface),
    onSurface = textColorFor(cSurface),
    onError = textColorFor(cError),
    isLight = isLight
)


val LocalRozvrhTheme = compositionLocalOf { DefaultRozvrhThemes.LIGHT }

@Composable
fun LepsirozvrhTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val rozvrhTheme = if (darkTheme){
        DefaultRozvrhThemes.DARK
    }else{
        DefaultRozvrhThemes.LIGHT
    }
    val colors = rozvrhTheme.colors()

    CompositionLocalProvider(LocalRozvrhTheme provides rozvrhTheme){
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = {
                val suiController = rememberSystemUiController()
                suiController.setStatusBarColor(
                    MaterialTheme.colors.primarySurface.darker()
                )
                content()
            }
        )
    }

}