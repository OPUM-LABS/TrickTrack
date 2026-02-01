package ch.opum.tricktrack.data.repository

import ch.opum.tricktrack.data.CompanyDao
import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DriverDao
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.VehicleDao
import ch.opum.tricktrack.data.VehicleEntity
import kotlinx.coroutines.flow.Flow

class FavouritesRepository(
    private val driverDao: DriverDao,
    private val companyDao: CompanyDao,
    private val vehicleDao: VehicleDao
) {
    fun getAllDrivers(): Flow<List<DriverEntity>> = driverDao.getAll()
    fun getAllCompanies(): Flow<List<CompanyEntity>> = companyDao.getAll()
    fun getAllVehicles(): Flow<List<VehicleEntity>> = vehicleDao.getAll()

    suspend fun getDriverById(id: Int): DriverEntity? = driverDao.getById(id)
    suspend fun getCompanyById(id: Int): CompanyEntity? = companyDao.getById(id)
    suspend fun getVehicleById(id: Int): VehicleEntity? = vehicleDao.getById(id)
}
