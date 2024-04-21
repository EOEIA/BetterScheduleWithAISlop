package cz.vitskalicky.lepsirozvrh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import cz.vitskalicky.lepsirozvrh.*
import cz.vitskalicky.lepsirozvrh.theme.DefaultRozvrhThemes
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme
import cz.vitskalicky.lepsirozvrh.theme.SelectedTheme
import cz.vitskalicky.lepsirozvrh.theme.ThemeGenerator.darker
import cz.vitskalicky.lepsirozvrh.theme.ThemeGenerator.textColorFor
import kotlinx.serialization.json.Json

//Here is the theme for Jetpack Compose

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


val LocalRozvrhTheme = compositionLocalOf { DefaultRozvrhThemes.UNSPECIFIED }

@Composable
fun LepsirozvrhTheme(darkTheme: Boolean = isSystemInDarkTheme(), hasAppBar: Boolean = true, tintStatusBar: Boolean = true, content: @Composable () -> Unit) {
    val prefs = LocalContext.current.prefs
    val app = LocalContext.current.applicationContext as MainApplication;
    val themeLD: LiveData<RozvrhTheme?> = app.getThemeLD(darkTheme);
    //todo this is too slow at loading themes. Make tit faster
    val rt by themeLD.observeAsState()
    val rozvrhTheme = rt ?: DefaultRozvrhThemes.UNSPECIFIED // :(
    val colors = rozvrhTheme.colors()

    CompositionLocalProvider(LocalRozvrhTheme provides rozvrhTheme){
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = {
                if (tintStatusBar) {
                    val suiController = rememberSystemUiController()
                    suiController.setStatusBarColor(
                        if (hasAppBar) MaterialTheme.colors.primarySurface.darker() else MaterialTheme.colors.surface.darker()
                    )
                }
                content()
            }
        )
    }

}