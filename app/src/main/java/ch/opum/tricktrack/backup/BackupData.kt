package ch.opum.tricktrack.backup

import ch.opum.tricktrack.data.Trip

data class BackupData(
    val metadata: BackupMetadata,
    val trips: List<Trip>,
    val settings: Map<String, String>
)

data class BackupMetadata(
    val timestamp: Long,
    val version: Int
)