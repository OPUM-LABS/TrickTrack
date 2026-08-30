package ch.opum.tricktrack.util

import ch.opum.tricktrack.data.DistanceUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object DistanceFormatter {

    private const val KM_TO_MILES = 0.621371
    private const val KM_TO_NAUTICAL_MILES = 0.539957

    /**
     * Converts distance from Kilometers to the target unit.
     */
    fun convert(km: Double, targetUnit: DistanceUnit): Double {
        return when (targetUnit) {
            DistanceUnit.KM -> km
            DistanceUnit.MILES -> km * KM_TO_MILES
            DistanceUnit.NAUTICAL_MILES -> km * KM_TO_NAUTICAL_MILES
        }
    }

    /**
     * Converts distance from the current unit back to Kilometers for storage.
     */
    fun toKm(value: Double, currentUnit: DistanceUnit): Double {
        return when (currentUnit) {
            DistanceUnit.KM -> value
            DistanceUnit.MILES -> value / KM_TO_MILES
            DistanceUnit.NAUTICAL_MILES -> value / KM_TO_NAUTICAL_MILES
        }
    }

    /**
     * Formats a distance value (in KM) for display based on the selected unit.
     * Includes localized decimal formatting and unit suffix.
     */
    fun format(km: Double, unit: DistanceUnit, locale: Locale = Locale.getDefault()): String {
        val convertedValue = convert(km, unit)
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val df = DecimalFormat("#,##0.00", symbols)
        
        val unitSuffix = when (unit) {
            DistanceUnit.KM -> "km"
            DistanceUnit.MILES -> "mi"
            DistanceUnit.NAUTICAL_MILES -> "NM"
        }
        
        return "${df.format(convertedValue)} $unitSuffix"
    }
    
    /**
     * Formats a distance value (in KM) for display based on the selected unit, with single decimal.
     */
    fun formatShort(km: Double, unit: DistanceUnit, locale: Locale = Locale.getDefault()): String {
        val convertedValue = convert(km, unit)
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val df = DecimalFormat("#,##0.1", symbols)
        
        val unitSuffix = when (unit) {
            DistanceUnit.KM -> "km"
            DistanceUnit.MILES -> "mi"
            DistanceUnit.NAUTICAL_MILES -> "NM"
        }
        
        return "${df.format(convertedValue)} $unitSuffix"
    }

    /**
     * Returns the string suffix for the unit.
     */
    fun getUnitSuffix(unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.KM -> "km"
            DistanceUnit.MILES -> "mi"
            DistanceUnit.NAUTICAL_MILES -> "NM"
        }
    }
}
