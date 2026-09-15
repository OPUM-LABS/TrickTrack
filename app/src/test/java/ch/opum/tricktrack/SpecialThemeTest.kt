package ch.opum.tricktrack

import androidx.compose.ui.graphics.Color
import ch.opum.tricktrack.ui.theme.SpecialThemeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialThemeTest {

    @Test
    fun testDaytimeColors_returnsCorrectPaletteForTimeRanges() {
        // Morning (5..8)
        val morningColors = SpecialThemeHelper.getDaytimeColors(7)
        assertEquals(3, morningColors.size)
        assertEquals(Color(0xFFFF7E5F), morningColors[0])

        // Midday (9..13)
        val middayColors = SpecialThemeHelper.getDaytimeColors(12)
        assertEquals(3, middayColors.size)
        assertEquals(Color(0xFF00A8E8), middayColors[0])

        // Afternoon (14..17)
        val afternoonColors = SpecialThemeHelper.getDaytimeColors(16)
        assertEquals(3, afternoonColors.size)
        assertEquals(Color(0xFFF37335), afternoonColors[0])

        // Evening (18..21)
        val eveningColors = SpecialThemeHelper.getDaytimeColors(20)
        assertEquals(3, eveningColors.size)
        assertEquals(Color(0xFF8A2387), eveningColors[0])

        // Night (22..4)
        val lateNightColors = SpecialThemeHelper.getDaytimeColors(23)
        assertEquals(3, lateNightColors.size)
        assertEquals(Color(0xFF1A2A6C), lateNightColors[0])

        val earlyNightColors = SpecialThemeHelper.getDaytimeColors(3)
        assertEquals(3, earlyNightColors.size)
        assertEquals(Color(0xFF1A2A6C), earlyNightColors[0])
    }

    @Test
    fun testGetHeaderGradient_returnsNullForNone_andColorsForSpecialThemes() {
        assertNull(SpecialThemeHelper.getHeaderGradient(SpecialThemeHelper.THEME_NONE))

        val daytimeGradient = SpecialThemeHelper.getHeaderGradient(SpecialThemeHelper.THEME_DAYTIME, 10)
        assertNotNull(daytimeGradient)
        assertEquals(3, daytimeGradient?.size)

        val sunsetGradient = SpecialThemeHelper.getHeaderGradient(SpecialThemeHelper.THEME_SUNSET)
        assertNotNull(sunsetGradient)
        assertEquals(3, sunsetGradient?.size)

        val oceanGradient = SpecialThemeHelper.getHeaderGradient(SpecialThemeHelper.THEME_OCEAN)
        assertNotNull(oceanGradient)
        assertEquals(2, oceanGradient?.size)

        val auroraGradient = SpecialThemeHelper.getHeaderGradient(SpecialThemeHelper.THEME_AURORA)
        assertNotNull(auroraGradient)
        assertEquals(2, auroraGradient?.size)
    }

    @Test
    fun testIsGradientDark_evaluatesLuminanceCorrectly() {
        // Night colors should be dark
        val nightColors = SpecialThemeHelper.getDaytimeColors(23)
        assertTrue(SpecialThemeHelper.isGradientDark(nightColors))

        // Pure white should not be dark
        val brightColors = listOf(Color.White, Color.Yellow)
        assertFalse(SpecialThemeHelper.isGradientDark(brightColors))
    }

    @Test
    fun testGetPrimaryColor_adaptsToSpecialThemeAndDaytime() {
        val defaultColorLong = 0xFF6750A4L

        // None falls back to default
        val nonePrimary = SpecialThemeHelper.getPrimaryColor(SpecialThemeHelper.THEME_NONE, defaultColorLong)
        assertEquals(Color(0xFF6750A4L), nonePrimary)

        // Daytime uses first color of daytime period
        val morningPrimary = SpecialThemeHelper.getPrimaryColor(SpecialThemeHelper.THEME_DAYTIME, defaultColorLong, 7)
        assertEquals(Color(0xFFFF7E5F), morningPrimary)

        // Sunset uses sunset primary
        val sunsetPrimary = SpecialThemeHelper.getPrimaryColor(SpecialThemeHelper.THEME_SUNSET, defaultColorLong)
        assertEquals(Color(0xFFE94057), sunsetPrimary)
    }
}
