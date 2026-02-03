package ch.opum.tricktrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.opum.tricktrack.data.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: Trip)

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Delete
    suspend fun deleteTrips(trips: List<Trip>)

    @Query("SELECT * FROM trips WHERE isConfirmed = 1 ORDER BY date DESC")
    fun getConfirmedTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE isConfirmed = 0 ORDER BY date DESC")
    fun getUnconfirmedTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY id DESC LIMIT 1")
    suspend fun getLastTrip(): Trip?

    @Query("SELECT * FROM trips")
    suspend fun getTripsForBackup(): List<Trip>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun restoreTrips(trips: List<Trip>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Trip>)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
