package cz.vitskalicky.lepsirozvrh.theme

import androidx.compose.ui.graphics.Color
import cz.vitskalicky.lepsirozvrh.BuildConfig

object DefaultRozvrhThemes {
    // Gruvbox Light
    final val LIGHT = RozvrhTheme(
        isLight = true,
        cABg = Color(0xFFebdbb2),           // gruvbox bg1
        cAPrimaryText = Color(0xFF3c3836),   // gruvbox fg
        cARoomText = Color(0xFF665c54),      // gruvbox fg2
        cASecondaryText = Color(0xFF3c3836),
        cChngBg = Color(0xFFebdbb2),
        cChngPrimaryText = Color(0xFF3c3836),
        cChngRoomText = Color(0xFF665c54),
        cChngSecondaryText = Color(0xFF3c3836),
        cDivider = Color(0xFFd5c4a1),        // gruvbox bg2
        cEmptyBg = Color(0xFFd5c4a1),
        cError = Color(0xFF9d0006),          // gruvbox red dark
        cHBg = Color(0xFFfbf1c7),            // gruvbox bg
        cHPrimaryText = Color(0xFF3c3836),
        cHRoomText = Color(0xFF665c54),
        cHSecondaryText = Color(0xFF3c3836),
        cHeaderBg = Color(0xFFd5c4a1),       // gruvbox bg2
        cHeaderPrimaryText = Color(0xFF3c3836),
        cHeaderSecondaryText = Color(0xFF504945), // gruvbox fg1
        cHighlight = Color(0xFFaf3a03),      // gruvbox orange dark
        cHomework = Color(0xFF9d0006),       // gruvbox red dark
        cInfolineBg = Color(0xFF3c3836),     // gruvbox fg (dark bar)
        cInfolineText = Color(0xFFfbf1c7),   // gruvbox bg
        cPrimary = Color(0xFFaf3a03),        // gruvbox orange dark
        cSecondary = Color(0xFF076678),      // gruvbox blue dark
        cSurface = Color(0xFFfbf1c7),        // gruvbox bg
        dpDividerWidth = 1.0f,
        dpHighlightWidth = 1.0f,
        dpHomework = 5.0f,
        dpPaddingBottom = 3.0f,
        dpPaddingLeft = 3.0f,
        dpPaddingRight = 3.0f,
        dpPaddingTop = 3.0f,
        dpTextPadding = 2.0f,
        spInfolineTextSize = 12.0f,
        spPrimaryText = 18.0f,
        spSecondaryText = 12.0f,
        customizationLevel = 0,
    )

