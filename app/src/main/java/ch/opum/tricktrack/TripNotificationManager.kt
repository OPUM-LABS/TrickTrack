package ch.opum.tricktrack

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.logging.AppLogger

object TripNotificationManager {

    private lateinit var CHANNEL_ID: String

    /**
     * Creates the notification channel for trip review updates.
     * This should be called once, for example, in your Application's onCreate method.
     */
    fun createNotificationChannel(context: Context) {
        if (!this::CHANNEL_ID.isInitialized) {
            CHANNEL_ID = context.getString(R.string.trip_review_channel_id)
        }
        val name = context.getString(R.string.notification_channel_name)
        val descriptionText = context.getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Sends a notification to the user about a trip that is ready for review.
     *
     * @param context The context.
     * @param trip The trip to be reviewed.
     */
    fun sendTripReviewNotification(context: Context, trip: Trip) {
        // Ensure CHANNEL_ID is initialized before use
        if (!this::CHANNEL_ID.isInitialized) {
            // This case should ideally not happen if createNotificationChannel is called first.
            // For robustness, we can initialize it here as well.
            CHANNEL_ID = context.getString(R.string.trip_review_channel_id)
        }

        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // The app does not have permission to post notifications.
                // You might want to log this or handle it in a specific way.
                return
            }
        }

        // Create an intent that will open the MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_REVIEW", true) // Bonus: Add extra for deep linking
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = context.getString(R.string.notification_content_text, trip.startLoc, trip.endLoc, trip.distance)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.tricktrack_outline) // Using launcher foreground as a safe fallback
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Automatically removes the notification when the user taps it

        // Add Accept and Decline actions
        val acceptIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_ACCEPT_TRIP
            putExtra("trip_id", trip.id)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context, trip.id.toInt() + 100, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DECLINE_TRIP
            putExtra("trip_id", trip.id)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context, trip.id.toInt() + 200, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(0, context.getString(R.string.button_accept), acceptPendingIntent)
        builder.addAction(0, context.getString(R.string.button_decline), declinePendingIntent)

        with(NotificationManagerCompat.from(context)) {
            val notificationId = trip.id.toInt()
            AppLogger.log("TripNotificationManager", "Showing notification for trip ID: ${trip.id} as notification ID: $notificationId")
            notify(notificationId, builder.build())
        }
    }

    /**
     * Cancels a specific trip review notification.
     *
     * @param context The context.
     * @param tripId The ID of the trip whose notification should be cancelled.
     */
    fun cancelTripNotification(context: Context, tripId: Long) {
        val notificationId = tripId.toInt()
        AppLogger.log("TripNotificationManager", "Cancelling notification for trip ID: $tripId as notification ID: $notificationId")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
