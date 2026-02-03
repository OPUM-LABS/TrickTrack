package ch.opum.tricktrack.data.place

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.opum.tricktrack.data.place.SavedPlace
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Insert
    suspend fun insert(savedPlace: SavedPlace)

    @Update
    suspend fun update(savedPlace: SavedPlace)

    @Delete
    suspend fun delete(savedPlace: SavedPlace)

    @Query("DELETE FROM saved_places")
    suspend fun deleteAll()

    @Query("SELECT * FROM saved_places")
    fun getAll(): Flow<List<SavedPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavedPlace>)
}