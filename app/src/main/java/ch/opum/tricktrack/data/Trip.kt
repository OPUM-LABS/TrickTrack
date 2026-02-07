package ch.opum.tricktrack.data

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

@Keep
@Entity(tableName = "trips")
data class Trip(
    @SerializedName(value = "id", alternate = ["a"])
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @SerializedName(value = "startLoc", alternate = ["b"])
    val startLoc: String,
    @SerializedName(value = "endLoc", alternate = ["c"])
    val endLoc: String,
    @SerializedName(value = "distance", alternate = ["d"])
    val distance: Double,
    @SerializedName(value = "type", alternate = ["e"])
    val type: String,
    @SerializedName(value = "description", alternate = ["f"])
    val description: String?,
    @SerializedName(value = "date", alternate = ["g"])
    val date: Date,
    @SerializedName(value = "endDate", alternate = ["h"])
    val endDate: Long = date.time,
    @SerializedName(value = "isConfirmed", alternate = ["i"])
    val isConfirmed: Boolean = true,
    @SerializedName(value = "startLat", alternate = ["j"])
    val startLat: Double? = null,
    @SerializedName(value = "startLon", alternate = ["k"])
    val startLon: Double? = null,
    @SerializedName(value = "endLat", alternate = ["l"])
    val endLat: Double? = null,
    @SerializedName(value = "endLon", alternate = ["m"])
    val endLon: Double? = null,
    @SerializedName(value = "isAutomatic", alternate = ["n"])
    @ColumnInfo(defaultValue = "false")
    val isAutomatic: Boolean = false
)
