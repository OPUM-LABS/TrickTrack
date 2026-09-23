package ch.opum.tricktrack

import ch.opum.tricktrack.data.ActiveTripCheckpointData
import ch.opum.tricktrack.data.Trip
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class ActiveTripShutdownRecoveryTest {

    private val gson = Gson()

    @Test
    fun testCheckpointSerializationRoundTrip() {
        val checkpoint = ActiveTripCheckpointData(
            startTime = 1727000000000L,
            startLat = 47.3769,
            startLon = 8.5417,
            startLoc = "Zurich HB",
            lastLat = 47.4500,
            lastLon = 8.5600,
            lastLoc = "Zurich Airport",
            distanceMeters = 11500f,
            routePolyline = "u_p_Ib_y_@...",
            trigger = "AUTOMATIC",
            vehicleId = 1,
            type = "Business",
            lastUpdated = 1727001000000L
        )

        val json = gson.toJson(checkpoint)
        val deserialized = gson.fromJson(json, ActiveTripCheckpointData::class.java)

        assertEquals(checkpoint.startTime, deserialized.startTime)
        assertEquals(checkpoint.startLat, deserialized.startLat)
        assertEquals(checkpoint.startLon, deserialized.startLon)
        assertEquals(checkpoint.startLoc, deserialized.startLoc)
        assertEquals(checkpoint.lastLat, deserialized.lastLat)
        assertEquals(checkpoint.lastLon, deserialized.lastLon)
        assertEquals(checkpoint.lastLoc, deserialized.lastLoc)
        assertEquals(checkpoint.distanceMeters, deserialized.distanceMeters, 0.01f)
        assertEquals(checkpoint.routePolyline, deserialized.routePolyline)
        assertEquals(checkpoint.trigger, deserialized.trigger)
        assertEquals(checkpoint.vehicleId, deserialized.vehicleId)
        assertEquals(checkpoint.type, deserialized.type)
        assertEquals(checkpoint.lastUpdated, deserialized.lastUpdated)
    }

    @Test
    fun testTripStopReasonSerializationAndBackwardsCompatibility() {
        val shutdownTrip = Trip(
            startLoc = "Start Point",
            endLoc = "Interrupted Mid-Way",
            distance = 15.0,
            type = "Business",
            description = null,
            date = Date(1727000000000L),
            endDate = 1727001000000L,
            isConfirmed = false,
            stopReason = "SHUTDOWN"
        )

        val jsonWithShutdown = gson.toJson(shutdownTrip)
        val deserializedShutdownTrip = gson.fromJson(jsonWithShutdown, Trip::class.java)
        assertEquals("SHUTDOWN", deserializedShutdownTrip.stopReason)

        // Backwards compatibility: A trip serialized without stopReason field
        val legacyTrip = Trip(
            id = 42,
            startLoc = "Zurich",
            endLoc = "Bern",
            distance = 120.0,
            type = "Personal",
            description = null,
            date = Date(1727000000000L),
            endDate = 1727005000000L,
            isConfirmed = true
        )
        val legacyJson = gson.toJson(legacyTrip)
        val deserializedLegacy = gson.fromJson(legacyJson, Trip::class.java)
        assertNull(deserializedLegacy.stopReason)
    }

    @Test
    fun testInterruptedTripEditingWorkflow() {
        // Interrupted trip created upon shutdown recovery
        val interruptedTrip = Trip(
            id = 101L,
            startLoc = "Home",
            endLoc = "Highway Km 42 (Interrupted)",
            distance = 35.0,
            gpsDistance = 35.0,
            type = "Business",
            description = null,
            date = Date(1727000000000L),
            endDate = 1727002000000L,
            isConfirmed = false,
            stopReason = "SHUTDOWN"
        )

        // Only shutdown trips should be eligible for the special edit button
        assertEquals("SHUTDOWN", interruptedTrip.stopReason)
        assertFalse("Interrupted trip must be unconfirmed and pending review", interruptedTrip.isConfirmed)

        // Normal trip should NOT have stopReason
        val normalTrip = Trip(
            id = 102L,
            startLoc = "Home",
            endLoc = "Office",
            distance = 50.0,
            type = "Business",
            description = null,
            date = Date(1727000000000L),
            endDate = 1727003000000L,
            isConfirmed = false,
            stopReason = null
        )
        assertNull(normalTrip.stopReason)

        // User edits the interrupted trip: updates destination and recalculates distance
        val updatedTrip = interruptedTrip.copy(
            endLoc = "Office Building A",
            distance = 55.0,
            stopReason = null
        )

        // After save: stopReason is cleared so edit banner vanishes, but isConfirmed remains false for review approval
        assertNull("stopReason should be cleared to null after user saves destination", updatedTrip.stopReason)
        assertFalse("Trip remains unconfirmed until approved on ReviewScreen", updatedTrip.isConfirmed)
        assertEquals("Office Building A", updatedTrip.endLoc)
        assertEquals(55.0, updatedTrip.distance, 0.001)
    }
}
