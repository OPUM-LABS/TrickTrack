package ch.opum.tricktrack.backup

import ch.opum.tricktrack.data.Trip
import com.google.gson.Gson

class BackupManager {

    private val gson = Gson()

    fun createBackupJson(trips: List<Trip>, settings: Map<String, String>): String {
        val metadata = BackupMetadata(
            timestamp = System.currentTimeMillis(),
            version = 1
        )
        val backupData = BackupData(
            metadata = metadata,
            trips = trips,
            settings = settings
        )
        return gson.toJson(backupData)
    }

    fun restoreBackupFromJson(json: String): BackupData? {
        return try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            // Handle parsing errors, maybe log them
            null
        }
    }
}