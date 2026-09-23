package ch.opum.tricktrack.data

import android.content.Context
import androidx.annotation.Keep
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

@Keep
data class ActiveTripCheckpointData(
    @SerializedName("startTime") val startTime: Long,
    @SerializedName("startLat") val startLat: Double?,
    @SerializedName("startLon") val startLon: Double?,
    @SerializedName("startLoc") val startLoc: String,
    @SerializedName("lastLat") val lastLat: Double?,
    @SerializedName("lastLon") val lastLon: Double?,
    @SerializedName("lastLoc") val lastLoc: String,
    @SerializedName("distanceMeters") val distanceMeters: Float,
    @SerializedName("routePolyline") val routePolyline: String?,
    @SerializedName("trigger") val trigger: String?,
    @SerializedName("vehicleId") val vehicleId: Int?,
    @SerializedName("type") val type: String,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis()
)

object ActiveTripCheckpointManager {
    private const val PREFS_NAME = "active_trip_checkpoint"
    private const val KEY_CHECKPOINT = "checkpoint_data"
    private val gson = Gson()

    fun saveCheckpoint(context: Context, data: ActiveTripCheckpointData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(data)
        prefs.edit { putString(KEY_CHECKPOINT, json) }
    }

    fun getCheckpoint(context: Context): ActiveTripCheckpointData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CHECKPOINT, null) ?: return null
        return try {
            gson.fromJson(json, ActiveTripCheckpointData::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun clearCheckpoint(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_CHECKPOINT) }
    }
}
