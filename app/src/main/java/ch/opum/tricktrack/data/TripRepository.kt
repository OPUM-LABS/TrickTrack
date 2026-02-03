package ch.opum.tricktrack.data

import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.VehicleEntity
import ch.opum.tricktrack.data.place.SavedPlaceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TripRepository(
    private val tripDao: TripDao,
    private val savedPlaceDao: SavedPlaceDao,
    private val driverDao: DriverDao,
    private val companyDao: CompanyDao,
    private val vehicleDao: VehicleDao
) {

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

    suspend fun getAllDataForBackup(): BackupContainer {
        val trips = tripDao.getTripsForBackup()
        val places = savedPlaceDao.getAll().first()
        val drivers = driverDao.getAll().first()
        val companies = companyDao.getAll().first()
        val vehicles = vehicleDao.getAll().first()

        return BackupContainer(
            trips = trips,
            places = places,
            drivers = drivers,
            companies = companies,
            vehicles = vehicles
        )
    }

    suspend fun restoreFullBackup(container: BackupContainer) {
        // Step A: Clear existing data
        tripDao.deleteAll()
        savedPlaceDao.deleteAll()
        driverDao.deleteAll()
        companyDao.deleteAll()
        vehicleDao.deleteAll()

        // Step B: Insert new data from the container
        container.trips?.let { tripDao.insertAll(it) }
        container.places?.let { savedPlaceDao.insertAll(it) }
        container.drivers?.let { driverDao.insertAll(it) }
        container.companies?.let { companyDao.insertAll(it) }
        container.vehicles?.let { vehicleDao.insertAll(it) }
    }
}
