package ch.opum.tricktrack.backup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.opum.tricktrack.R
import ch.opum.tricktrack.TripApplication
import ch.opum.tricktrack.data.dataStore
import ch.opum.tricktrack.logging.AppLogger
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private object PreferencesKeys {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val BACKUP_DAY_OF_WEEK = intPreferencesKey("backup_day_of_week")
        val BACKUP_DAY_OF_MONTH = intPreferencesKey("backup_day_of_month")
        val BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val LAST_AUTO_BACKUP_TIMESTAMP = longPreferencesKey("last_auto_backup_timestamp")
    }

    override suspend fun doWork(): Result {
        val application = applicationContext as TripApplication
        val userPreferences = application.dataStore.data.first()

        val isAutoBackupEnabled = userPreferences[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false
        if (!isAutoBackupEnabled) {
            return Result.success()
        }

        val backupFolderUriString = userPreferences[PreferencesKeys.BACKUP_FOLDER_URI]
        if (backupFolderUriString.isNullOrBlank()) {
            AppLogger.log("BackupWorker", "Auto backup is enabled, but no backup folder URI is defined. Doing nothing.")
            return Result.success()
        }

        val backupFrequency = userPreferences[PreferencesKeys.BACKUP_FREQUENCY] ?: "DAILY"
        val today = Calendar.getInstance()

        val shouldBackup = when (backupFrequency) {
            "DAILY" -> true
            "WEEKLY" -> {
                val backupDay = userPreferences[PreferencesKeys.BACKUP_DAY_OF_WEEK] ?: Calendar.SUNDAY
                today.get(Calendar.DAY_OF_WEEK) == backupDay
            }
            "MONTHLY" -> {
                val backupDay = userPreferences[PreferencesKeys.BACKUP_DAY_OF_MONTH] ?: 1
                today.get(Calendar.DAY_OF_MONTH) == backupDay
            }
            else -> false
        }

        if (!shouldBackup) {
            return Result.success()
        }

        val lastBackupTime = userPreferences[PreferencesKeys.LAST_AUTO_BACKUP_TIMESTAMP] ?: 0L
        val nowMs = System.currentTimeMillis()
        val eighteenHoursMs = 18 * 60 * 60 * 1000L

        if (nowMs - lastBackupTime < eighteenHoursMs) {
            AppLogger.log("BackupWorker", "Backup already executed within last 18 hours. Skipping.")
            return Result.success()
        }

        val backupManager = BackupManager()
        val tripRepository = application.repository
        val userPreferencesRepository = application.userPreferencesRepository

        try {
            val trips = tripRepository.getTripsForBackup()
            val settings = userPreferencesRepository.getAllPreferences()
            val places = tripRepository.getSavedPlacesList()
            val backupJson = backupManager.createBackupJson(trips, settings, places)

            val backupDir = DocumentFile.fromTreeUri(application, backupFolderUriString.toUri())
            if (backupDir != null && backupDir.canWrite()) {
                val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                val fileName = "tricktrack-backup_$timeStamp.json"
                val backupFile = backupDir.createFile("application/json", fileName)

                if (backupFile != null) {
                    application.contentResolver.openOutputStream(backupFile.uri)?.use {
                        it.write(backupJson.toByteArray())
                    }

                    // Housekeeping: Keep at most 5 recent backups
                    val backups = backupDir.listFiles().sortedBy { it.lastModified() }
                    if (backups.size > 5) {
                        for (i in 0 until backups.size - 5) {
                            backups[i].delete()
                        }
                    }
                    userPreferencesRepository.setLastAutoBackupTimestamp(nowMs)
                    showBackupNotification(true)
                    return Result.success()
                } else {
                    AppLogger.log("BackupWorker", "Failed to create backup file in selected directory.")
                    showBackupNotification(false)
                    return Result.failure()
                }
            } else {
                AppLogger.log("BackupWorker", "Selected backup directory is not writable or no longer accessible.")
                showBackupNotification(false)
                return Result.failure()
            }
        } catch (e: Exception) {
            AppLogger.log("BackupWorker", "Error executing auto backup: ${e.message}")
            showBackupNotification(false)
            return Result.failure()
        }
    }

    private fun showBackupNotification(success: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Cannot post notifications without permission
                return
            }

            val notificationManager = NotificationManagerCompat.from(applicationContext)
            val notificationId = 1001 // Unique ID for backup notifications

            val title = applicationContext.getString(R.string.backup_notification_title)
            val contentText = if (success) {
                applicationContext.getString(R.string.backup_notification_text)
            } else {
                applicationContext.getString(R.string.import_failed) // Reusing this string for backup failure
            }

            val builder = NotificationCompat.Builder(applicationContext, "backup_channel")
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handle potential SecurityException
        }
    }
}
