package ch.opum.tricktrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)
