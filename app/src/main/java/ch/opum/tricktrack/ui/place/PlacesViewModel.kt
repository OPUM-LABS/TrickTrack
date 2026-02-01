package ch.opum.tricktrack.ui.place

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.opum.tricktrack.GeocoderHelper
import ch.opum.tricktrack.data.CompanyDao
import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DriverDao
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.VehicleDao
import ch.opum.tricktrack.data.VehicleEntity
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.data.place.SavedPlaceDao
import ch.opum.tricktrack.ui.LocationSuggestion
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class PlacesViewModel(
    application: Application,
    private val savedPlaceDao: SavedPlaceDao,
    private val driverDao: DriverDao,
    private val companyDao: CompanyDao,
    private val vehicleDao: VehicleDao,
    private val geocoderHelper: GeocoderHelper // Inject GeocoderHelper
) : AndroidViewModel(application) {

    var onFabClicked: ((Int) -> Unit)? = null

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    val placesList: StateFlow<List<SavedPlace>> = savedPlaceDao.getAll()
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driversList: StateFlow<List<DriverEntity>> = driverDao.getAll()
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companiesList: StateFlow<List<CompanyEntity>> = companyDao.getAll()
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehiclesList: StateFlow<List<VehicleEntity>> = vehicleDao.getAll()
        .map { list -> list.sortedBy { it.licensePlate.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun <T> groupByName(items: List<T>, nameSelector: (T) -> String): Map<Char, List<T>> {
        return items
            .groupBy {
                nameSelector(it).firstOrNull()?.uppercaseChar()?.let { char ->
                    if (char.isLetter()) char else '#'
                } ?: '#'
            }
            .toSortedMap()
    }

    val groupedPlaces: StateFlow<Map<Char, List<SavedPlace>>> = placesList
        .map { groupByName(it) { item -> item.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val groupedDrivers: StateFlow<Map<Char, List<DriverEntity>>> = driversList
        .map { groupByName(it) { item -> item.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val groupedCompanies: StateFlow<Map<Char, List<CompanyEntity>>> = companiesList
        .map { groupByName(it) { item -> item.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val groupedVehicles: StateFlow<Map<Char, List<VehicleEntity>>> = vehiclesList
        .map { groupByName(it) { item -> item.licensePlate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    private val _addressSuggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val addressSuggestions = _addressSuggestions.asStateFlow()

    private val _nameSuggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val nameSuggestions = _nameSuggestions.asStateFlow()

    private var searchJob: Job? = null
    private val geocoder by lazy { Geocoder(getApplication<Application>().applicationContext) }

    private fun formatAddress(address: Address): String {
        val street = address.thoroughfare
        val number = address.subThoroughfare
        val postalCode = address.postalCode
        val city = address.locality

        val firstPart = listOfNotNull(street, number).joinToString(" ")
        val secondPart = listOfNotNull(postalCode, city).joinToString(" ")

        return listOfNotNull(firstPart, secondPart).joinToString(", ")
    }

    private fun performSearch(query: String, suggestionsState: MutableStateFlow<List<LocationSuggestion>>) {
        searchJob?.cancel()
        if (query.isBlank()) {
            suggestionsState.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300) // Debounce delay

            val localPlaces = savedPlaceDao.getAll().first()
            val localSuggestions = localPlaces
                .filter {
                    it.name.contains(query, ignoreCase = true) || it.address.contains(
                        query,
                        ignoreCase = true
                    )
                }
                .map {
                    LocationSuggestion(
                        title = it.name,
                        subtitle = it.address,
                        fullAddress = "${it.name}, ${it.address}",
                        isFavorite = true,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        postalCode = null // Local places don't have postal code in SavedPlace
                    )
                }

            val photonSuggestions = try {
                // --- Get current location for bias ---
                val lastLocation = try {
                    fusedLocationClient.lastLocation.await()
                } catch (e: SecurityException) {
                    null
                }

                var urlString = "https://photon.komoot.io/api/?q=$query&limit=5"
                if (lastLocation != null) {
                    urlString += "&lat=${lastLocation.latitude}&lon=${lastLocation.longitude}"
                }
                Log.d("PhotonSearch", "URL: $urlString")

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonResponse = JSONObject(response)
                val features = jsonResponse.getJSONArray("features")
                val suggestions = mutableListOf<LocationSuggestion>()

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val properties = feature.getJSONObject("properties")
                    Log.d("PhotonSearch", "Properties: $properties") // Log the properties object
                    val geometry = feature.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")

                    val name = properties.optString("name")
                    val street = properties.optString("street")
                    val housenumber = properties.optString("housenumber")
                    val city = properties.optString("city")
                    val postcode = properties.optString("postcode")

                    // Requirement 1: Display Name (Title)
                    val title = name.ifBlank {
                        listOf(street, housenumber).filter { it.isNotBlank() }.joinToString(" ")
                    }

                    // Requirement 2: Full Address (Subtitle)
                    val streetAndNumber = listOf(street, housenumber).filter { it.isNotBlank() }.joinToString(" ")
                    val postcodeAndCity = listOf(postcode, city).filter { it.isNotBlank() }.joinToString(" ")
                    val subtitle = listOf(streetAndNumber, postcodeAndCity).filter { it.isNotBlank() }.joinToString(", ")

                    // For the full address to be passed when selected
                    val fullAddressForSelection = if (name.isNotBlank() && name != streetAndNumber) {
                        "$name, $subtitle"
                    } else {
                        subtitle
                    }

                    if (title.isNotBlank()) {
                        suggestions.add(
                            LocationSuggestion(
                                title = title,
                                subtitle = subtitle,
                                fullAddress = fullAddressForSelection, // This is what is used for display and selection
                                isFavorite = false,
                                latitude = coordinates.getDouble(1),
                                longitude = coordinates.getDouble(0),
                                postalCode = postcode.ifBlank { null }
                            )
                        )
                    }
                }
                suggestions
            } catch (e: Exception) {
                Log.e("PhotonSearch", "Error fetching from Photon API", e)
                emptyList()
            }

            suggestionsState.value = (localSuggestions + photonSuggestions).distinctBy { it.fullAddress }
        }
    }

    fun searchAddress(query: String) {
        performSearch(query, _addressSuggestions)
    }

    fun searchName(query: String) {
        performSearch(query, _nameSuggestions)
    }

    fun clearAddressSuggestions() {
        _addressSuggestions.value = emptyList()
    }

    fun clearNameSuggestions() {
        _nameSuggestions.value = emptyList()
    }

    fun addPlace(name: String, address: String, latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val canonicalAddress = geocoderHelper.getAddressFromLocation(latitude, longitude)

            val savedPlace = SavedPlace(
                name = name,
                address = canonicalAddress, // Use the canonical address
                latitude = latitude,
                longitude = longitude
            )
            savedPlaceDao.insert(savedPlace)
        }
    }

    fun updatePlace(
        place: SavedPlace,
        newName: String,
        newAddress: String, // This newAddress might be from user input, not necessarily canonical
        newLatitude: Double?,
        newLongitude: Double?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val canonicalAddress = if (newLatitude != null && newLongitude != null) {
                geocoderHelper.getAddressFromLocation(newLatitude, newLongitude)
            } else {
                // If lat/lng are not changed, try to geocode the newAddress text, or keep old
                geocoderHelper.getAddressFromName(newAddress)
            }

            val updatedPlace = place.copy(
                name = newName,
                address = canonicalAddress, // Use the canonical address
                latitude = newLatitude ?: place.latitude,
                longitude = newLongitude ?: place.longitude
            )
            savedPlaceDao.update(updatedPlace)
        }
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch {
            savedPlaceDao.delete(place)
        }
    }

    // New functions for Driver, Company, Vehicle
    fun addDriver(name: String) {
        viewModelScope.launch {
            driverDao.insert(DriverEntity(name = name))
        }
    }

    fun updateDriver(driver: DriverEntity) {
        viewModelScope.launch {
            driverDao.update(driver)
        }
    }

    fun deleteDriver(driver: DriverEntity) {
        viewModelScope.launch {
            driverDao.delete(driver)
        }
    }

    fun addCompany(name: String) {
        viewModelScope.launch {
            companyDao.insert(CompanyEntity(name = name))
        }
    }

    fun updateCompany(company: CompanyEntity) {
        viewModelScope.launch {
            companyDao.update(company)
        }
    }

    fun deleteCompany(company: CompanyEntity) {
        viewModelScope.launch {
            companyDao.delete(company)
        }
    }

    fun addVehicle(licensePlate: String, carModel: String?) {
        viewModelScope.launch {
            vehicleDao.insert(VehicleEntity(licensePlate = licensePlate, carModel = carModel))
        }
    }

    fun updateVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            vehicleDao.update(vehicle)
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            vehicleDao.delete(vehicle)
        }
    }
}
