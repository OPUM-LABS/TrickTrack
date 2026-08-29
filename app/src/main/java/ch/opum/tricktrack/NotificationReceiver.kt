package ch.opum.tricktrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.opum.tricktrack.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tripId = intent.getLongExtra("trip_id", -1L)
        if (tripId == -1L) return

        val application = context.applicationContext as TripApplication
        val repository = application.repository

        when (intent.action) {
            ACTION_ACCEPT_TRIP -> {
                AppLogger.log("NotificationReceiver", "Accepting trip: $tripId")
                application.applicationScope.launch(Dispatchers.IO) {
                    val trip = repository.getTripById(tripId)
                    trip?.let {
                        repository.updateTrip(it.copy(isConfirmed = true))
                        TripNotificationManager.cancelTripNotification(context, tripId)
                    }
                }
            }
            ACTION_DECLINE_TRIP -> {
                AppLogger.log("NotificationReceiver", "Declining trip: $tripId")
                application.applicationScope.launch(Dispatchers.IO) {
                    val trip = repository.getTripById(tripId)
                    trip?.let {
                        repository.deleteTrip(it)
                        TripNotificationManager.cancelTripNotification(context, tripId)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_ACCEPT_TRIP = "ch.opum.tricktrack.ACTION_ACCEPT_TRIP"
        const val ACTION_DECLINE_TRIP = "ch.opum.tricktrack.ACTION_DECLINE_TRIP"
    }
}
