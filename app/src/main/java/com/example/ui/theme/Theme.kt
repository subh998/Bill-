package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = LedgerNavy,
    onPrimary = Color.White,
    primaryContainer = LedgerSkyContainer,
    onPrimaryContainer = LedgerOnSkyContainer,
    secondary = SaffronAmber,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    background = PaperBackground,
    onBackground = InkSlate,
    surface = PaperSurface,
    onSurface = InkSlate,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = HairlineBorder,
    outlineVariant = Color(0xFFE5E2DA),
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = OnCrimsonContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkLedgerPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = LedgerSkyContainer,
    secondary = DarkSaffron,
    onSecondary = Color(0xFF3A1D00),
    secondaryContainer = DarkSaffronContainer,
    onSecondaryContainer = SaffronContainer,
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF003826),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = EmeraldContainer,
    background = DarkSlateBg,
    onBackground = DarkTextPrimary,
    surface = DarkSlateSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSlateVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutline,
    outlineVariant = Color(0xFF253247),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = CrimsonContainer
)

val SupplyBillShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun SupplyBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SupplyBillShapes,
        content = content
    )
}
