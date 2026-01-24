package ch.opum.tricktrack.data.place

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Insert
    suspend fun insert(savedPlace: SavedPlace)

    @Update
    suspend fun update(savedPlace: SavedPlace)

    @Delete
    suspend fun delete(savedPlace: SavedPlace)

    @Query("SELECT * FROM saved_places")
    fun getAll(): Flow<List<SavedPlace>>
}