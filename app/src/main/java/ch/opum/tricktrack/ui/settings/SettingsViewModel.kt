package ch.opum.tricktrack.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.opum.tricktrack.R
import ch.opum.tricktrack.backup.BackupManager
import ch.opum.tricktrack.backup.BackupScheduler
import ch.opum.tricktrack.data.TripRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val tripRepository: TripRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

    private val backupManager: BackupManager = BackupManager()

    val autoBackupEnabled: Flow<Boolean> = userPreferencesRepository.autoBackupEnabled
    val backupFrequency: Flow<String> = userPreferencesRepository.backupFrequency
    val backupDayOfWeek: Flow<Int> = userPreferencesRepository.backupDayOfWeek
    val backupDayOfMonth: Flow<Int> = userPreferencesRepository.backupDayOfMonth
    val backupFolderUri: Flow<String?> = userPreferencesRepository.backupFolderUri

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupEnabled(enabled)
            if (enabled) {
                BackupScheduler.scheduleBackupWorker(getApplication())
            }
        }
    }

    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupFrequency(frequency)
            BackupScheduler.scheduleBackupWorker(getApplication())
        }
    }

    fun setBackupDayOfWeek(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupDayOfWeek(day)
            BackupScheduler.scheduleBackupWorker(getApplication())
        }
    }

    fun setBackupDayOfMonth(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupDayOfMonth(day)
            BackupScheduler.scheduleBackupWorker(getApplication())
        }
    }

    fun setBackupFolderUri(uri: Uri) {
        viewModelScope.launch {
            val contentResolver = getApplication<Application>().contentResolver
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            userPreferencesRepository.setBackupFolderUri(uri.toString())
        }
    }


    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val trips = tripRepository.getTripsForBackup()
            val settings = userPreferencesRepository.getAllPreferences()
            val json = backupManager.createBackupJson(trips, settings)

            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    val json = it.reader().readText()
                    val backupData = backupManager.restoreBackupFromJson(json)
                    if (backupData != null) {
                        tripRepository.restoreTrips(backupData.trips)
                        // You'll need to implement the logic to restore settings
                        // restoreSettings(backupData.settings)
                        Toast.makeText(getApplication(), R.string.import_successful, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(getApplication(), R.string.import_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions
                Toast.makeText(getApplication(), R.string.import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}