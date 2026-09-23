package ch.opum.tricktrack.data

import android.content.Context
import ch.opum.tricktrack.GeocoderHelper
import ch.opum.tricktrack.R
import ch.opum.tricktrack.TripNotificationManager
import ch.opum.tricktrack.logging.AppLogger
import ch.opum.tricktrack.ui.TripTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

object ActiveTripRecoveryHelper {

    fun recoverInterruptedTripIfNeeded(
        context: Context,
        repository: TripRepository,
        geocoderHelper: GeocoderHelper,
        userPreferencesRepository: UserPreferencesRepository,
        scope: CoroutineScope
    ) {
        val checkpoint = ActiveTripCheckpointManager.getCheckpoint(context) ?: return
        ActiveTripCheckpointManager.clearCheckpoint(context)

        // Only recover if trip had minimal movement (e.g. >= 50m)
        if (checkpoint.distanceMeters < 50f) {
            AppLogger.log("ActiveTripRecoveryHelper", "Checkpoint distance too short (${checkpoint.distanceMeters}m). Discarding.")
            return
        }

        AppLogger.log("ActiveTripRecoveryHelper", "Recovering interrupted trip from checkpoint: dist=${checkpoint.distanceMeters}m, trigger=${checkpoint.trigger}")

        scope.launch(Dispatchers.IO) {
            try {
                val offlinePendingAddress = context.getString(R.string.address_pending_offline)
                val isSmartLocationEnabled = userPreferencesRepository.isSmartLocationEnabled.first()
                val smartLocationRadius = userPreferencesRepository.smartLocationRadius.first()
                val savedPlaces = repository.getAllSavedPlaces().first()

                val rawStart = if (checkpoint.startLat != null && checkpoint.startLon != null) {
                    geocoderHelper.getAddressFromLocation(checkpoint.startLat, checkpoint.startLon)
                } else offlinePendingAddress
                val geocodedStart = if (rawStart == "Unknown Start" || rawStart.isBlank()) offlinePendingAddress else rawStart
                val startAddress = geocoderHelper.getSmartAddress(
                    originalAddress = geocodedStart,
                    lat = checkpoint.startLat,
                    lng = checkpoint.startLon,
                    favorites = savedPlaces,
                    isEnabled = isSmartLocationEnabled,
                    radius = smartLocationRadius
                )

                val rawEnd = if (checkpoint.lastLat != null && checkpoint.lastLon != null) {
                    geocoderHelper.getAddressFromLocation(checkpoint.lastLat, checkpoint.lastLon)
                } else offlinePendingAddress
                val geocodedEnd = if (rawEnd == "Unknown End" || rawEnd.isBlank()) offlinePendingAddress else rawEnd
                val endAddress = geocoderHelper.getSmartAddress(
                    originalAddress = geocodedEnd,
                    lat = checkpoint.lastLat,
                    lng = checkpoint.lastLon,
                    favorites = savedPlaces,
                    isEnabled = isSmartLocationEnabled,
                    radius = smartLocationRadius
                )

                val distKm = checkpoint.distanceMeters / 1000.0
                val trip = Trip(
                    startLoc = startAddress,
                    endLoc = endAddress,
                    distance = distKm,
                    gpsDistance = distKm,
                    type = checkpoint.type,
                    description = "",
                    date = Date(checkpoint.startTime),
                    endDate = checkpoint.lastUpdated,
                    isConfirmed = false,
                    startLat = checkpoint.startLat,
                    startLon = checkpoint.startLon,
                    endLat = checkpoint.lastLat,
                    endLon = checkpoint.lastLon,
                    isAutomatic = checkpoint.trigger != TripTrigger.MANUAL.name,
                    vehicleId = checkpoint.vehicleId,
                    trigger = checkpoint.trigger ?: TripTrigger.AUTOMATIC.name,
                    routePolyline = checkpoint.routePolyline,
                    stopReason = "SHUTDOWN"
                )

                val newId = repository.insert(trip)
                val tripWithId = trip.copy(id = newId)
                val distanceUnit = userPreferencesRepository.distanceUnit.first()
                TripNotificationManager.sendTripReviewNotification(context, tripWithId, distanceUnit)
                AppLogger.log("ActiveTripRecoveryHelper", "Interrupted trip saved for review with ID: $newId and stopReason=SHUTDOWN")
            } catch (e: Exception) {
                AppLogger.log("ActiveTripRecoveryHelper", "Failed to recover interrupted trip: ${e.message}")
            }
        }
    }
}
