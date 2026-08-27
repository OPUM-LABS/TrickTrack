package ch.opum.tricktrack.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @SerializedName(value = "id", alternate = ["a"])
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @SerializedName(value = "licensePlate", alternate = ["b"])
    val licensePlate: String,
    @SerializedName(value = "carModel", alternate = ["c"])
    val carModel: String?,
    @SerializedName(value = "brand", alternate = ["d"])
    val brand: String? = null,
    @SerializedName(value = "currentOdometer", alternate = ["f"])
    val currentOdometer: Double = 0.0
)
