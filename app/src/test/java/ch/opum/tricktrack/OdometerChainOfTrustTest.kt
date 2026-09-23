package ch.opum.tricktrack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.round

class OdometerChainOfTrustTest {

    private fun cascadeEndOdometer(
        isChained: Boolean,
        previousTripNewEnd: Double,
        subTripStart: Double,
        subTripDistance: Double,
        subTripCurrentEnd: Double
    ): Double {
        return if (isChained || previousTripNewEnd > subTripStart) {
            round((previousTripNewEnd + subTripDistance) * 10000.0) / 10000.0
        } else {
            subTripCurrentEnd
        }
    }

    @Test
    fun testSameChain_cascadesProgressionAndPreservesDistance() {
        // Trip 1 (2 Sep): 1000 km -> 1003 km (Distance: 3 km)
        val trip1End = 1003.0

        // Trip 2 (17 Sep): 1003 km -> 1010 km (Distance: 7 km) - chained to Trip 1
        val trip2End = 1010.0
        val trip2Distance = 7.0
        val trip2Start = trip2End - trip2Distance

        // Chained check:
        val isChained = abs(trip2Start - trip1End) < 0.05
        assertTrue("Trip 2 should be in the same chain as Trip 1", isChained)

        // User edits Trip 1's end odometer: 1003 km -> 1005 km
        val newTrip1End = 1005.0
        val newTrip2Start = newTrip1End
        val newTrip2End = cascadeEndOdometer(isChained, newTrip1End, trip2Start, trip2Distance, trip2End)

        assertEquals(1005.0, newTrip2Start, 0.001)
        assertEquals(1012.0, newTrip2End, 0.001)
        assertEquals(7.0, newTrip2End - newTrip2Start, 0.001)
    }

    @Test
    fun testGapTrip_preservesNewChainOfTrustWhenEarlierTripEdited() {
        // Trip 1 (2 Sep): 1000 km -> 1003 km (Distance: 3 km)
        val oldTrip1End = 1003.0

        // Gap: 47 km of unlogged private driving
        // Trip 2 (17 Sep): manual start = 1050 km, end = 1060 km (Distance: 10 km)
        val trip2End = 1060.0
        val trip2Distance = 10.0
        val trip2Start = trip2End - trip2Distance

        // Chained check:
        val isChained = abs(trip2Start - oldTrip1End) < 0.05
        assertFalse("Trip 2 has a gap and starts a new chain of trust", isChained)

        // User edits Trip 1's end: 1003 km -> 1005 km
        val newTrip1End = 1005.0

        val finalTrip2End = cascadeEndOdometer(isChained, newTrip1End, trip2Start, trip2Distance, trip2End)

        assertEquals(1060.0, finalTrip2End, 0.001)
        assertEquals(1050.0, finalTrip2End - trip2Distance, 0.001)

        // The gap between Trip 1 and Trip 2 has shrunk from 47 km to 45 km:
        val remainingGap = trip2Start - newTrip1End
        assertEquals(45.0, remainingGap, 0.001)
    }

    @Test
    fun testEarlierTripPushesForward_whenExceedingManualStartOdometer() {
        val oldTrip1End = 1003.0
        val trip2Distance = 10.0
        val trip2Start = 1050.0
        val trip2End = 1060.0

        // User edits Trip 1's end to 1055 km (which exceeds Trip 2's start of 1050 km)
        val newTrip1End = 1055.0

        val isChained = abs(trip2Start - oldTrip1End) < 0.05
        assertFalse("Trip 2 initially has a gap", isChained)

        val finalTrip2End = cascadeEndOdometer(isChained, newTrip1End, trip2Start, trip2Distance, trip2End)

        assertEquals(1065.0, finalTrip2End, 0.001)
        assertEquals(1055.0, finalTrip2End - trip2Distance, 0.001)
        assertEquals(10.0, finalTrip2End - (finalTrip2End - trip2Distance), 0.001)
    }

