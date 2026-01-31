package ch.opum.tricktrack.data

import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.data.place.SavedPlaceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TripRepository(private val tripDao: TripDao, private val savedPlaceDao: SavedPlaceDao) {

    val confirmedTrips: Flow<List<Trip>> = tripDao.getConfirmedTrips()
    val unconfirmedTrips: Flow<List<Trip>> = tripDao.getUnconfirmedTrips()

    fun getAllSavedPlaces(): Flow<List<SavedPlace>> {
        return savedPlaceDao.getAll()
    }

    // FIXED: Changed to 'suspend' and removed 'runBlocking'.
    // Use this when you need a single snapshot of the list (not a live stream).
    suspend fun getSavedPlacesList(): List<SavedPlace> {
        return savedPlaceDao.getAll().first()
    }

    suspend fun insert(trip: Trip) {
        tripDao.insert(trip)
    }

    suspend fun updateTrip(trip: Trip) {
        tripDao.update(trip)
    }

    suspend fun deleteTrip(trip: Trip) {
        tripDao.delete(trip)
    }

    suspend fun deleteTrips(trips: List<Trip>) {
        tripDao.deleteTrips(trips)
    }

    suspend fun getTripsForBackup(): List<Trip> {
        return tripDao.getTripsForBackup()
    }

    suspend fun restoreTrips(trips: List<Trip>) {
        tripDao.restoreTrips(trips)
    }

    suspend fun restorePlaces(places: List<SavedPlace>) {
        savedPlaceDao.deleteAll()
        places.forEach { savedPlaceDao.insert(it) }
    }
}