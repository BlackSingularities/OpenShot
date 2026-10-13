package com.blacksingularities.registrer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    // Czarno-biały interfejs pod AMOLED: tła w czystej czerni, akcent = biel, czerwień tylko dla nagrywania.
    val Amber = Color(0xFFFFFFFF)
    val Coral = Color(0xFFE0E0E0)
    val Record = Color(0xFFFF3B47)
    val Moon = Color(0xFFBDBDBD)
    val Ink = Color(0xFF000000)
    val Panel = Color(0xFF000000)
    val PanelHigh = Color(0xFF101010)
    val Glass = Color(0x29FFFFFF)
    val GlassStrong = Color(0x47000000)
    val TextDim = Color(0xB3FFFFFF)
    val TextFaint = Color(0x66FFFFFF)
    /** Cienka obwódka elementów na czarnym tle (AMOLED – reszta pikseli zgaszona). */
    val Hairline = Color(0x2EFFFFFF)
}

private val scheme = darkColorScheme(
    primary = Palette.Amber,
    onPrimary = Palette.Ink,
    secondary = Palette.Coral,
    onSecondary = Palette.Ink,
    background = Palette.Ink,
    onBackground = Color.White,
    surface = Palette.Panel,
    onSurface = Color.White,
    surfaceVariant = Palette.PanelHigh,
    onSurfaceVariant = Palette.TextDim,
    surfaceContainerLow = Palette.Panel,
    surfaceContainer = Palette.Panel,
    surfaceContainerHigh = Palette.PanelHigh,
    outline = Color(0x33FFFFFF),
)

val MonoDigits = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 14.sp)

@Composable
fun OpenShotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
