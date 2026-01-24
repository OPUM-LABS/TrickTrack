package ch.opum.tricktrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ch.opum.tricktrack.ui.TripTrigger
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class TripRecognitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "ch.opum.tricktrack.TRANSITION_ACTION") {
            return
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                for (event in it.transitionEvents) {
                    val activityType = when (event.activityType) {
                        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
                        DetectedActivity.STILL -> "STILL"
                        else -> "UNKNOWN"
                    }
                    val transitionType = when (event.transitionType) {
                        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
                        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
                        else -> "UNKNOWN"
                    }
                    Log.d("TripRecognition", "Activity: $activityType, Transition: $transitionType")

                    handleTransition(context, event)
                }
            }
        }
    }

    private fun handleTransition(context: Context, event: ActivityTransitionEvent) {
        when (event.activityType) {
            DetectedActivity.IN_VEHICLE -> {
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    Log.d("TripRecognition", "Starting trip due to entering vehicle.")
                    // To comply with background start restrictions, we start a foreground service.
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_START_AUTOMATIC
                        putExtra("trigger", TripTrigger.AUTOMATIC)
                    }
                    context.startForegroundService(serviceIntent)
                } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                    Log.d("TripRecognition", "Stopping trip due to exiting vehicle.")
                    context.stopService(Intent(context, LocationService::class.java))
                }
            }

            DetectedActivity.STILL -> {
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    Log.d("TripRecognition", "Stopping trip due to being still.")
                    context.stopService(Intent(context, LocationService::class.java))
                }
            }
        }
    }
}
