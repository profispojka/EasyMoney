package cz.calmmoney.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Výchozí: bílý papír / černý inkoust.
private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Ink,
    onSecondary = Paper,
    tertiary = Ink,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = Gray700,
    outline = Ink,
    outlineVariant = Gray300,
    error = Ink,
    onError = Paper,
)

/**
 * Monochromatické téma CalmMoney pro E-Ink. Jediný motiv: bílý papír / černý inkoust.
 * Bez dynamických barev, bez přepínání motivu. Ripple efekt vypnutý (na E-Ink ruší).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmMoneyTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CalmTypography,
        shapes = CalmShapes,
    ) {
        // Vypnutí ripple animace (E-Ink: žádné animace).
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            content()
        }
    }
}