    // Gruvbox Dark
    final val DARK = RozvrhTheme(
        isLight = false,
        cABg = Color(0xFF504945),            // gruvbox bg2
        cAPrimaryText = Color(0xFFebdbb2),   // gruvbox fg
        cARoomText = Color(0xFFa89984),      // gruvbox fg4
        cASecondaryText = Color(0xFFd5c4a1), // gruvbox fg1
        cChngBg = Color(0xFF504945),
        cChngPrimaryText = Color(0xFFebdbb2),
        cChngRoomText = Color(0xFFa89984),
        cChngSecondaryText = Color(0xFFd5c4a1),
        cDivider = Color(0xFF1d2021),        // gruvbox bg hard
        cEmptyBg = Color(0xFF1d2021),
        cError = Color(0xFFfb4934),          // gruvbox red bright
        cHBg = Color(0xFF3c3836),            // gruvbox bg1
        cHPrimaryText = Color(0xFFebdbb2),
        cHRoomText = Color(0xFFa89984),
        cHSecondaryText = Color(0xFFd5c4a1),
        cHeaderBg = Color(0xFF32302f),       // gruvbox bg soft
        cHeaderPrimaryText = Color(0xFFebdbb2),
        cHeaderSecondaryText = Color(0xFFa89984),
        cHighlight = Color(0xFFfe8019),      // gruvbox orange bright
        cHomework = Color(0xFFfabd2f),       // gruvbox yellow bright
        cInfolineBg = Color(0xFF1d2021),     // gruvbox bg hard
        cInfolineText = Color(0xFFebdbb2),
        cPrimary = Color(0xFFfe8019),        // gruvbox orange bright
        cSecondary = Color(0xFF83a598),      // gruvbox blue bright
        cSurface = Color(0xFF282828),        // gruvbox bg
        dpDividerWidth = 1.0f,
        dpHighlightWidth = 1.0f,
        dpHomework = 5.0f,
        dpPaddingBottom = 3.0f,
        dpPaddingLeft = 3.0f,
        dpPaddingRight = 3.0f,
        dpPaddingTop = 3.0f,
        dpTextPadding = 2.0f,
        spInfolineTextSize = 12.0f,
        spPrimaryText = 18.0f,
        spSecondaryText = 12.0f,
        customizationLevel = 0,
    )
    final val BLACK = RozvrhTheme(
        isLight = false,
        cABg = Color(0xFF464C4F),
        cAPrimaryText = Color(0xFFFFFFFF),
        cARoomText = Color(0xFFE3E8EB),
        cASecondaryText = Color(0xFFFFFFFF),
        cChngBg = Color(0xFF464C4F),
        cChngPrimaryText = Color(0xFFFFFFFF),
        cChngRoomText = Color(0xFFE3E8EB),
        cChngSecondaryText = Color(0xFFFFFFFF),
        cDivider = Color(0xFF121212),
        cEmptyBg = Color(0xFF121212),
        cError = Color(0xFFCF6679),
        cHBg = Color(0xFF000000),
        cHPrimaryText = Color(0xFFFFFFFF),
        cHRoomText = Color(0xFFE3E8EB),
        cHSecondaryText = Color(0xFFFFFFFF),
        cHeaderBg = Color(0xFF242424),
        cHeaderPrimaryText = Color(0xFFFFFFFF),
        cHeaderSecondaryText = Color(0xFFFFFFFF),
        cHighlight = Color(0xFFF9A825),
        cHomework = Color(0xFFF9A825),
        cInfolineBg = Color(0xFF424242),
        cInfolineText = Color(0xFFFFFFFF),
        cPrimary = Color(0xFFF9A825),
        cSecondary = Color(0xFFCFD8DC),
        cSurface = Color(0xFF000000),
        dpDividerWidth = 1.5f,
        dpHighlightWidth = 1.0f,
        dpHomework = 5.0f,
        dpPaddingBottom = 3.0f,
        dpPaddingLeft = 3.0f,
        dpPaddingRight = 3.0f,
        dpPaddingTop = 3.0f,
        dpTextPadding = 2.0f,
        spInfolineTextSize = 12.0f,
        spPrimaryText = 18.0f,
        spSecondaryText = 12.0f,
        customizationLevel = 0,
    )

    // as visible as possible if debug, hope it is unnoticed in production
    final val UNSPECIFIED = if (BuildConfig.DEBUG)
        RozvrhTheme(
            isLight = false,
            cABg = Color.Magenta,
            cAPrimaryText = Color.Cyan,
            cARoomText = Color.Cyan,
            cASecondaryText = Color.Cyan,
            cChngBg = Color.Magenta,
            cChngPrimaryText = Color.Cyan,
            cChngRoomText = Color.Cyan,
            cChngSecondaryText = Color.Cyan,
            cDivider = Color.Magenta,
            cEmptyBg = Color.Magenta,
            cError = Color.Magenta,
            cHBg = Color.Magenta,
            cHPrimaryText = Color.Cyan,
            cHRoomText = Color.Cyan,
            cHSecondaryText = Color.Cyan,
            cHeaderBg = Color.Magenta,
            cHeaderPrimaryText = Color.Cyan,
            cHeaderSecondaryText = Color.Cyan,
            cHighlight = Color.Magenta,
            cHomework = Color.Magenta,
            cInfolineBg = Color.Magenta,
            cInfolineText = Color.Cyan,
            cPrimary = Color.Magenta,
            cSecondary = Color.Magenta,
            cSurface = Color.Magenta,
            dpDividerWidth = 1.0f,
            dpHighlightWidth = 1.0f,
            dpHomework = 5.0f,
            dpPaddingBottom = 3.0f,
            dpPaddingLeft = 3.0f,
            dpPaddingRight = 3.0f,
            dpPaddingTop = 3.0f,
            dpTextPadding = 2.0f,
            spInfolineTextSize = 12.0f,
            spPrimaryText = 18.0f,
            spSecondaryText = 12.0f,
            customizationLevel = 0,
        ) else LIGHT
}
