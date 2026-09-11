package ch.opum.tricktrack.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ch.opum.tricktrack.logging.AppLogger
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BackupScheduler {

    private const val BACKUP_WORKER_TAG = "BackupWorker"

    fun scheduleBackupWorker(context: Context, hour: Int = 2, minute: Int = 0) {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = targetTime.timeInMillis - now.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val backupWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        AppLogger.log("BackupScheduler", "Scheduling BackupWorker targeting %02d:%02d with initial delay of ${initialDelay / 1000 / 60} minutes".format(hour, minute))

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORKER_TAG,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            backupWorkRequest
        )
    }

    fun cancelBackupWorker(context: Context) {
        AppLogger.log("BackupScheduler", "Cancelling BackupWorker")
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORKER_TAG)
    }
}
