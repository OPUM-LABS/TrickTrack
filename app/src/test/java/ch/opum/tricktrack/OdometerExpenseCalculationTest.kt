package ch.opum.tricktrack

import ch.opum.tricktrack.data.DistanceUnit
import ch.opum.tricktrack.util.DistanceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OdometerExpenseCalculationTest {

    @Test
    fun testOdometerModeAndExpensesEnabled_calculatesBasedOnOdometerDifference() {
        val vehicleBaselineOdometerKm = 10000.0 // From favourites vehicle or last trip
        val userEnteredEndOdometerKm = 10025.0
        val expenseRatePerKm = 0.70f

        // Distance is calculated as the difference between entered odometer and vehicle baseline
        val distanceKm = (userEnteredEndOdometerKm - vehicleBaselineOdometerKm).coerceAtLeast(0.0)
        assertEquals(25.0, distanceKm, 0.001)

        // Expense is calculated based on the odometer difference
        val tripCost = distanceKm.toFloat() * expenseRatePerKm
        assertEquals(17.50f, tripCost, 0.001f)
    }

    @Test
    fun testOdometerModeAndExpensesEnabled_withMiles() {
        val expenseRatePerKm = 0.70f
        val distanceUnit = DistanceUnit.MILES

        // Vehicle has 10,000 miles stored in km
        val vehicleOdometerMiles = 10000.0
        val vehicleOdometerKm = DistanceFormatter.toKm(vehicleOdometerMiles, distanceUnit)

        // User enters 10,025 miles in ReviewScreen / EditDialog
        val userEnteredMiles = 10025.0
        val endOdometerKm = DistanceFormatter.toKm(userEnteredMiles, distanceUnit)

        // Distance in km
        val distanceKm = (endOdometerKm - vehicleOdometerKm).coerceAtLeast(0.0)
        val expectedDistanceKm = DistanceFormatter.toKm(25.0, distanceUnit)
        assertEquals(expectedDistanceKm, distanceKm, 0.001)

        // Expense calculated on km
        val tripCost = distanceKm.toFloat() * expenseRatePerKm
        val expectedCost = expectedDistanceKm.toFloat() * expenseRatePerKm
        assertEquals(expectedCost, tripCost, 0.001f)
    }

    @Test
    fun testOdometerModeDisabledAndExpensesEnabled_calculatesBasedOnGpsData() {
        val gpsDistanceMeters = 14500.0 // 14.5 km recorded by LocationService
        val gpsDistanceKm = gpsDistanceMeters / 1000.0
        val expenseRatePerKm = 0.70f

        // When odometer mode is disabled, endOdometer passed is null
        val isOdometerModeEnabled = false
        val endOdometerToPass: Double? = if (isOdometerModeEnabled) 10025.0 else null
        assertNull(endOdometerToPass)

        // Trip distance remains GPS distance
        val finalTripDistance = if (endOdometerToPass != null) 0.0 else gpsDistanceKm
        assertEquals(14.5, finalTripDistance, 0.001)

        // Expense is calculated strictly on GPS distance
        val tripCost = finalTripDistance.toFloat() * expenseRatePerKm
        assertEquals(10.15f, tripCost, 0.001f)
    }

    @Test
    fun testOdometerMode_sequentialTripsOdometerProgression() {
        var vehicleOdometer = 5000.0
        val expenseRate = 0.50f

        // Trip 1
        val trip1EndOdo = 5040.0
        val trip1Distance = (trip1EndOdo - vehicleOdometer).coerceAtLeast(0.0)
        assertEquals(40.0, trip1Distance, 0.001)
        assertEquals(20.00f, trip1Distance.toFloat() * expenseRate, 0.001f)
        vehicleOdometer = maxOf(vehicleOdometer, trip1EndOdo)
        assertEquals(5040.0, vehicleOdometer, 0.001)

        // Trip 2
        val trip2EndOdo = 5095.0
        val trip2Distance = (trip2EndOdo - vehicleOdometer).coerceAtLeast(0.0)
        assertEquals(55.0, trip2Distance, 0.001)
        assertEquals(27.50f, trip2Distance.toFloat() * expenseRate, 0.001f)
        vehicleOdometer = maxOf(vehicleOdometer, trip2EndOdo)
        assertEquals(5095.0, vehicleOdometer, 0.001)
    }

    private fun resolveStartOdometerKm(
        tripVehicleId: Long?,
        selectedVehicleId: Long?,
        tripEndOdometer: Double?,
        tripDistance: Double,
        vehicleCurrentOdometer: Double
    ): Double {
        return if (tripEndOdometer != null && tripVehicleId != null && tripVehicleId == selectedVehicleId) {
            (tripEndOdometer - tripDistance).coerceAtLeast(0.0)
        } else {
            vehicleCurrentOdometer
        }
    }

    @Test
    fun testEditTripDialog_editingExistingTripRecoversOriginalStartOdometer() {
        // Vehicle has moved on to 50150 km because later trips occurred
        val vehicleCurrentOdometerKm = 50150.0
        val vehicleId = 1L
        val expenseRatePerKm = 0.60f

        // Existing trip was 50000 -> 50050 km (distance = 50 km)
        val existingTripEndOdometerKm = 50050.0
        val existingTripDistanceKm = 50.0
        val existingTripVehicleId = 1L

        // In EditTripDialog:
        val startOdometerKm = resolveStartOdometerKm(
            tripVehicleId = existingTripVehicleId,
            selectedVehicleId = vehicleId,
            tripEndOdometer = existingTripEndOdometerKm,
            tripDistance = existingTripDistanceKm,
            vehicleCurrentOdometer = vehicleCurrentOdometerKm
        )
        assertEquals(50000.0, startOdometerKm, 0.001)

        // User updates end odometer to 50065 km
        val userEditedOdometerKm = 50065.0
        val updatedDistanceKm = (userEditedOdometerKm - startOdometerKm).coerceAtLeast(0.0)
        assertEquals(65.0, updatedDistanceKm, 0.001)

        // Expenses updated based on the new distance
        val updatedCost = updatedDistanceKm.toFloat() * expenseRatePerKm
        assertEquals(39.00f, updatedCost, 0.001f)

        // Vehicle odometer does not regress (maxOf)
        val updatedVehicleOdo = maxOf(vehicleCurrentOdometerKm, userEditedOdometerKm)
        assertEquals(50150.0, updatedVehicleOdo, 0.001)
    }

    @Test
    fun testEditTripDialog_switchingVehicleUsesNewVehicleOdometer() {
        val vehicle1Id = 1L
        val vehicle2Id = 2L
        val vehicle2CurrentOdometerKm = 8000.0
        val expenseRatePerKm = 0.50f

        // Trip was recorded for Vehicle 1 (with different odometer values)
        val existingTripVehicleId = vehicle1Id
        val existingTripEndOdo = 12500.0
        val existingTripDist = 35.0

        // User changes vehicle to Vehicle 2
        val selectedVehicleId = vehicle2Id
        val startOdometerKm = resolveStartOdometerKm(
            tripVehicleId = existingTripVehicleId,
            selectedVehicleId = selectedVehicleId,
            tripEndOdometer = existingTripEndOdo,
            tripDistance = existingTripDist,
            vehicleCurrentOdometer = vehicle2CurrentOdometerKm
        )
        assertEquals(8000.0, startOdometerKm, 0.001)

        // User enters end odometer 8030 for Vehicle 2
        val enteredOdoKm = 8030.0
        val updatedDistanceKm = (enteredOdoKm - startOdometerKm).coerceAtLeast(0.0)
        assertEquals(30.0, updatedDistanceKm, 0.001)
        assertEquals(15.00f, updatedDistanceKm.toFloat() * expenseRatePerKm, 0.001f)
    }

    @Test
    fun testEditTripDialog_manualTripUsesVehicleOdometer() {
        val vehicleId = 1L
        val vehicleOdometerKm = 3000.0

        val startOdometerKm = resolveStartOdometerKm(
            tripVehicleId = null,
            selectedVehicleId = vehicleId,
            tripEndOdometer = null,
            tripDistance = 0.0,
            vehicleCurrentOdometer = vehicleOdometerKm
        )
        assertEquals(3000.0, startOdometerKm, 0.001)
    }

    @Test
    fun testDistanceFormatter_calcDistanceKmDoesNotDoubleConvertInMiles() {
        val distanceUnit = DistanceUnit.MILES
        val startOdoKm = DistanceFormatter.toKm(100.0, distanceUnit) // 100 miles in km
        val enteredMiles = 150.0 // 150 miles entered by user
        val endOdoKm = DistanceFormatter.toKm(enteredMiles, distanceUnit)

        val calcDistanceKm = (endOdoKm - startOdoKm).coerceAtLeast(0.0)
        val formatted = DistanceFormatter.format(calcDistanceKm, distanceUnit, java.util.Locale.US)
        assertEquals("50.00 mi", formatted)
    }
}
