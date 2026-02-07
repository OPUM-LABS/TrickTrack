package ch.opum.tricktrack.data.place

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "saved_places")
data class SavedPlace(
    @SerializedName(value = "id", alternate = ["a"])
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @SerializedName(value = "name", alternate = ["b"])
    val name: String,
    @SerializedName(value = "address", alternate = ["c"])
    val address: String,
    @SerializedName(value = "latitude", alternate = ["d"])
    val latitude: Double,
    @SerializedName(value = "longitude", alternate = ["e"])
    val longitude: Double
)
