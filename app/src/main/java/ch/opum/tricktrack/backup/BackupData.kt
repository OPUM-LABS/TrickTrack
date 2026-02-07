package ch.opum.tricktrack.backup

import androidx.annotation.Keep
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.place.SavedPlace
import com.google.gson.annotations.SerializedName

@Keep
data class BackupData(
    @SerializedName("metadata")
    val metadata: BackupMetadata,
    @SerializedName("trips")
    val trips: List<Trip>,
    @SerializedName("settings")
    val settings: Map<String, String>,
    @SerializedName("places")
    val places: List<SavedPlace>
)

@Keep
data class BackupMetadata(
    @SerializedName("appName")
    val appName: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("version")
    val version: Int
)
