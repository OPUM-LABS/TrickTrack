package ch.opum.tricktrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Duration.Companion.minutes

private fun sRgbToLinear(c: Float): Float {
    return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
}

fun calculateLuminance(color: Color): Float {
    return 0.2126f * sRgbToLinear(color.red) + 0.7152f * sRgbToLinear(color.green) + 0.0722f * sRgbToLinear(color.blue)
}

fun calculateContrast(c1: Color, c2: Color): Float {
    val l1 = calculateLuminance(c1)
    val l2 = calculateLuminance(c2)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun colorToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val l = (max + min) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
    val h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta).let { ((it % 6f) + 6f) % 6f } * 60f
        max == g -> (((b - r) / delta) + 2f) * 60f
        else -> (((r - g) / delta) + 4f) * 60f
    }
    return floatArrayOf(h, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (rPrime, gPrime, bPrime) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (rPrime + m).coerceIn(0f, 1f),
        green = (gPrime + m).coerceIn(0f, 1f),
        blue = (bPrime + m).coerceIn(0f, 1f),
        alpha = alpha
    )
}

fun adjustColorForDarkTheme(color: Color): Color {
    // In dark theme, text and icons appear on Grey10 (#121212) and Grey20 (#1E1E1E).
    // Ensure contrast is at least 7.0:1 (AAA) while preserving hue and pleasant saturation.
    if (calculateContrast(color, Grey10) >= 7.0f) {
        return color
    }

    val hsl = colorToHsl(color)
    val h = hsl[0]
    val s = hsl[1]
    var l = hsl[2]
    val targetS = if (s >= 0.05f) s.coerceIn(0.35f, 0.75f) else 0f

    var candidate = color
    while (l < 0.90f && calculateContrast(candidate, Grey10) < 7.0f) {
        l += 0.02f
        candidate = hslToColor(h, targetS, l, color.alpha)
    }
    return candidate
}

fun adjustColorForLightTheme(color: Color): Color {
    // In light theme, text and icons appear on White (#FFFFFF) and Grey90 (#E6E6E6).
    // Ensure contrast is at least 4.8:1 (WCAG AA) against White.
    if (calculateContrast(color, White) >= 4.8f) {
        return color
    }

    val hsl = colorToHsl(color)
    val h = hsl[0]
    val s = hsl[1]
    var l = hsl[2]
    // Maintain rich saturation when darkening light colors so they don't look muddy
    val targetS = if (s >= 0.05f) s.coerceAtLeast(0.55f) else 0f

    var candidate = color
    while (l > 0.12f && calculateContrast(candidate, White) < 4.8f) {
        l -= 0.02f
        candidate = hslToColor(h, targetS, l, color.alpha)
    }
    return candidate
}

fun buildCustomColorScheme(baseColor: Color, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        val effectivePrimary = adjustColorForDarkTheme(baseColor)
        val isPrimaryDark = (0.299f * effectivePrimary.red + 0.587f * effectivePrimary.green + 0.114f * effectivePrimary.blue) < 0.6f
        val onPrimaryColor = if (isPrimaryDark) White else Grey10

        darkColorScheme(
            primary = effectivePrimary,
            primaryContainer = effectivePrimary.copy(alpha = 0.22f),
            onPrimary = onPrimaryColor,
            onPrimaryContainer = White,
            secondary = effectivePrimary,
            secondaryContainer = effectivePrimary.copy(alpha = 0.20f),
            onSecondary = onPrimaryColor,
            onSecondaryContainer = White,
            background = Grey10,
            surface = Grey20,
            onBackground = Grey90,
            onSurface = White
        )
    } else {
        val effectivePrimary = adjustColorForLightTheme(baseColor)
        val isPrimaryDark = (0.299f * effectivePrimary.red + 0.587f * effectivePrimary.green + 0.114f * effectivePrimary.blue) < 0.6f
        val onPrimaryColor = if (isPrimaryDark) White else Grey10
        val onPrimaryContainerColor = if (isPrimaryDark) effectivePrimary else Grey10

        lightColorScheme(
            primary = effectivePrimary,
            primaryContainer = effectivePrimary.copy(alpha = 0.18f),
            onPrimary = onPrimaryColor,
            onPrimaryContainer = onPrimaryContainerColor,
            secondary = effectivePrimary,
            secondaryContainer = effectivePrimary.copy(alpha = 0.15f),
            onSecondary = onPrimaryColor,
            onSecondaryContainer = onPrimaryContainerColor,
            background = Grey90,
            surface = White,
            onBackground = Grey10,
            onSurface = Grey10
        )
    }
}

fun buildCustomColorScheme(accentColorLong: Long, darkTheme: Boolean): ColorScheme {
    val baseColor = if (accentColorLong == 0L) Color(0xFF6750A4L) else Color(accentColorLong.toInt())
    return buildCustomColorScheme(baseColor, darkTheme)
}

@Composable
fun rememberCurrentHour(): Int {
    val hourState = produceState(initialValue = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        while (true) {
            delay(1.minutes)
            value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    return hourState.value
}

@Composable
fun TrickTrackTheme(
    themeMode: String = "SYSTEM",
    accentColorHex: Long = 0xFF6750A4L,
    dynamicColor: Boolean = false,
    specialTheme: String = SpecialThemeHelper.THEME_NONE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    
    val currentHour = rememberCurrentHour()
    val headerGradient = SpecialThemeHelper.getHeaderGradient(specialTheme, currentHour)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        specialTheme != SpecialThemeHelper.THEME_NONE -> {
            val primaryColor = SpecialThemeHelper.getPrimaryColor(specialTheme, accentColorHex, currentHour)
            buildCustomColorScheme(primaryColor, darkTheme)
        }
        else -> buildCustomColorScheme(accentColorHex, darkTheme)
    }

    val isHeaderBackgroundDark = if (dynamicColor) {
        darkTheme
    } else if (headerGradient != null) {
        SpecialThemeHelper.isGradientDark(headerGradient)
    } else {
        val rawAccent = if (accentColorHex == 0L) Color(0xFF6750A4L) else Color(accentColorHex.toInt())
        (0.299f * rawAccent.red + 0.587f * rawAccent.green + 0.114f * rawAccent.blue) < 0.6f
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isHeaderBackgroundDark
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
