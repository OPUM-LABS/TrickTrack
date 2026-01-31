package ch.opum.tricktrack.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.opum.tricktrack.backup.BackupManager
import ch.opum.tricktrack.data.TripRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // This is a placeholder. You should replace this with your actual
    // dependency injection for the repository and backup manager.
    private val tripRepository: TripRepository = TODO()
    private val backupManager: BackupManager = BackupManager()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val trips = tripRepository.getTripsForBackup()
            val settings = getSettingsMap()
            val places = tripRepository.getSavedPlacesList()
            val json = backupManager.createBackupJson(trips, settings, places)

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
                        tripRepository.restorePlaces(backupData.places)
                        // You'll need to implement the logic to restore settings
                        // restoreSettings(backupData.settings)
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }

    private fun getSettingsMap(): Map<String, String> {
        // This is a placeholder. You should implement this to read
        // all key-value pairs from your SharedPreferences.
        return emptyMap()
    }
}