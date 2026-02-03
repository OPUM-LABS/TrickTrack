package ch.opum.tricktrack.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.opum.tricktrack.R
import ch.opum.tricktrack.backup.BackupScheduler
import ch.opum.tricktrack.data.BackupContainer
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.TripRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val tripRepository: TripRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

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

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val container = tripRepository.getAllDataForBackup()
                val gson = Gson()
                val json = gson.toJson(container)

                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                Toast.makeText(getApplication(), R.string.backup_successful, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    it.reader().readText()
                } ?: throw Exception("Could not read backup file")

                val gson = Gson()
                var restoredContainer: BackupContainer? = null

                // Strategy 1: Try to parse as new BackupContainer format
                try {
                    val container = gson.fromJson(json, BackupContainer::class.java)
                    // Basic validation to ensure it's not an empty or malformed new format
                    if (container != null && (container.trips != null || container.places != null || container.drivers != null || container.companies != null || container.vehicles != null)) {
                        restoredContainer = container
                    }
                } catch (e: JsonSyntaxException) {
                    // Fallback to old format if new format parsing fails
                }

                // Strategy 2: If new format failed, try old List<Trip> format
                if (restoredContainer == null) {
                    try {
                        val oldListType = object : TypeToken<List<Trip>>() {}.type
                        val oldTrips: List<Trip> = gson.fromJson(json, oldListType)
                        if (oldTrips != null) {
                            restoredContainer = BackupContainer(trips = oldTrips)
                        }
                    } catch (e: JsonSyntaxException) {
                        // Both strategies failed
                    }
                }

                if (restoredContainer != null) {
                    tripRepository.restoreFullBackup(restoredContainer)
                    Toast.makeText(getApplication(), R.string.import_successful, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), R.string.import_failed, Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(getApplication(), R.string.import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
