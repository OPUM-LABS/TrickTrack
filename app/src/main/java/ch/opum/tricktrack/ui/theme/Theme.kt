package ch.opum.tricktrack.ui.theme

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

fun buildCustomColorScheme(accentColorLong: Long, darkTheme: Boolean): ColorScheme {
    val baseColor = if (accentColorLong == 0L) Color(0xFF6750A4L) else Color(accentColorLong.toInt())
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

@Composable
fun TrickTrackTheme(
    themeMode: String = "SYSTEM",
    accentColorHex: Long = 0xFF6750A4L,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildCustomColorScheme(accentColorHex, darkTheme)
    }

    val isHeaderBackgroundDark = if (dynamicColor) {
        darkTheme
    } else {
        (0.299f * colorScheme.primary.red + 0.587f * colorScheme.primary.green + 0.114f * colorScheme.primary.blue) < 0.6f
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    AndroidColor.TRANSPARENT,
                    AndroidColor.TRANSPARENT,
                    detectDarkMode = { isHeaderBackgroundDark }
                ),
                navigationBarStyle = SystemBarStyle.auto(
                    AndroidColor.TRANSPARENT,
                    AndroidColor.TRANSPARENT,
                    detectDarkMode = { darkTheme }
                )
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
