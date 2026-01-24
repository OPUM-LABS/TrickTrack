package ch.opum.tricktrack.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startLoc: String,
    val endLoc: String,
    val distance: Double,
    val type: String,
    val description: String?,
    val date: Date,
    val endDate: Long = date.time,
    val isConfirmed: Boolean = true,
    val startLat: Double? = null,
    val startLon: Double? = null,
    val endLat: Double? = null,
    val endLon: Double? = null,
    @ColumnInfo(defaultValue = "false")
    val isAutomatic: Boolean = false
)