    @Test
    fun testClearField_revertsToDefaultChainOfTrustAndCalculatesEndAccordingly() {
        // Previous trip ended at 1003 km
        val previousTripEndKm = 1003.0

        // Current trip had a manual gap start = 1050 km, end = 1060 km (Distance: 10 km)
        val tripDistance = 10.0
        val initialStartKm = 1050.0
        val initialEndKm = 1060.0
        assertEquals(10.0, initialEndKm - initialStartKm, 0.001)

        // User hits "Clear" on the Start Odometer field in EditTripDialog:
        // 1. Start reverts to previous trip's end odometer
        val currentStartKm = previousTripEndKm

        // 2. End odometer is calculated accordingly based on trip distance
        val currentEndKm = round((currentStartKm + tripDistance) * 10000.0) / 10000.0

        assertEquals(1003.0, currentStartKm, 0.001)
        assertEquals(1013.0, currentEndKm, 0.001)
        assertEquals(10.0, currentEndKm - currentStartKm, 0.001)

        // Trip is now chained to the previous trip:
        val isNowChained = abs(currentStartKm - previousTripEndKm) < 0.05
        assertTrue("Trip is now part of the default chain of trust", isNowChained)
    }

    @Test
    fun testDecoupledGpsDistance_switchingModesDoesNotOverwriteEither() {
        // Initial trip created via GPS tracking: 83.0 km
        val recordedTrip = ch.opum.tricktrack.data.Trip(
            id = 1L,
            startLoc = "Start",
            endLoc = "End",
            distance = 83.0,
            gpsDistance = 83.0,
            type = "Business",
            description = null,
            date = java.util.Date(),
            vehicleId = 1
        )

        // In normal mode, displays 83.0 km:
        assertEquals(83.0, recordedTrip.getEffectiveDistance(isOdometerMode = false), 0.001)

        // User enters Odometer Mode and enters odometer values: 1000 km to 1003 km (3.0 km)
        val odoEditedTrip = recordedTrip.copy(
            startOdometer = 1000.0,
            endOdometer = 1003.0,
            distance = 3.0,
            gpsDistance = recordedTrip.gpsDistance ?: recordedTrip.distance
        )

        // In Odometer Mode, displays odometer distance (3.0 km):
        assertEquals(3.0, odoEditedTrip.getEffectiveDistance(isOdometerMode = true), 0.001)

        // User switches back to Normal Mode: displays original GPS distance (83.0 km), NOT 3.0 km!
        assertEquals(83.0, odoEditedTrip.getEffectiveDistance(isOdometerMode = false), 0.001)

        // User edits trip in Normal Mode to 85.0 km:
        val normalEditedTrip = odoEditedTrip.copy(
            distance = 85.0,
            gpsDistance = 85.0
            // startOdometer and endOdometer remain intact
        )

        // In Normal Mode, displays 85.0 km:
        assertEquals(85.0, normalEditedTrip.getEffectiveDistance(isOdometerMode = false), 0.001)

        // User switches back to Odometer Mode: odometer values and 3.0 km distance are preserved!
        assertEquals(3.0, normalEditedTrip.getEffectiveDistance(isOdometerMode = true), 0.001)
        assertEquals(1000.0, normalEditedTrip.startOdometer)
        assertEquals(1003.0, normalEditedTrip.endOdometer)
    }

    @Test
    fun testLegacyTripBackfill_compatibilityWithPriorVersions() {
        // Trip from prior version before startOdometer and gpsDistance columns existed
        val legacyTrip = ch.opum.tricktrack.data.Trip(
            id = 2L,
            startLoc = "Origin",
            endLoc = "Dest",
            distance = 7.0,
            type = "Personal",
            description = null,
            date = java.util.Date(),
            vehicleId = 1,
            endOdometer = 1010.0,
            startOdometer = null,
            gpsDistance = null
        )

        // In normal mode, falls back to legacy distance:
        assertEquals(7.0, legacyTrip.getEffectiveDistance(isOdometerMode = false), 0.001)

        // Simulated Room Migration 14 -> 15 backfill:
        val migratedTrip = legacyTrip.copy(
            gpsDistance = legacyTrip.distance,
            startOdometer = legacyTrip.endOdometer?.let { (it - legacyTrip.distance).coerceAtLeast(0.0) }
        )

        assertEquals(1003.0, migratedTrip.startOdometer)
        assertEquals(1010.0, migratedTrip.endOdometer)
        assertEquals(7.0, migratedTrip.gpsDistance)
        assertEquals(7.0, migratedTrip.getEffectiveDistance(isOdometerMode = true), 0.001)
        assertEquals(7.0, migratedTrip.getEffectiveDistance(isOdometerMode = false), 0.001)
    }
}

