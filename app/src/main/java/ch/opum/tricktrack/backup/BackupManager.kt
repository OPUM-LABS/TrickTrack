package ch.opum.tricktrack.backup

import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.place.SavedPlace
import com.google.gson.Gson

class BackupManager {

    private val gson = Gson()

    fun createBackupJson(trips: List<Trip>, settings: Map<String, String>, places: List<SavedPlace>): String {
        val metadata = BackupMetadata(
            appName = "TrickTrack",
            timestamp = System.currentTimeMillis(),
            version = 1
        )
        val backupData = BackupData(
            metadata = metadata,
            trips = trips,
            settings = settings,
            places = places
        )
        return gson.toJson(backupData)
    }
}