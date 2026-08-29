package ch.opum.tricktrack.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.opum.tricktrack.GeocoderHelper
import ch.opum.tricktrack.LocationService
import ch.opum.tricktrack.TripNotificationManager
import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.ScheduleSettings
import ch.opum.tricktrack.data.ScheduleTarget
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.TripWithVehicle
import ch.opum.tricktrack.data.TripRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import ch.opum.tricktrack.data.VehicleEntity
import ch.opum.tricktrack.data.repository.DistanceRepository
import ch.opum.tricktrack.data.repository.FavouritesRepository
import ch.opum.tricktrack.logging.AppLogger
import ch.opum.tricktrack.ui.settings.PermissionHealthState
import ch.opum.tricktrack.ui.settings.PermissionRequirement
import ch.opum.tricktrack.ui.settings.PermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class TripType {
    ALL, BUSINESS, PERSONAL
}

data class FilterState(
    val type: TripType = TripType.ALL,
    val keyword: String = "",
    val startDate: Long? = null, // Timestamp for start of day
    val endDate: Long? = null,    // Timestamp for end of day
    val vehicleIds: Set<Int> = emptySet()
)

data class TripGroup(
    val date: Long,
    val trips: List<TripWithVehicle>,
    val totalDistance: Double
)

class TripsViewModel(
    application: Application,
    private val repository: TripRepository,
    val userPreferencesRepository: UserPreferencesRepository,
    private val geocoderHelper: GeocoderHelper, // Inject GeocoderHelper
    private val favouritesRepository: FavouritesRepository
) : AndroidViewModel(application) {

    private val distanceRepository = DistanceRepository(application)
    var isCalculating by mutableStateOf(false)
    var distanceInput by mutableStateOf("")

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilterState()
    )

    val isFilterActive: StateFlow<Boolean> = _filterState.map {
        it.type != TripType.ALL || it.keyword.isNotEmpty() || it.startDate != null || it.endDate != null || it.vehicleIds.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val confirmedTrips = combine(repository.confirmedTrips, _filterState) { allTrips, filter ->
        allTrips.filter { tripWithVehicle ->
            val trip = tripWithVehicle.trip
            val matchesType = when (filter.type) {
                TripType.ALL -> true
                TripType.BUSINESS -> trip.type == "Business"
                TripType.PERSONAL -> trip.type == "Personal"
            }

            val matchesKeyword = if (filter.keyword.isBlank()) {
                true
            } else {
                val keywordLower = filter.keyword.lowercase(Locale.getDefault())
                trip.startLoc.lowercase(Locale.getDefault()).contains(keywordLower) ||
                        trip.endLoc.lowercase(Locale.getDefault()).contains(keywordLower) ||
                        (trip.description?.lowercase(Locale.getDefault())?.contains(keywordLower)
                            ?: false)
            }

            val matchesStartDate = if (filter.startDate == null) {
                true
            } else {
                trip.date.time >= filter.startDate
            }

            val matchesEndDate = if (filter.endDate == null) {
                true
            } else {
                trip.date.time <= filter.endDate
            }

            val matchesVehicle = if (filter.vehicleIds.isEmpty()) {
                true
            } else {
                trip.vehicleId != null && filter.vehicleIds.contains(trip.vehicleId)
            }

            matchesType && matchesKeyword && matchesStartDate && matchesEndDate && matchesVehicle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groupedTrips: StateFlow<List<TripGroup>> = combine(
        confirmedTrips,
        userPreferencesRepository.isSmartLocationEnabled,
        userPreferencesRepository.smartLocationRadius
    ) { trips, isSmartLocationEnabled, smartLocationRadius ->
        trips.map { item ->
            val trip = item.trip
            val smartStart = geocoderHelper.getSmartAddress(
                originalAddress = trip.startLoc,
                lat = trip.startLat,
                lng = trip.startLon,
                favorites = repository.getSavedPlacesList(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val smartEnd = geocoderHelper.getSmartAddress(
                originalAddress = trip.endLoc,
                lat = trip.endLat,
                lng = trip.endLon,
                favorites = repository.getSavedPlacesList(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )
            item.copy(trip = trip.copy(startLoc = smartStart, endLoc = smartEnd))
        }.groupBy {
            // Normalize date to the start of the day
            val cal = Calendar.getInstance()
            cal.time = it.trip.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.map { (date, tripsOnDate) ->
            TripGroup(
                date = date,
                trips = tripsOnDate,
                totalDistance = tripsOnDate.sumOf { it.trip.distance }
            )
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unconfirmedTrips: StateFlow<List<TripWithVehicle>> = repository.unconfirmedTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedReviewTrips: StateFlow<List<TripGroup>> = combine(
        unconfirmedTrips,
        userPreferencesRepository.isSmartLocationEnabled,
        userPreferencesRepository.smartLocationRadius
    ) { trips, isSmartLocationEnabled, smartLocationRadius ->
        trips.map { item ->
            val trip = item.trip
            val smartStart = geocoderHelper.getSmartAddress(
                originalAddress = trip.startLoc,
                lat = trip.startLat,
                lng = trip.startLon,
                favorites = repository.getSavedPlacesList(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val smartEnd = geocoderHelper.getSmartAddress(
                originalAddress = trip.endLoc,
                lat = trip.endLat,
                lng = trip.endLon,
                favorites = repository.getSavedPlacesList(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )
            item.copy(trip = trip.copy(startLoc = smartStart, endLoc = smartEnd))
        }.groupBy {
            // Normalize date to the start of the day
            val cal = Calendar.getInstance()
            cal.time = it.trip.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.map { (date, tripsOnDate) ->
            TripGroup(
                date = date,
                trips = tripsOnDate.sortedByDescending { it.trip.date },
                totalDistance = tripsOnDate.sumOf { it.trip.distance }
            )
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // New StateFlow for total distance label
    val totalDistanceLabel: StateFlow<String> = confirmedTrips.map { filteredTrips ->
        val total = filteredTrips.sumOf { it.trip.distance }
        "Total: %.1f km".format(total)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Total: 0.0 km"
    )

    val isTracking: StateFlow<Boolean> = LocationService.isTracking
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Changed distance to StateFlow
    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance.asStateFlow()

    val isScheduleEnabled: StateFlow<Boolean> = userPreferencesRepository.isScheduleEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val scheduleSettings: StateFlow<ScheduleSettings> = userPreferencesRepository.scheduleSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScheduleSettings(ScheduleTarget.AUTOMATIC, emptyMap())
        )

    val scheduleSummary: StateFlow<String> = scheduleSettings.map { settings ->
        val enabledDays = settings.dailySchedules.filter { it.value.isEnabled }
        if (enabledDays.isEmpty()) return@map "No active days"

        val first = enabledDays.values.first()
        val allSame = enabledDays.values.all {
            it.startHour == first.startHour && it.startMinute == first.startMinute &&
                    it.endHour == first.endHour && it.endMinute == first.endMinute
        }

        val timeRange = "${formatTime(first.startHour, first.startMinute)}–${
            formatTime(
                first.endHour,
                first.endMinute
            )
        }"

        if (allSame) {
            if (enabledDays.size == 7) {
                "Daily, $timeRange"
            } else if (enabledDays.size == 5 &&
                enabledDays.containsKey(java.time.DayOfWeek.MONDAY) &&
                enabledDays.containsKey(java.time.DayOfWeek.FRIDAY)
            ) {
                "Mon–Fri, $timeRange"
            } else {
                "${enabledDays.size} days, $timeRange"
            }
        } else {
            "${enabledDays.size} days active"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private fun formatTime(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    val isAutoTrackingEnabled: StateFlow<Boolean> = userPreferencesRepository.isAutoTrackingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBluetoothTriggerEnabled: StateFlow<Boolean> = userPreferencesRepository.bluetoothTriggerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val bluetoothSummary: StateFlow<String> = userPreferencesRepository.selectedBluetoothDevices.map { devices ->
        if (devices.isEmpty()) "No devices selected"
        else if (devices.size == 1) "1 device configured"
        else "${devices.size} devices configured"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")


    // Event to request background location permission from the UI
    private val _permissionEvent = MutableSharedFlow<Unit>()
    val permissionEvent: SharedFlow<Unit> = _permissionEvent.asSharedFlow()

    val selectedBluetoothDevices: StateFlow<Set<String>> =
        userPreferencesRepository.selectedBluetoothDevices
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    val defaultIsBusiness: StateFlow<Boolean> = userPreferencesRepository.defaultIsBusiness
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _permissionsStatus = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissionsStatus: StateFlow<List<PermissionStatus>> = _permissionsStatus.asStateFlow()

    val permissionHealth: StateFlow<PermissionHealthState> = _permissionsStatus.map { statuses ->
        val missing = statuses.filter { !it.isGranted }
        if (missing.isEmpty()) PermissionHealthState.AllGranted
        else PermissionHealthState.Missing(missing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionHealthState.AllGranted)

    val isAllPermissionsGranted: StateFlow<Boolean> = permissionHealth.map { 
        it is PermissionHealthState.AllGranted 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val expenseTrackingEnabled: StateFlow<Boolean> =
        userPreferencesRepository.expenseTrackingEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    val expenseRatePerKm: StateFlow<Float> = userPreferencesRepository.expenseRatePerKm
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0f
        )

    val expenseCurrency: StateFlow<String> = userPreferencesRepository.expenseCurrency
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Currency.getInstance(Locale.getDefault()).symbol
        )

    val exportColumns: StateFlow<Set<String>> = userPreferencesRepository.exportColumns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = setOf("DATE", "TIME", "START_LOCATION", "END_LOCATION", "DISTANCE", "TYPE", "EXPENSES")
        )

    val isSmartLocationEnabled: StateFlow<Boolean> = userPreferencesRepository.isSmartLocationEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val smartLocationRadius: StateFlow<Int> = userPreferencesRepository.smartLocationRadius
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 150
        )

    val stillnessTimer: StateFlow<Int> = userPreferencesRepository.stillnessTimer
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 60
        )

    val minSpeed: StateFlow<Int> = userPreferencesRepository.minSpeed
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 15
        )

    val isOdometerModeEnabled: StateFlow<Boolean> = userPreferencesRepository.isOdometerModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isDistanceMonitoringEnabled: StateFlow<Boolean> = userPreferencesRepository.isDistanceMonitoringEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val distanceMonitoringRadius: StateFlow<Int> = userPreferencesRepository.distanceMonitoringRadius
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 50
        )

    val distanceMonitoringSummary: StateFlow<String> = distanceMonitoringRadius.map { radius ->
        "Wakes up every $radius meters"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val scheduleTicker = flow {
        while (true) {
            emit(Unit)
            delay(30.seconds) // Update every 30 seconds
        }
    }

    val isScheduleActive: StateFlow<Boolean> = combine(
        isScheduleEnabled,
        scheduleSettings,
        scheduleTicker
    ) { enabled, settings, _ ->
        if (!enabled) return@combine true
        
        val now = Calendar.getInstance()
        val dayOfWeek = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> java.time.DayOfWeek.MONDAY
            Calendar.TUESDAY -> java.time.DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> java.time.DayOfWeek.THURSDAY
            Calendar.FRIDAY -> java.time.DayOfWeek.FRIDAY
            Calendar.SATURDAY -> java.time.DayOfWeek.SATURDAY
            Calendar.SUNDAY -> java.time.DayOfWeek.SUNDAY
            else -> return@combine true
        }
        
        val daySchedule = settings.dailySchedules[dayOfWeek] ?: return@combine true
        if (!daySchedule.isEnabled) return@combine false
        
        val currentTime = LocalTime.of(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        val startTime = LocalTime.of(daySchedule.startHour, daySchedule.startMinute)
        val endTime = LocalTime.of(daySchedule.endHour, daySchedule.endMinute)
        
        !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // New: Total Expense for the entire filtered list
    val totalExpense: StateFlow<Float> = combine(
        confirmedTrips,
        expenseRatePerKm,
        expenseTrackingEnabled
    ) { trips, rate, enabled ->
        if (enabled) {
            val totalDistanceKm = trips.sumOf { it.trip.distance }.toFloat()
            totalDistanceKm * rate
        } else {
            0.0f
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0f
    )

    private val _pdfFileCreated = MutableSharedFlow<Uri>()
    val pdfFileCreated: SharedFlow<Uri> = _pdfFileCreated.asSharedFlow()

    val allDrivers: StateFlow<List<DriverEntity>> = favouritesRepository.getAllDrivers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCompanies: StateFlow<List<CompanyEntity>> = favouritesRepository.getAllCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allVehicles: StateFlow<List<VehicleEntity>> = favouritesRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedDriver by mutableStateOf<DriverEntity?>(null)
    var selectedCompany by mutableStateOf<CompanyEntity?>(null)
    var selectedVehicle by mutableStateOf<VehicleEntity?>(null)

    val hasDrivers: StateFlow<Boolean> = allDrivers.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hasCompanies: StateFlow<Boolean> = allCompanies.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hasVehicles: StateFlow<Boolean> = allVehicles.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val exportIncludeDriver: StateFlow<Boolean> = userPreferencesRepository.exportIncludeDriver
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val exportIncludeCompany: StateFlow<Boolean> = userPreferencesRepository.exportIncludeCompany
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val exportIncludeVehicle: StateFlow<Boolean> = userPreferencesRepository.exportIncludeVehicle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // Collect distance updates from LocationService whenever the ViewModel is active
        viewModelScope.launch {
            LocationService.distance.collect { newDistance ->
                _distance.value = newDistance // Update the StateFlow
            }
        }

        // Start monitoring on app start if enabled
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    "android.permission.ACCESS_BACKGROUND_LOCATION"
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Not needed for older versions
            }

            if (userPreferencesRepository.isAutoTrackingEnabled.first() && hasPermission) {
                AppLogger.log("TripsViewModel", "Initial check: Auto-tracking is enabled, starting monitoring.")
                Intent(context, LocationService::class.java).also {
                    it.action = LocationService.ACTION_START_MONITORING
                    context.startService(it)
                }
            }
        }

        userPreferencesRepository.defaultDriverId.onEach { driverId ->
            selectedDriver = if (driverId != -1) favouritesRepository.getDriverById(driverId) else null
        }.launchIn(viewModelScope)

        userPreferencesRepository.defaultCompanyId.onEach { companyId ->
            selectedCompany = if (companyId != -1) favouritesRepository.getCompanyById(companyId) else null
        }.launchIn(viewModelScope)

        userPreferencesRepository.defaultVehicleId.onEach { vehicleId ->
            selectedVehicle = if (vehicleId != -1) favouritesRepository.getVehicleById(vehicleId) else null
        }.launchIn(viewModelScope)
    }

    fun calculateDistance(startAddress: String, endAddress: String) {
        Log.d("TripsViewModel", "calculateDistance called with start: $startAddress, end: $endAddress")
        if (startAddress.isNotBlank() && endAddress.isNotBlank()) {
            isCalculating = true
            viewModelScope.launch(Dispatchers.IO) {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                try {
                    val startAddresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocationName(startAddress, 1) { addresses ->
                                continuation.resume(addresses)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(startAddress, 1)
                    }

                    val endAddresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocationName(endAddress, 1) { addresses ->
                                continuation.resume(addresses)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(endAddress, 1)
                    }

                    if (startAddresses != null && endAddresses != null && startAddresses.isNotEmpty() && endAddresses.isNotEmpty()) {
                        val start = startAddresses[0]
                        val end = endAddresses[0]
                        val distance = distanceRepository.getDrivingDistance(start.latitude, start.longitude, end.latitude, end.longitude)
                        withContext(Dispatchers.Main) {
                            if (distance != null) {
                                distanceInput = distance.toString()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TripsViewModel", "Error calculating distance", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isCalculating = false
                    }
                }
            }
        }
    }

    fun checkPermissions(context: Context) {
        val requirements = mutableListOf<PermissionRequirement>(
            PermissionRequirement.PreciseLocation
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requirements.add(PermissionRequirement.BackgroundLocation)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requirements.add(PermissionRequirement.Bluetooth)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requirements.add(PermissionRequirement.Notifications)
        }
        
        requirements.add(PermissionRequirement.BatteryOptimization)

        val statuses = requirements.map { req ->
            val isGranted = when (req) {
                PermissionRequirement.BatteryOptimization -> {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                }
                else -> {
                    req.permission?.let {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    } ?: true
                }
            }
            PermissionStatus(req, isGranted)
        }

        _permissionsStatus.value = statuses
    }

    fun startTracking(trigger: TripTrigger) {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, LocationService::class.java).apply {
            action = when (trigger) {
                TripTrigger.MANUAL -> LocationService.ACTION_START_MANUAL
                TripTrigger.AUTOMATIC -> LocationService.ACTION_START_AUTOMATIC
                TripTrigger.BLUETOOTH -> LocationService.ACTION_START_MANUAL // Or a new specific action
            }
            putExtra("trigger", trigger)
        }
        context.startService(intent)
    }

    fun stopTracking() {
        val context = getApplication<Application>().applicationContext
        Intent(context, LocationService::class.java).also {
            it.action = LocationService.ACTION_STOP
            context.startService(it)
        }
    }

    fun onToggleAutoTracking(
        checked: Boolean
    ) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoTrackingEnabled(checked)
            applyScheduleChanges()
        }
    }

    fun saveOrUpdateTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            if (trip.endOdometer != null && trip.vehicleId != null) {
                val vehicle = favouritesRepository.getVehicleById(trip.vehicleId)
                if (vehicle != null) {
                    favouritesRepository.updateVehicle(vehicle.copy(currentOdometer = trip.endOdometer))
                }
            }
            if (trip.id == 0L) {
                repository.insert(trip)
            } else {
                repository.updateTrip(trip)
            }
            if (trip.isConfirmed) {
                TripNotificationManager.cancelTripNotification(getApplication<Application>(), trip.id)
            }
        }
        _distance.value = 0.0 // Reset distance after adding trip
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            if (trip.endOdometer != null && trip.vehicleId != null) {
                val vehicle = favouritesRepository.getVehicleById(trip.vehicleId)
                if (vehicle != null) {
                    favouritesRepository.updateVehicle(vehicle.copy(currentOdometer = trip.endOdometer))
                }
            }
            repository.updateTrip(trip)
            if (trip.isConfirmed) {
                TripNotificationManager.cancelTripNotification(getApplication<Application>(), trip.id)
            }
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrip(trip)
            TripNotificationManager.cancelTripNotification(getApplication<Application>(), trip.id)
        }
    }

    fun deleteFilteredTrips() {
        viewModelScope.launch(Dispatchers.IO) {
            val tripsToDelete = confirmedTrips.first()
            repository.deleteTrips(tripsToDelete)
            tripsToDelete.forEach {
                TripNotificationManager.cancelTripNotification(getApplication<Application>(), it.trip.id)
            }
        }
    }

    fun approveTrip(trip: Trip, finalType: TripType, endOdometer: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val typeString = when (finalType) {
                TripType.BUSINESS -> "Business"
                TripType.PERSONAL -> "Personal"
                TripType.ALL -> trip.type // Fallback
            }
            
            var updatedTrip = trip.copy(type = typeString, isConfirmed = true)
            
            if (endOdometer != null && trip.vehicleId != null) {
                val vehicle = favouritesRepository.getVehicleById(trip.vehicleId)
                if (vehicle != null) {
                    val distance = endOdometer - vehicle.currentOdometer
                    updatedTrip = updatedTrip.copy(
                        distance = if (distance > 0) distance else updatedTrip.distance,
                        endOdometer = endOdometer
                    )
                    // Update vehicle odometer
                    favouritesRepository.updateVehicle(vehicle.copy(currentOdometer = endOdometer))
                }
            }
            
            repository.updateTrip(updatedTrip)
            TripNotificationManager.cancelTripNotification(getApplication<Application>(), trip.id)
        }
    }

    fun discardTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrip(trip)
            TripNotificationManager.cancelTripNotification(getApplication<Application>(), trip.id)
        }
    }

    suspend fun exportAllTripsToCsv(
        context: Context,
        driverName: String?,
        companyName: String?,
        vehicleName: String?
    ): Uri? = withContext(Dispatchers.IO) {
        val trips = confirmedTrips.first()
        val columns = exportColumns.first()
        val isExpenseEnabled = expenseTrackingEnabled.first() && columns.contains("EXPENSES")
        val rate = expenseRatePerKm.first()
        val includeDriver = exportIncludeDriver.first()
        val includeCompany = exportIncludeCompany.first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun escape(s: String?) = if (s == null) "" else "\"${s.replace("\"", "\"\"")}\""

        val headers = mutableListOf<String>()
        if (columns.contains("DATE")) headers.add("Date")
        if (columns.contains("TIME")) {
            headers.add("Start time")
            headers.add("End time")
        }
        if (columns.contains("START_LOCATION")) headers.add("Start Location")
        if (columns.contains("END_LOCATION")) headers.add("End Location")
        if (columns.contains("DISTANCE")) headers.add("Distance")
        if (columns.contains("TYPE")) headers.add("Type")
        if (isExpenseEnabled) headers.add("Expenses")
        if (includeDriver) headers.add("Driver")
        if (includeCompany) headers.add("Company")
        if (columns.contains("VEHICLE")) headers.add("Vehicle")

        val csvHeader = headers.joinToString(",") + "\n"

        val csvBody = trips.joinToString(separator = "\n") { item ->
            val trip = item.trip
            val row = mutableListOf<String>()

            if (columns.contains("DATE")) row.add(dateFormat.format(trip.date))
            if (columns.contains("TIME")) {
                row.add(timeFormat.format(trip.date))
                row.add(timeFormat.format(Date(trip.endDate)))
            }
            if (columns.contains("START_LOCATION")) row.add(escape(trip.startLoc))
            if (columns.contains("END_LOCATION")) row.add(escape(trip.endLoc))
            if (columns.contains("DISTANCE")) row.add("%.2f".format(trip.distance))
            if (columns.contains("TYPE")) row.add(trip.type)
            if (isExpenseEnabled) {
                val expense = trip.distance * rate
                row.add("%.2f".format(expense))
            }
            if (includeDriver) row.add(escape(driverName))
            if (includeCompany) row.add(escape(companyName))
            if (columns.contains("VEHICLE")) row.add(escape(item.vehicle?.licensePlate ?: vehicleName))
            row.joinToString(",")
        }

        val content = csvHeader + csvBody
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "tricktrack-trips_$timestamp.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(content)
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportTripsToPdf() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val trips = confirmedTrips.first()
            val exportSettings = exportColumns.first()
            val rate = expenseRatePerKm.first()
            val currency = expenseCurrency.first()
            val isExpenseEnabled = expenseTrackingEnabled.first() && exportSettings.contains("EXPENSES")
            val includeDriver = exportIncludeDriver.first()
            val includeCompany = exportIncludeCompany.first()
            val filter = filterState.first()

            val pdfFile = withContext(Dispatchers.IO) {
                PdfGenerator().generateTripReport(
                    context = context,
                    tripsWithVehicle = trips,
                    columns = exportSettings,
                    isExpenseEnabled = isExpenseEnabled,
                    expenseRate = rate,
                    expenseCurrency = currency,
                    driverName = if (includeDriver) selectedDriver?.name else null,
                    companyName = if (includeCompany) selectedCompany?.name else null,
                    vehicleName = if (filter.vehicleIds.size == 1) selectedVehicle?.licensePlate else null
                )
            }
            pdfFile?.let {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    it
                )
                _pdfFileCreated.emit(uri)
            }
        }
    }

    fun updateFilter(newFilterState: FilterState) {
        _filterState.value = newFilterState
    }

    fun removeFilter(filterState: FilterState) {
        _filterState.value = filterState
    }

    fun setBluetoothTriggerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBluetoothTriggerEnabled(enabled)
            applyScheduleChanges()
        }
    }

    fun toggleBluetoothDevice(address: String) {
        viewModelScope.launch {
            userPreferencesRepository.toggleBluetoothDevice(address)
        }
    }

    fun setDefaultTripType(isBusiness: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDefaultTripType(isBusiness)
        }
    }

    fun setExpenseTracking(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setExpenseTrackingEnabled(enabled)
        }
    }

    fun setExpenseRate(rate: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setExpenseRatePerKm(rate)
        }
    }

    fun setExpenseCurrency(currency: String) {
        viewModelScope.launch {
            userPreferencesRepository.setExpenseCurrency(currency)
        }
    }
    
    fun setExportColumns(columns: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setExportColumns(columns)
        }
    }

    fun setSmartLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSmartLocationEnabled(enabled)
        }
    }

    fun setSmartLocationRadius(radius: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setSmartLocationRadius(radius)
        }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setScheduleEnabled(enabled)
            applyScheduleChanges()
        }
    }

    fun updateScheduleSettings(settings: ScheduleSettings) {
        viewModelScope.launch {
            userPreferencesRepository.updateScheduleSettings(settings)
            userPreferencesRepository.setScheduleEnabled(true)
            applyScheduleChanges()
        }
    }

    fun applyScheduleChanges() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            Intent(context, LocationService::class.java).also {
                it.action = LocationService.ACTION_START_MONITORING
                context.startService(it)
            }
        }
    }

    fun setStillnessTimer(seconds: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setStillnessTimer(seconds)
        }
    }

    fun setMinSpeed(speed: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setMinSpeed(speed)
        }
    }

    fun toggleIncludeDriver() {
        viewModelScope.launch {
            userPreferencesRepository.setExportIncludeDriver(!exportIncludeDriver.first())
        }
    }

    fun toggleIncludeCompany() {
        viewModelScope.launch {
            userPreferencesRepository.setExportIncludeCompany(!exportIncludeCompany.first())
        }
    }

    fun toggleIncludeVehicle() {
        viewModelScope.launch {
            userPreferencesRepository.setExportIncludeVehicle(!exportIncludeVehicle.first())
        }
    }

    fun setOdometerModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setOdometerModeEnabled(enabled)
        }
    }

    fun setDistanceMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDistanceMonitoringEnabled(enabled)
            applyScheduleChanges()
        }
    }

    fun setDistanceMonitoringRadius(radius: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setDistanceMonitoringRadius(radius)
            applyScheduleChanges()
        }
    }
}
