package com.kayasetu.asha.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CreamBackground = Color(0xFFFAF8F5)
val IvorySurface = Color(0xFFFFFFFF)
val SandContainer = Color(0xFFF3EFE6)
val BorderHairline = Color(0xFFE6E0D3)
val BorderStrong = Color(0xFFD2C9B8)
val CharcoalPrimary = Color(0xFF18181B)
val StoneSecondary = Color(0xFF57534E)
val MutedText = Color(0xFF78716C)

val ClinicalForest = Color(0xFF15803D)
val ClinicalForestBg = Color(0xFFECFDF5)
val ClinicalAmber = Color(0xFFB45309)
val ClinicalAmberBg = Color(0xFFFFFBEB)
val ClinicalCrimson = Color(0xFFB91C1C)
val ClinicalCrimsonBg = Color(0xFFFEF2F2)
val WhatsAppTeal = Color(0xFF128C7E)

private val KayaSetuLightScheme = lightColorScheme(
    primary = CharcoalPrimary,
    onPrimary = IvorySurface,
    primaryContainer = SandContainer,
    onPrimaryContainer = CharcoalPrimary,
    secondary = ClinicalForest,
    onSecondary = IvorySurface,
    secondaryContainer = ClinicalForestBg,
    onSecondaryContainer = ClinicalForest,
    tertiary = ClinicalAmber,
    onTertiary = IvorySurface,
    tertiaryContainer = ClinicalAmberBg,
    onTertiaryContainer = ClinicalAmber,
    error = ClinicalCrimson,
    onError = IvorySurface,
    errorContainer = ClinicalCrimsonBg,
    onErrorContainer = ClinicalCrimson,
    background = CreamBackground,
    onBackground = CharcoalPrimary,
    surface = IvorySurface,
    onSurface = CharcoalPrimary,
    surfaceVariant = SandContainer,
    onSurfaceVariant = StoneSecondary,
    outline = BorderStrong
)

val KayaSetuShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(999.dp)
)

val KayaSetuTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun KayaSetuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KayaSetuLightScheme,
        shapes = KayaSetuShapes,
        typography = KayaSetuTypography,
        content = content
    )
}
