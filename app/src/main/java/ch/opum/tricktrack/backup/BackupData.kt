package ch.opum.tricktrack.backup

import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.place.SavedPlace

data class BackupData(
    val metadata: BackupMetadata,
    val trips: List<Trip>,
    val settings: Map<String, String>,
    val places: List<SavedPlace>
)

data class BackupMetadata(
    val appName: String,
    val timestamp: Long,
    val version: Int
)