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

fun buildCustomColorScheme(baseColor: Color, darkTheme: Boolean): ColorScheme {
    val isPrimaryDark = (0.299f * baseColor.red + 0.587f * baseColor.green + 0.114f * baseColor.blue) < 0.6f
    val onPrimaryColor = if (isPrimaryDark) White else Grey10
    val onPrimaryContainerColor = if (darkTheme) White else if (isPrimaryDark) baseColor else Grey10

    return if (darkTheme) {
        darkColorScheme(
            primary = baseColor,
            primaryContainer = baseColor.copy(alpha = 0.35f),
            onPrimary = onPrimaryColor,
            onPrimaryContainer = onPrimaryContainerColor,
            secondary = baseColor,
            secondaryContainer = baseColor.copy(alpha = 0.25f),
            onSecondary = onPrimaryColor,
            onSecondaryContainer = onPrimaryContainerColor,
            background = Grey10,
            surface = Grey20,
            onBackground = Grey90,
            onSurface = White
        )
    } else {
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

    val isHeaderBackgroundDark = if (dynamicColor) {
        darkTheme
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
