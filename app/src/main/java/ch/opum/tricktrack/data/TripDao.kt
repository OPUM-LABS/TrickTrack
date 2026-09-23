package ch.opum.tricktrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: Trip): Long

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    @Update
    suspend fun update(trip: Trip)

    @Update
    suspend fun updateTrips(trips: List<Trip>)

    @Delete
    suspend fun delete(trip: Trip)

    @Delete
    suspend fun deleteTrips(trips: List<Trip>)

    @Query("DELETE FROM trips WHERE id IN (:ids)")
    suspend fun deleteTripsByIds(ids: List<Long>)

    @Query("SELECT * FROM trips WHERE isConfirmed = 1 AND vehicleId = :vehicleId AND (date > :date OR (date = :date AND id > :tripId)) ORDER BY date ASC, id ASC")
    suspend fun getSubsequentTripsForVehicle(vehicleId: Int, date: Date, tripId: Long): List<Trip>

    @Query("SELECT endOdometer FROM trips WHERE isConfirmed = 1 AND vehicleId = :vehicleId AND endOdometer IS NOT NULL ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLatestEndOdometerForVehicle(vehicleId: Int): Double?

    @Transaction
    suspend fun updateTripAndCascade(updatedTrip: Trip): Double? {
        val oldTrip = getTripById(updatedTrip.id)
        val oldEndOdo = oldTrip?.endOdometer
        update(updatedTrip)

        val vehicleId = updatedTrip.vehicleId ?: return updatedTrip.endOdometer
        val startEndOdo = updatedTrip.endOdometer ?: return getLatestEndOdometerForVehicle(vehicleId)

        val subsequentTrips = getSubsequentTripsForVehicle(vehicleId, updatedTrip.date, updatedTrip.id)
        var currentEndOdo = startEndOdo
        var previousTripOldEnd = oldEndOdo
        for (subTrip in subsequentTrips) {
            if (subTrip.endOdometer == null) continue
            val subTripOldStart = subTrip.startOdometer
                ?: (kotlin.math.round((subTrip.endOdometer - subTrip.distance.coerceAtLeast(0.0)) * 10000.0) / 10000.0)
            val subTripOldEnd = subTrip.endOdometer
            val subTripDistance = if (subTrip.startOdometer != null) {
                (subTrip.endOdometer - subTrip.startOdometer).coerceAtLeast(0.0)
            } else {
                subTrip.distance.coerceAtLeast(0.0)
            }

            val isChained = previousTripOldEnd != null && kotlin.math.abs(subTripOldStart - previousTripOldEnd) < 0.05
            if (isChained || currentEndOdo > subTripOldStart) {
                val newStart = currentEndOdo
                val newEnd = kotlin.math.round((newStart + subTripDistance) * 10000.0) / 10000.0
                val newTrip = subTrip.copy(
                    startOdometer = newStart,
                    endOdometer = newEnd,
                    distance = newEnd - newStart
                )
                update(newTrip)
                currentEndOdo = newEnd
                previousTripOldEnd = subTripOldEnd
            } else {
                // Gap preserved: subTrip had a manual start odometer starting its own chain of trust.
                break
            }
        }
        return getLatestEndOdometerForVehicle(vehicleId)
    }

    @Transaction
    suspend fun insertTripAndCascade(newTrip: Trip): Pair<Long, Double?> {
        val id = insert(newTrip)
        val vehicleId = newTrip.vehicleId ?: return Pair(id, newTrip.endOdometer)
        val startEndOdo = newTrip.endOdometer ?: return Pair(id, getLatestEndOdometerForVehicle(vehicleId))

        val subsequentTrips = getSubsequentTripsForVehicle(vehicleId, newTrip.date, id)
        var currentEndOdo = startEndOdo
        var previousTripOldEnd = newTrip.endOdometer
        for (subTrip in subsequentTrips) {
            if (subTrip.endOdometer == null) continue
            val subTripOldStart = subTrip.startOdometer
                ?: (kotlin.math.round((subTrip.endOdometer - subTrip.distance.coerceAtLeast(0.0)) * 10000.0) / 10000.0)
            val subTripOldEnd = subTrip.endOdometer
            val subTripDistance = if (subTrip.startOdometer != null) {
                (subTrip.endOdometer - subTrip.startOdometer).coerceAtLeast(0.0)
            } else {
                subTrip.distance.coerceAtLeast(0.0)
            }

            val isChained = previousTripOldEnd != null && kotlin.math.abs(subTripOldStart - previousTripOldEnd) < 0.05
            if (isChained || currentEndOdo > subTripOldStart) {
                val newStart = currentEndOdo
                val newEnd = kotlin.math.round((newStart + subTripDistance) * 10000.0) / 10000.0
                val updated = subTrip.copy(
                    startOdometer = newStart,
                    endOdometer = newEnd,
                    distance = newEnd - newStart
                )
                update(updated)
                currentEndOdo = newEnd
                previousTripOldEnd = subTripOldEnd
            } else {
                // Gap preserved: subTrip had a manual start odometer starting its own chain of trust.
                break
            }
        }
        return Pair(id, getLatestEndOdometerForVehicle(vehicleId))
    }

    @Transaction
    suspend fun mergeTripsAndCascade(mergedTrip: Trip, originalTripIds: List<Long>): Pair<Long, Double?> {
        deleteTripsByIds(originalTripIds)
        val id = insert(mergedTrip)
        val vehicleId = mergedTrip.vehicleId ?: return Pair(id, mergedTrip.endOdometer)
        val startEndOdo = mergedTrip.endOdometer ?: return Pair(id, getLatestEndOdometerForVehicle(vehicleId))

        val subsequentTrips = getSubsequentTripsForVehicle(vehicleId, mergedTrip.date, id)
        var currentEndOdo = startEndOdo
        var previousTripOldEnd = mergedTrip.endOdometer
        for (subTrip in subsequentTrips) {
            if (subTrip.endOdometer == null) continue
            val subTripOldStart = subTrip.startOdometer
                ?: (kotlin.math.round((subTrip.endOdometer - subTrip.distance.coerceAtLeast(0.0)) * 10000.0) / 10000.0)
            val subTripOldEnd = subTrip.endOdometer
            val subTripDistance = if (subTrip.startOdometer != null) {
                (subTrip.endOdometer - subTrip.startOdometer).coerceAtLeast(0.0)
            } else {
                subTrip.distance.coerceAtLeast(0.0)
            }

            val isChained = previousTripOldEnd != null && kotlin.math.abs(subTripOldStart - previousTripOldEnd) < 0.05
            if (isChained || currentEndOdo > subTripOldStart) {
                val newStart = currentEndOdo
                val newEnd = kotlin.math.round((newStart + subTripDistance) * 10000.0) / 10000.0
                val updated = subTrip.copy(
                    startOdometer = newStart,
                    endOdometer = newEnd,
                    distance = newEnd - newStart
                )
                update(updated)
                currentEndOdo = newEnd
                previousTripOldEnd = subTripOldEnd
            } else {
                // Gap preserved: subTrip had a manual start odometer starting its own chain of trust.
                break
            }
        }
        return Pair(id, getLatestEndOdometerForVehicle(vehicleId))
    }

    @Transaction
    @Query("SELECT * FROM trips WHERE isConfirmed = 1 ORDER BY date DESC")
    fun getConfirmedTrips(): Flow<List<TripWithVehicle>>

    @Transaction
    @Query("SELECT * FROM trips WHERE isConfirmed = 0 ORDER BY date DESC")
    fun getUnconfirmedTrips(): Flow<List<TripWithVehicle>>

    @Query("SELECT * FROM trips ORDER BY id DESC LIMIT 1")
    suspend fun getLastTrip(): Trip?

    @Query("SELECT * FROM trips")
    suspend fun getTripsForBackup(): List<Trip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Trip>)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
