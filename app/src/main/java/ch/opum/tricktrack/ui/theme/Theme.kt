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
import kotlin.time.Duration.Companion.minutes

fun adjustColorForDarkTheme(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val l = (max + min) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - Math.abs(2f * l - 1f))
    val h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta).let { ((it % 6f) + 6f) % 6f } * 60f
        max == g -> (((b - r) / delta) + 2f) * 60f
        else -> (((r - g) / delta) + 4f) * 60f
    }

    // Maintain contrast against dark surfaces (Grey10 #121212 and Grey20 #1E1E1E).
    // Target lightness >= 0.72f ensures >= 7:1 (AAA) contrast ratio.
    val targetL = if (l < 0.72f) 0.72f else l

    // Preserve grayscale/monochrome if saturation is near zero; otherwise constrain
    // saturation to 0.35..0.75 to prevent glare/vibration on dark backgrounds
    val targetS = if (s >= 0.05f) s.coerceIn(0.35f, 0.75f) else 0f

    val c = (1f - Math.abs(2f * targetL - 1f)) * targetS
    val x = c * (1f - Math.abs((h / 60f) % 2f - 1f))
    val m = targetL - c / 2f

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
        alpha = color.alpha
    )
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
        val isPrimaryDark = (0.299f * baseColor.red + 0.587f * baseColor.green + 0.114f * baseColor.blue) < 0.6f
        val onPrimaryColor = if (isPrimaryDark) White else Grey10
        val onPrimaryContainerColor = if (isPrimaryDark) baseColor else Grey10

        lightColorScheme(
            primary = baseColor,
            primaryContainer = baseColor.copy(alpha = 0.18f),
            onPrimary = onPrimaryColor,
            onPrimaryContainer = onPrimaryContainerColor,
            secondary = baseColor,
            secondaryContainer = baseColor.copy(alpha = 0.15f),
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

    val isHeaderBackgroundDark = if (darkTheme) {
        if (headerGradient != null) SpecialThemeHelper.isGradientDark(headerGradient) else true
    } else if (dynamicColor) {
        false
    } else if (headerGradient != null) {
        SpecialThemeHelper.isGradientDark(headerGradient)
    } else {
        (0.299f * colorScheme.primary.red + 0.587f * colorScheme.primary.green + 0.114f * colorScheme.primary.blue) < 0.6f
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
