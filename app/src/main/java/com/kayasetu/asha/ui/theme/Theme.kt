package com.kayasetu.asha.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayasetu.asha.R

// Soft, warm, minimal palette inspired by reference design
val CreamBackground = Color(0xFFFDFBF7)
val IvorySurface = Color(0xFFFFFFFF)
val SoftPeachContainer = Color(0xFFF9EAE1)
val SoftSandContainer = Color(0xFFF4EFEA)
val SoftSageContainer = Color(0xFFE6F0EC)
val SoftBlueGray = Color(0xFFDCE5EC)

val DeepNavy = Color(0xFF1E2432)
val WarmCoral = Color(0xFFE07A5F)
val SoftGold = Color(0xFFF2CC8F)
val CalmSage = Color(0xFF3D8B6E)

val BorderSubtle = Color(0xFFEDE6DF)
val BorderMedium = Color(0xFFDED4CA)
val TextPrimary = Color(0xFF1E2432)
val TextSecondary = Color(0xFF646A78)
val TextMuted = Color(0xFF8E94A2)

val StatusHighRed = Color(0xFFD64545)
val StatusHighBg = Color(0xFFFDECEC)
val StatusModAmber = Color(0xFFD97724)
val StatusModBg = Color(0xFFFEF3E7)
val StatusLowGreen = Color(0xFF2E856E)
val StatusLowBg = Color(0xFFEAF6F2)

// Single clean sans-serif font family across the entire app
val AppFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

private val KayaSetuColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = IvorySurface,
    primaryContainer = SoftPeachContainer,
    onPrimaryContainer = DeepNavy,
    secondary = WarmCoral,
    onSecondary = IvorySurface,
    secondaryContainer = SoftSandContainer,
    onSecondaryContainer = DeepNavy,
    tertiary = CalmSage,
    onTertiary = IvorySurface,
    tertiaryContainer = SoftSageContainer,
    onTertiaryContainer = CalmSage,
    error = StatusHighRed,
    onError = IvorySurface,
    errorContainer = StatusHighBg,
    onErrorContainer = StatusHighRed,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = IvorySurface,
    onSurface = TextPrimary,
    surfaceVariant = SoftSandContainer,
    onSurfaceVariant = TextSecondary,
    outline = BorderMedium
)

val KayaSetuShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(999.dp)
)

val KayaSetuTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

@Composable
fun KayaSetuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KayaSetuColorScheme,
        shapes = KayaSetuShapes,
        typography = KayaSetuTypography,
        content = content
    )
}
