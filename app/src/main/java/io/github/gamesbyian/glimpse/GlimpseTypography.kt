package io.github.gamesbyian.glimpse

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Typeface system for Glimpse's October owner art direction.
 *
 * Fraunces gives important editorial/photo titles their character, while Plus
 * Jakarta Sans retains readable everyday actions and status labels. Both
 * unmodified families are bundled locally under OFL-1.1 with exact upstream
 * sources recorded at docs/licenses/fonts/README.md. No runtime font network
 * requests, system-default dependency, or font binary redistribution API.
 */
internal object GlimpseType {
    val editorial = FontFamily(Font(R.font.fraunces_display, weight = FontWeight.Normal))
    val interfaceText = FontFamily(Font(R.font.plus_jakarta_sans, weight = FontWeight.Normal))

    private val base = Typography()
    val theme = base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = editorial),
        displayMedium = base.displayMedium.copy(fontFamily = editorial),
        displaySmall = base.displaySmall.copy(fontFamily = editorial),
        headlineLarge = base.headlineLarge.copy(fontFamily = editorial),
        headlineMedium = base.headlineMedium.copy(fontFamily = editorial),
        headlineSmall = base.headlineSmall.copy(fontFamily = editorial),
        titleLarge = base.titleLarge.copy(fontFamily = editorial),
        titleMedium = base.titleMedium.copy(fontFamily = interfaceText),
        titleSmall = base.titleSmall.copy(fontFamily = interfaceText),
        bodyLarge = base.bodyLarge.copy(fontFamily = interfaceText),
        bodyMedium = base.bodyMedium.copy(fontFamily = interfaceText),
        bodySmall = base.bodySmall.copy(fontFamily = interfaceText),
        labelLarge = base.labelLarge.copy(fontFamily = interfaceText),
        labelMedium = base.labelMedium.copy(fontFamily = interfaceText),
        labelSmall = base.labelSmall.copy(fontFamily = interfaceText)
    )
}
