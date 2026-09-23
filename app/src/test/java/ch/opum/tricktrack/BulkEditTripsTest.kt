package ch.opum.tricktrack

import ch.opum.tricktrack.data.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class BulkEditTripsTest {

    private fun applyBulkEdit(
        trips: List<Trip>,
        targetType: String? = null,
        updateType: Boolean = false,
        targetVehicleId: Int? = null,
        updateVehicle: Boolean = false,
        targetDescription: String? = null,
        updateDescription: Boolean = false
    ): List<Trip> {
        return trips.map { trip ->
            var updated = trip
            if (updateType && targetType != null) {
                updated = updated.copy(type = targetType)
            }
            if (updateVehicle) {
                updated = updated.copy(vehicleId = targetVehicleId)
            }
            if (updateDescription) {
                updated = updated.copy(description = targetDescription?.takeIf { it.isNotBlank() })
            }
            updated
        }
    }

    private fun sampleTrip(id: Long, type: String, vehicleId: Int?, description: String?): Trip {
        return Trip(
            id = id,
            startLoc = "Start",
            endLoc = "End",
            distance = 10.0,
            type = type,
            vehicleId = vehicleId,
            description = description,
            date = Date()
        )
    }

    @Test
    fun testUpdateVehicleOnly_preservesTypesAndDescriptions() {
        val trips = listOf(
            sampleTrip(1L, "Business", 1, "Meeting with client A"),
            sampleTrip(2L, "Personal", 2, "Grocery shopping"),
            sampleTrip(3L, "Business", null, "Airport run")
        )

        // User updates vehicle only to vehicle 3
        val result = applyBulkEdit(
            trips = trips,
            targetVehicleId = 3,
            updateVehicle = true
        )

        // All trips should now have vehicleId = 3
        assertEquals(3, result[0].vehicleId)
        assertEquals(3, result[1].vehicleId)
        assertEquals(3, result[2].vehicleId)

        // Types and descriptions should remain completely untouched
        assertEquals("Business", result[0].type)
        assertEquals("Personal", result[1].type)
        assertEquals("Business", result[2].type)

        assertEquals("Meeting with client A", result[0].description)
        assertEquals("Grocery shopping", result[1].description)
        assertEquals("Airport run", result[2].description)
    }

    @Test
    fun testUpdateTypeOnly_preservesVehiclesAndDescriptions() {
        val trips = listOf(
            sampleTrip(1L, "Personal", 1, "Trip 1"),
            sampleTrip(2L, "Personal", 2, "Trip 2")
        )

        // User updates type only to "Business"
        val businessResult = applyBulkEdit(
            trips = trips,
            targetType = "Business",
            updateType = true
        )

        assertEquals("Business", businessResult[0].type)
        assertEquals("Business", businessResult[1].type)

        assertEquals(1, businessResult[0].vehicleId)
        assertEquals(2, businessResult[1].vehicleId)

        assertEquals("Trip 1", businessResult[0].description)
        assertEquals("Trip 2", businessResult[1].description)

        // User updates type to "Personal"
        val personalResult = applyBulkEdit(
            trips = businessResult,
            targetType = "Personal",
            updateType = true
        )

        assertEquals("Personal", personalResult[0].type)
        assertEquals("Personal", personalResult[1].type)
    }

    @Test
    fun testUpdateDescriptionOnly_preservesTypesAndVehicles() {
        val trips = listOf(
            sampleTrip(1L, "Business", 1, "Old note 1"),
            sampleTrip(2L, "Personal", 2, "Old note 2")
        )

        val result = applyBulkEdit(
            trips = trips,
            targetDescription = "New unified note",
            updateDescription = true
        )

        assertEquals("New unified note", result[0].description)
        assertEquals("New unified note", result[1].description)

        assertEquals("Business", result[0].type)
        assertEquals("Personal", result[1].type)

        assertEquals(1, result[0].vehicleId)
        assertEquals(2, result[1].vehicleId)
    }

    @Test
    fun testClearVehicleAndClearDescription() {
        val trips = listOf(
            sampleTrip(1L, "Business", 1, "Some note")
        )

        val result = applyBulkEdit(
            trips = trips,
            targetVehicleId = null,
            updateVehicle = true, // updateVehicle = true with null clears the vehicle
            targetDescription = "",
            updateDescription = true // updateDescription = true with blank clears the description
        )

        assertNull(result[0].vehicleId)
        assertNull(result[0].description)
        assertEquals("Business", result[0].type)
    }
}
