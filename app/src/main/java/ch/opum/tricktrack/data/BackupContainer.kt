package ch.opum.tricktrack.data

import androidx.annotation.Keep
import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.VehicleEntity
import com.google.gson.annotations.SerializedName

@Keep
data class BackupContainer(
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName(value = "trips", alternate = ["a"])
    val trips: List<Trip>? = null,
    @SerializedName(value = "places", alternate = ["b"])
    val places: List<SavedPlace>? = null,
    @SerializedName(value = "drivers", alternate = ["c"])
    val drivers: List<DriverEntity>? = null,
    @SerializedName(value = "companies", alternate = ["d"])
    val companies: List<CompanyEntity>? = null,
    @SerializedName(value = "vehicles", alternate = ["e"])
    val vehicles: List<VehicleEntity>? = null
)
