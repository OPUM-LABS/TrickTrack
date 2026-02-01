package ch.opum.tricktrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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
}
