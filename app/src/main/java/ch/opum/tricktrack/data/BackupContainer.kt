package ch.opum.tricktrack.data

import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.VehicleEntity

data class BackupContainer(
    val version: Int = 1,
    val trips: List<Trip>? = null,
    val places: List<SavedPlace>? = null,
    val drivers: List<DriverEntity>? = null,
    val companies: List<CompanyEntity>? = null,
    val vehicles: List<VehicleEntity>? = null
)
