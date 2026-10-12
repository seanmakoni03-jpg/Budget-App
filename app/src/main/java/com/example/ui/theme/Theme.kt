package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GoldAccent,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    background = DarkBackground,
    onBackground = SlateText,
    surface = DarkSurface,
    onSurface = SlateText,
    surfaceVariant = EmeraldSurfaceCard,
    onSurfaceVariant = SlateTextMuted,
    outline = EmeraldBorder,
    outlineVariant = Color(0xFF0D332A),
    error = ExpenseRose,
    onError = Color.White
)

private val VaultLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = ExpenseRose,
    onError = Color.White
)

@Composable
fun VaultFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VaultDarkColorScheme else VaultLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
