package ch.opum.tricktrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val licensePlate: String,
    val carModel: String?
)
