package ch.opum.tricktrack.ui.settings

import android.app.Application
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
import kotlinx.coroutines.flow.first
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
    val backupTimeHour: Flow<Int> = userPreferencesRepository.backupTimeHour
    val backupTimeMinute: Flow<Int> = userPreferencesRepository.backupTimeMinute

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupEnabled(enabled)
            if (enabled) {
                val hour = userPreferencesRepository.backupTimeHour.first()
                val minute = userPreferencesRepository.backupTimeMinute.first()
                BackupScheduler.scheduleBackupWorker(getApplication(), hour, minute)
            } else {
                BackupScheduler.cancelBackupWorker(getApplication())
            }
        }
    }

    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupFrequency(frequency)
            val hour = userPreferencesRepository.backupTimeHour.first()
            val minute = userPreferencesRepository.backupTimeMinute.first()
            BackupScheduler.scheduleBackupWorker(getApplication(), hour, minute)
        }
    }

    fun setBackupDayOfWeek(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupDayOfWeek(day)
            val hour = userPreferencesRepository.backupTimeHour.first()
            val minute = userPreferencesRepository.backupTimeMinute.first()
            BackupScheduler.scheduleBackupWorker(getApplication(), hour, minute)
        }
    }

    fun setBackupDayOfMonth(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupDayOfMonth(day)
            val hour = userPreferencesRepository.backupTimeHour.first()
            val minute = userPreferencesRepository.backupTimeMinute.first()
            BackupScheduler.scheduleBackupWorker(getApplication(), hour, minute)
        }
    }

    fun setBackupFolderUri(uri: Uri) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupFolderUri(uri.toString())
        }
    }

    fun setBackupTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupTime(hour, minute)
            BackupScheduler.scheduleBackupWorker(getApplication(), hour, minute)
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
            } catch (_: Exception) {
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

                try {
                    val container = gson.fromJson(json, BackupContainer::class.java)
                    if (container != null && (container.trips != null || container.places != null || container.drivers != null || container.companies != null || container.vehicles != null)) {
                        restoredContainer = container
                    }
                } catch (_: JsonSyntaxException) {
                }

                if (restoredContainer == null) {
                    try {
                        val oldListType = object : TypeToken<List<Trip>>() {}.type
                        val oldTrips: List<Trip>? = gson.fromJson(json, oldListType)
                        if (!oldTrips.isNullOrEmpty()) {
                            restoredContainer = BackupContainer(trips = oldTrips)
                        }
                    } catch (_: JsonSyntaxException) {
                    }
                }

                if (restoredContainer != null) {
                    tripRepository.restoreFullBackup(restoredContainer)
                    Toast.makeText(getApplication(), R.string.import_successful, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), R.string.import_failed, Toast.LENGTH_SHORT).show()
                }

            } catch (_: Exception) {
                Toast.makeText(getApplication(), R.string.import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
