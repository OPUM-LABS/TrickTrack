package ch.opum.tricktrack.ui.theme

import androidx.compose.ui.graphics.Color
import ch.opum.tricktrack.R
import java.util.Calendar

data class SpecialThemePreset(
    val id: String,
    val titleRes: Int,
    val previewColors: List<Color>,
    val isDaytime: Boolean = false
)

object SpecialThemeHelper {

    const val THEME_NONE = "NONE"
    const val THEME_DAYTIME = "DAYTIME"
    const val THEME_SUNSET = "SUNSET"
    const val THEME_OCEAN = "OCEAN"
    const val THEME_AURORA = "AURORA"

    val specialPresets = listOf(
        SpecialThemePreset(
            id = THEME_DAYTIME,
            titleRes = R.string.special_theme_daytime,
            previewColors = listOf(Color(0xFFFF7E5F), Color(0xFF00A8E8), Color(0xFF8A2387)),
            isDaytime = true
        ),
        SpecialThemePreset(
            id = THEME_SUNSET,
            titleRes = R.string.special_theme_sunset,
            previewColors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
        ),
        SpecialThemePreset(
            id = THEME_OCEAN,
            titleRes = R.string.special_theme_ocean,
            previewColors = listOf(Color(0xFF2E3192), Color(0xFF00C0FF))
        ),
        SpecialThemePreset(
            id = THEME_AURORA,
            titleRes = R.string.special_theme_aurora,
            previewColors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
        )
    )

    fun getDaytimeColors(hour: Int): List<Color> {
        return when (hour) {
            in 5..8 -> listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B), Color(0xFFFFD194))  // Dawn / Morning
            in 9..13 -> listOf(Color(0xFF00A8E8), Color(0xFF00C49F), Color(0xFF70E0D0)) // Midday / Noon
            in 14..17 -> listOf(Color(0xFFF37335), Color(0xFFFDC830), Color(0xFFFF8C42)) // Afternoon / Golden
            in 18..21 -> listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)) // Evening / Sunset
            else -> listOf(Color(0xFF1A2A6C), Color(0xFF27408B), Color(0xFF203A43))      // Night / Midnight
        }
    }

    fun getHeaderGradient(specialTheme: String, hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): List<Color>? {
        return when (specialTheme) {
            THEME_DAYTIME -> getDaytimeColors(hour)
            THEME_SUNSET -> listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
            THEME_OCEAN -> listOf(Color(0xFF2E3192), Color(0xFF00C0FF))
            THEME_AURORA -> listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
            else -> null
        }
    }

    fun getPrimaryColor(
        specialTheme: String,
        defaultColorLong: Long,
        hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): Color {
        return when (specialTheme) {
            THEME_DAYTIME -> getDaytimeColors(hour).first()
            THEME_SUNSET -> Color(0xFFE94057)
            THEME_OCEAN -> Color(0xFF0099FF)
            THEME_AURORA -> Color(0xFF00C9FF)
            else -> if (defaultColorLong == 0L) Color(0xFF6750A4L) else Color(defaultColorLong.toInt())
        }
    }

    fun isGradientDark(colors: List<Color>): Boolean {
        if (colors.isEmpty()) return true
        val avgLuminance = colors.map {
            0.299f * it.red + 0.587f * it.green + 0.114f * it.blue
        }.average().toFloat()
        return avgLuminance < 0.6f
    }
}
