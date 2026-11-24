package com.ntk.callshield.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ntk.callshield.domain.ThemeMode

@Immutable
data class ShieldSemanticColors(
    val verified: Color,
    val verifiedContainer: Color,
    val mint: Color,
    val caution: Color,
    val cautionContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val cardBorder: Color,
    val isAmoled: Boolean,
    val isDark: Boolean
)

val LocalShieldColors = staticCompositionLocalOf {
    ShieldSemanticColors(
        verified = TrustTeal,
        verifiedContainer = TealContainerLight,
        mint = LuminousMint,
        caution = AmberInkLight,
        cautionContainer = AmberContainerLight,
        danger = CoralInkLight,
        dangerContainer = CoralContainerLight,
        info = CalmIndigo,
        infoContainer = IndigoContainerLight,
        cardBorder = HairlineLight,
        isAmoled = false,
        isDark = false
    )
}

private val LightColorScheme = lightColorScheme(
    primary = TrustTeal,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF0F766E),
    secondary = DeepSlateInk,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = Color(0xFF0F766E),
    tertiary = AmberInkLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF78350F),
    error = CoralInkLight,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = Color(0xFF991B1B),
    background = PorcelainCanvas,
    onBackground = DeepSlateInk,
    surface = PorcelainCanvas,
    onSurface = DeepSlateInk,
    surfaceVariant = PorcelainSurfaceLow,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = PorcelainSurface,
    surfaceContainerLow = PorcelainSurfaceLow,
    surfaceContainer = PorcelainSurface,
    surfaceContainerHigh = PorcelainSurfaceHigh,
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = HairlineLight,
    outlineVariant = HairlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TrustTeal,
    onPrimary = Color.White,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = LuminousMint,
    secondary = LuminousMint,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = LuminousMint,
    tertiary = WarmAmber,
    onTertiary = Color(0xFF291800),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    error = SignalCoral,
    onError = Color.White,
    errorContainer = CoralContainerDark,
    onErrorContainer = Color(0xFFFECACA),
    background = MidnightSlate,
    onBackground = SoftCrispWhite,
    surface = MidnightSlate,
    onSurface = SoftCrispWhite,
    surfaceVariant = SlateSurfaceDark,
    onSurfaceVariant = MutedSlateDarkText,
    surfaceContainerLowest = Color(0xFF060B15),
    surfaceContainerLow = Color(0xFF0D1526),
    surfaceContainer = SlateSurfaceDark,
    surfaceContainerHigh = SlateSurfaceHighDark,
    surfaceContainerHighest = Color(0xFF243454),
    outline = HairlineDark,
    outlineVariant = Color(0xFF1D2A42)
)

private val AmoledColorScheme = DarkColorScheme.copy(
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = Color(0xFF060910),
    surfaceContainer = AmoledSurface,
    surfaceContainerHigh = AmoledSurfaceHigh,
    surfaceContainerHighest = Color(0xFF1E2B42),
    surfaceVariant = AmoledSurface,
    outline = Color(0xFF1E293B),
    outlineVariant = Color(0xFF141D2E)
)

val CallShieldShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NtkCallShieldTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val isAmoled = themeMode == ThemeMode.AMOLED
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isAmoled) {
                dynamicDarkColorScheme(context).copy(
                    background = AmoledBlack,
                    surface = AmoledBlack,
                    surfaceContainerLowest = AmoledBlack,
                    surfaceContainer = AmoledSurface
                )
            } else if (useDark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        isAmoled -> AmoledColorScheme
        useDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val semanticColors = if (useDark) {
        ShieldSemanticColors(
            verified = LuminousMint,
            verifiedContainer = TealContainerDark,
            mint = LuminousMint,
            caution = WarmAmber,
            cautionContainer = AmberContainerDark,
            danger = SignalCoral,
            dangerContainer = CoralContainerDark,
            info = Color(0xFF38BDF8),
            infoContainer = IndigoContainerDark,
            cardBorder = if (isAmoled) Color(0xFF1E293B) else HairlineDark,
            isAmoled = isAmoled,
            isDark = true
        )
    } else {
        ShieldSemanticColors(
            verified = TrustTeal,
            verifiedContainer = TealContainerLight,
            mint = LuminousMint,
            caution = AmberInkLight,
            cautionContainer = AmberContainerLight,
            danger = CoralInkLight,
            dangerContainer = CoralContainerLight,
            info = CalmIndigo,
            infoContainer = IndigoContainerLight,
            cardBorder = HairlineLight,
            isAmoled = false,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalShieldColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CallShieldTypography,
            shapes = CallShieldShapes,
            content = content
        )
    }
}
