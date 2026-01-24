package ch.opum.tricktrack.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.opum.tricktrack.GeocoderHelper
import ch.opum.tricktrack.TripApplication
import ch.opum.tricktrack.data.TripRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import ch.opum.tricktrack.ui.place.PlacesViewModel
import ch.opum.tricktrack.ui.troubleshooting.TroubleshootingViewModel

class ViewModelFactory(
    private val application: Application,
    private val repository: TripRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(TripsViewModel::class.java) -> {
                val geocoderHelper = GeocoderHelper(application.applicationContext)
                TripsViewModel(application, repository, userPreferencesRepository, geocoderHelper) as T
            }
            modelClass.isAssignableFrom(PlacesViewModel::class.java) -> {
                val database = (application as TripApplication).database
                val geocoderHelper = GeocoderHelper(application.applicationContext)
                PlacesViewModel(application, database.savedPlaceDao(), geocoderHelper) as T
            }
            modelClass.isAssignableFrom(TroubleshootingViewModel::class.java) -> {
                TroubleshootingViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
