package ch.opum.tricktrack.util

import ch.opum.tricktrack.data.DistanceUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

object DistanceFormatter {

    private const val KM_TO_MILES = 0.621371
    private const val KM_TO_NAUTICAL_MILES = 0.539957
    private const val METERS_TO_FEET = 3.28084
    private const val KMH_TO_MPH = 0.621371
    private const val KMH_TO_KNOTS = 0.539957

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
        
        val unitSuffix = getUnitSuffix(unit)
        return "${df.format(convertedValue)} $unitSuffix"
    }
    
    /**
     * Formats a distance value (in KM) for display based on the selected unit, with single decimal.
     */
    fun formatShort(km: Double, unit: DistanceUnit, locale: Locale = Locale.getDefault()): String {
        val convertedValue = convert(km, unit)
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val df = DecimalFormat("#,##0.1", symbols)
        
        val unitSuffix = getUnitSuffix(unit)
        return "${df.format(convertedValue)} $unitSuffix"
    }

    /**
     * Returns the string suffix for the unit (e.g. "km", "mi", "NM").
     */
    fun getUnitSuffix(unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.KM -> "km"
            DistanceUnit.MILES -> "mi"
            DistanceUnit.NAUTICAL_MILES -> "NM"
        }
    }

    /**
     * Returns the speed unit suffix (e.g. "km/h", "mph", "knots").
     */
    fun getSpeedUnitSuffix(unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.KM -> "km/h"
            DistanceUnit.MILES -> "mph"
            DistanceUnit.NAUTICAL_MILES -> "knots"
        }
    }

    /**
     * Converts speed from km/h to target unit.
     */
    fun convertSpeed(speedKmh: Double, unit: DistanceUnit): Double {
        return when (unit) {
            DistanceUnit.KM -> speedKmh
            DistanceUnit.MILES -> speedKmh * KMH_TO_MPH
            DistanceUnit.NAUTICAL_MILES -> speedKmh * KMH_TO_KNOTS
        }
    }

    /**
     * Converts speed from target unit back to km/h for storage.
     */
    fun speedToKmh(speed: Double, unit: DistanceUnit): Double {
        return when (unit) {
            DistanceUnit.KM -> speed
            DistanceUnit.MILES -> speed / KMH_TO_MPH
            DistanceUnit.NAUTICAL_MILES -> speed / KMH_TO_KNOTS
        }
    }

    /**
     * Converts meters to display radius value (meters or feet).
     */
    fun convertMetersToDisplayRadius(meters: Int, unit: DistanceUnit): Int {
        return when (unit) {
            DistanceUnit.KM -> meters
            DistanceUnit.MILES, DistanceUnit.NAUTICAL_MILES -> (meters * METERS_TO_FEET).roundToInt()
        }
    }

    /**
     * Converts display radius value (meters or feet) back to meters for storage.
     */
    fun convertDisplayRadiusToMeters(displayValue: Int, unit: DistanceUnit): Int {
        return when (unit) {
            DistanceUnit.KM -> displayValue
            DistanceUnit.MILES, DistanceUnit.NAUTICAL_MILES -> (displayValue / METERS_TO_FEET).roundToInt()
        }
    }
}
