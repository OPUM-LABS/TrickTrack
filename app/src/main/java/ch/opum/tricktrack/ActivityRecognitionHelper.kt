package ch.opum.tricktrack

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionHelper(private val context: Context) {

    private val transitions = mutableListOf<ActivityTransition>()

    init {
        // We want to be notified when the user enters or exits a vehicle.
        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
    }

    private val request = ActivityTransitionRequest(transitions)

    private val intent by lazy {
        val intent = Intent("ch.opum.tricktrack.TRANSITION_ACTION")
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun registerForUpdates() {
        val client = ActivityRecognition.getClient(context)
        client.requestActivityTransitionUpdates(request, intent)
            .addOnSuccessListener {
                Log.d("ActivityRecognition", "Successfully registered for activity updates.")
            }
            .addOnFailureListener { e ->
                Log.e("ActivityRecognition", "Failed to register for activity updates.", e)
            }
    }

    @SuppressLint("MissingPermission")
    fun unregisterForUpdates() {
        val client = ActivityRecognition.getClient(context)
        client.removeActivityTransitionUpdates(intent)
            .addOnSuccessListener {
                Log.d("ActivityRecognition", "Successfully unregistered for activity updates.")
            }
            .addOnFailureListener { e ->
                Log.e("ActivityRecognition", "Failed to unregister for activity updates.", e)
            }
    }
}
