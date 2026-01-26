package ch.opum.tricktrack.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.opum.tricktrack.data.TripRepository

class SettingsViewModelFactory(
    private val application: Application,
    private val tripRepository: TripRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application, tripRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}