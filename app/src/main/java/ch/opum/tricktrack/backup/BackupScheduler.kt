package ch.opum.tricktrack.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {

    private const val BACKUP_WORKER_TAG = "BackupWorker"

    fun scheduleBackupWorker(context: Context) {
        val backupWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORKER_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWorkRequest
        )
    }
}