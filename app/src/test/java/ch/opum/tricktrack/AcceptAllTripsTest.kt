package ch.opum.tricktrack

import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.TripWithVehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class AcceptAllTripsTest {

    @Test
    fun testBatchApproval_confirmsAllTripsAndAppliesDefaultType() {
        val trip1 = Trip(
            id = 1L,
            startLoc = "Home",
            endLoc = "Client A",
            distance = 15.0,
            type = "",
            description = null,
            date = Date(1000L),
            isConfirmed = false
        )
        val trip2 = Trip(
            id = 2L,
            startLoc = "Client A",
            endLoc = "Client B",
            distance = 25.0,
            type = "Personal",
            description = null,
            date = Date(2000L),
            isConfirmed = false
        )

        val unconfirmed = listOf(trip1, trip2)
        val defaultIsBusiness = true

        val approved = unconfirmed.map { trip ->
            val finalType = if (trip.type.isNotBlank()) trip.type else (if (defaultIsBusiness) "Business" else "Personal")
            trip.copy(
                type = finalType,
                isConfirmed = true,
                gpsDistance = trip.gpsDistance ?: trip.distance
            )
        }

        assertTrue("All trips should now be confirmed", approved.all { it.isConfirmed })
        assertEquals("Business", approved[0].type)
        assertEquals("Personal", approved[1].type)
    }

    @Test
    fun testBatchApproval_chainsOdometerChronologically() {
        var vehicleOdo = 50000.0

        val trip1 = Trip(
            id = 1L,
            startLoc = "Zurich",
            endLoc = "Winterthur",
            distance = 25.0,
            type = "Business",
            description = null,
            date = Date(1000L),
            isConfirmed = false,
            vehicleId = 1
        )
        val trip2 = Trip(
            id = 2L,
            startLoc = "Winterthur",
            endLoc = "St. Gallen",
            distance = 55.0,
            type = "Business",
            description = null,
            date = Date(2000L),
            isConfirmed = false,
            vehicleId = 1
        )

        val trips = listOf(trip2, trip1).sortedBy { it.date.time } // out of order input, sorted chronologically

        val approved = mutableListOf<Trip>()
        for (trip in trips) {
            val startOdo = vehicleOdo
            val endOdo = startOdo + trip.distance
            vehicleOdo = endOdo
            approved.add(
                trip.copy(
                    isConfirmed = true,
                    startOdometer = startOdo,
                    endOdometer = endOdo
                )
            )
        }

        assertEquals(2, approved.size)
        // Trip 1 (first chronologically)
        assertEquals(50000.0, approved[0].startOdometer!!, 0.001)
        assertEquals(50025.0, approved[0].endOdometer!!, 0.001)
        // Trip 2 (second chronologically)
        assertEquals(50025.0, approved[1].startOdometer!!, 0.001)
        assertEquals(50080.0, approved[1].endOdometer!!, 0.001)
        // Vehicle odometer end state
        assertEquals(50080.0, vehicleOdo, 0.001)
    }

    @Test
    fun testBatchApproval_excludesPendingDiscards() {
        val trip1 = Trip(id = 1L, startLoc = "A", endLoc = "B", distance = 10.0, type = "Business", description = null, date = Date(), isConfirmed = false)
        val trip2 = Trip(id = 2L, startLoc = "B", endLoc = "C", distance = 20.0, type = "Business", description = null, date = Date(), isConfirmed = false)
        val trip3 = Trip(id = 3L, startLoc = "C", endLoc = "D", distance = 30.0, type = "Business", description = null, date = Date(), isConfirmed = false)

        val allUnconfirmed = listOf(
            TripWithVehicle(trip1, null),
            TripWithVehicle(trip2, null),
            TripWithVehicle(trip3, null)
        )
        val pendingDiscarded = listOf(TripWithVehicle(trip2, null))
        val pendingDiscardIds = pendingDiscarded.map { it.trip.id }.toSet()

        val toApprove = allUnconfirmed.filter { it.trip.id !in pendingDiscardIds }

        assertEquals(2, toApprove.size)
        assertEquals(listOf(1L, 3L), toApprove.map { it.trip.id })
    }
}
