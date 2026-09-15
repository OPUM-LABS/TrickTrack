package ch.opum.tricktrack

import androidx.compose.ui.graphics.Color
import ch.opum.tricktrack.ui.theme.adjustColorForDarkTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DarkThemeContrastTest {

    private val darkBackground = Color(0xFF121212) // Grey10
    private val darkSurface = Color(0xFF1E1E1E)    // Grey20

    private fun sRgbToLinear(c: Float): Double {
        return if (c <= 0.04045f) {
            c.toDouble() / 12.92
        } else {
            ((c.toDouble() + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun calculateLuminance(color: Color): Double {
        val r = sRgbToLinear(color.red)
        val g = sRgbToLinear(color.green)
        val b = sRgbToLinear(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrast(c1: Color, c2: Color): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun getHue(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val maxVal = maxOf(r, g, b)
        val minVal = minOf(r, g, b)
        val delta = maxVal - minVal
        if (delta == 0f) return 0f
        return when {
            maxVal == r -> ((g - b) / delta).let { ((it % 6f) + 6f) % 6f } * 60f
            maxVal == g -> (((b - r) / delta) + 2f) * 60f
            else -> (((r - g) / delta) + 4f) * 60f
        }
    }

    @Test
    fun testNightTheme_hasHighContrastAfterAdjustment() {
        val nightColor = Color(0xFF1A2A6C) // Deep navy blue from daytime night set
        val adjusted = adjustColorForDarkTheme(nightColor)

        val contrastAgainstBg = calculateContrast(adjusted, darkBackground)
        val contrastAgainstSurface = calculateContrast(adjusted, darkSurface)

        // WCAG AA requirement is 4.5:1, our target is >= 7:1 (WCAG AAA)
        assertTrue("Contrast against background ($contrastAgainstBg) should be >= 4.5", contrastAgainstBg >= 4.5)
        assertTrue("Contrast against surface ($contrastAgainstSurface) should be >= 4.5", contrastAgainstSurface >= 4.5)
    }

    @Test
    fun testEveningTheme_hasHighContrastAfterAdjustment() {
        val eveningColor = Color(0xFF8A2387) // Deep magenta from daytime evening set
        val adjusted = adjustColorForDarkTheme(eveningColor)

        val contrastAgainstBg = calculateContrast(adjusted, darkBackground)
        val contrastAgainstSurface = calculateContrast(adjusted, darkSurface)

        assertTrue("Contrast against background ($contrastAgainstBg) should be >= 4.5", contrastAgainstBg >= 4.5)
        assertTrue("Contrast against surface ($contrastAgainstSurface) should be >= 4.5", contrastAgainstSurface >= 4.5)
    }

    @Test
    fun testDefaultPurple_hasHighContrastAfterAdjustment() {
        val defaultPurple = Color(0xFF6750A4)
        val adjusted = adjustColorForDarkTheme(defaultPurple)

        val contrastAgainstBg = calculateContrast(adjusted, darkBackground)
        val contrastAgainstSurface = calculateContrast(adjusted, darkSurface)

        assertTrue("Contrast against background ($contrastAgainstBg) should be >= 4.5", contrastAgainstBg >= 4.5)
        assertTrue("Contrast against surface ($contrastAgainstSurface) should be >= 4.5", contrastAgainstSurface >= 4.5)
    }

    @Test
    fun testOceanTheme_preservesHighContrast() {
        val oceanColor = Color(0xFF0099FF)
        val adjusted = adjustColorForDarkTheme(oceanColor)

        val contrastAgainstBg = calculateContrast(adjusted, darkBackground)
        assertTrue("Contrast against background ($contrastAgainstBg) should be >= 4.5", contrastAgainstBg >= 4.5)
    }

    @Test
    fun testHuePreservation() {
        val colors = listOf(
            Color(0xFF1A2A6C), // Blue
            Color(0xFF8A2387), // Magenta
            Color(0xFF6750A4), // Purple
            Color(0xFF0099FF)  // Cyan-blue
        )

        for (color in colors) {
            val originalHue = getHue(color)
            val adjusted = adjustColorForDarkTheme(color)
            val adjustedHue = getHue(adjusted)

            val hueDiff = abs(originalHue - adjustedHue)
            assertTrue("Hue should be preserved for $color (diff: $hueDiff)", hueDiff < 1.0f)

            val l = (maxOf(adjusted.red, adjusted.green, adjusted.blue) + minOf(adjusted.red, adjusted.green, adjusted.blue)) / 2f
            assertTrue("Lightness ($l) should be >= 0.70 for dark theme readability", l >= 0.70f)
        }
    }
}
