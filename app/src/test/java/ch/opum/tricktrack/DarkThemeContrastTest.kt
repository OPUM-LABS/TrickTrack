package ch.opum.tricktrack

import androidx.compose.ui.graphics.Color
import ch.opum.tricktrack.ui.theme.adjustColorForDarkTheme
import ch.opum.tricktrack.ui.theme.adjustColorForLightTheme
import ch.opum.tricktrack.ui.theme.calculateContrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DarkThemeContrastTest {

    private val darkBackground = Color(0xFF121212) // Grey10
    private val darkSurface = Color(0xFF1E1E1E)    // Grey20
    private val lightBackground = Color(0xFFFFFFFF) // White

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
    fun testDarkTheme_huePreservation() {
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

            val contrast = calculateContrast(adjusted, darkBackground)
            assertTrue("Contrast for $color against dark background should be >= 4.5 (got: $contrast)", contrast >= 4.5)
        }
    }

    @Test
    fun testLightTheme_brightColorsAreAdjustedForContrast() {
        // Bright lime green (from user screenshot)
        val limeGreen = Color(0xFF39FF14)
        val adjustedLime = adjustColorForLightTheme(limeGreen)
        val limeContrast = calculateContrast(adjustedLime, lightBackground)
        assertTrue("Adjusted lime green contrast ($limeContrast) should be >= 4.5 on white", limeContrast >= 4.5)

        // Pure yellow
        val yellow = Color(0xFFFFFF00)
        val adjustedYellow = adjustColorForLightTheme(yellow)
        val yellowContrast = calculateContrast(adjustedYellow, lightBackground)
        assertTrue("Adjusted yellow contrast ($yellowContrast) should be >= 4.5 on white", yellowContrast >= 4.5)

        // Pure cyan
        val cyan = Color(0xFF00FFFF)
        val adjustedCyan = adjustColorForLightTheme(cyan)
        val cyanContrast = calculateContrast(adjustedCyan, lightBackground)
        assertTrue("Adjusted cyan contrast ($cyanContrast) should be >= 4.5 on white", cyanContrast >= 4.5)
    }

    @Test
    fun testLightTheme_alreadyDarkColorsAreUnchanged() {
        val defaultPurple = Color(0xFF6750A4)
        val adjustedPurple = adjustColorForLightTheme(defaultPurple)
        assertEquals(defaultPurple, adjustedPurple)

        val navyBlue = Color(0xFF1A2A6C)
        val adjustedNavy = adjustColorForLightTheme(navyBlue)
        assertEquals(navyBlue, adjustedNavy)
    }

    @Test
    fun testLightTheme_huePreservation() {
        val brightColors = listOf(
            Color(0xFF39FF14), // Lime Green
            Color(0xFFFFFF00), // Yellow
            Color(0xFF00FFFF), // Cyan
            Color(0xFFFF80DF)  // Pink
        )

        for (color in brightColors) {
            val originalHue = getHue(color)
            val adjusted = adjustColorForLightTheme(color)
            val adjustedHue = getHue(adjusted)

            val hueDiff = abs(originalHue - adjustedHue)
            assertTrue("Hue should be preserved for $color (diff: $hueDiff)", hueDiff < 1.0f)
            val contrast = calculateContrast(adjusted, lightBackground)
            assertTrue("Contrast for $color should be >= 4.5 (got: $contrast)", contrast >= 4.5)
        }
    }
}
