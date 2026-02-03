package ch.opum.tricktrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.opum.tricktrack.data.CompanyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Insert
    suspend fun insert(company: CompanyEntity)

    @Update
    suspend fun update(company: CompanyEntity)

    @Delete
    suspend fun delete(company: CompanyEntity)

    @Query("SELECT * FROM companies")
    fun getAll(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getById(id: Int): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompanyEntity>)

    @Query("DELETE FROM companies")
    suspend fun deleteAll()
}
