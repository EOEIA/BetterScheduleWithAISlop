package cz.vitskalicky.lepsirozvrh.theme

fun RozvrhTheme.compact(): RozvrhTheme = copy(
    dpPaddingLeft   = dpPaddingLeft   * 0.55f,
    dpPaddingTop    = dpPaddingTop    * 0.55f,
    dpPaddingRight  = dpPaddingRight  * 0.55f,
    dpPaddingBottom = dpPaddingBottom * 0.55f,
    dpTextPadding   = dpTextPadding   * 0.55f,
    spPrimaryText   = spPrimaryText   * 0.88f,
    spSecondaryText = spSecondaryText * 0.88f,
)
