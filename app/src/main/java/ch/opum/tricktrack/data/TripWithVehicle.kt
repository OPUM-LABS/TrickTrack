package ch.opum.tricktrack.data

import androidx.room.Embedded
import androidx.room.Relation

data class TripWithVehicle(
    @Embedded val trip: Trip,
    @Relation(
        parentColumn = "vehicleId",
        entityColumn = "id"
    )
    val vehicle: VehicleEntity?
)
