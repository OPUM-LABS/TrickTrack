package ch.opum.tricktrack.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.opum.tricktrack.TripApplication
import ch.opum.tricktrack.data.AppPreferences
import ch.opum.tricktrack.data.TripRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import ch.opum.tricktrack.data.repository.FavouritesRepository

class SettingsViewModelFactory(
    private val application: Application,
    private val tripRepository: TripRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val database = (application as TripApplication).database
            val favouritesRepository = FavouritesRepository(database.driverDao(), database.companyDao(), database.vehicleDao())
            val appPreferences = AppPreferences(application.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application, tripRepository, userPreferencesRepository, favouritesRepository, appPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}