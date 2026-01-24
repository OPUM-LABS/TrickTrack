package ch.opum.tricktrack

import android.app.Application
import ch.opum.tricktrack.data.AppDatabase
import ch.opum.tricktrack.data.BluetoothRepository
import ch.opum.tricktrack.data.TripRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import ch.opum.tricktrack.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class TripApplication : Application() {
    // No need to cancel this scope as it'll live for the duration of the app
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TripRepository(database.tripDao(), database.savedPlaceDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(applicationContext) } // Instantiate UserPreferencesRepository
    val geocoderHelper by lazy { GeocoderHelper(applicationContext) } // Instantiate GeocoderHelper
    val bluetoothRepository by lazy { BluetoothRepository(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
    }
}