package ch.opum.tricktrack.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import ch.opum.tricktrack.MotionSensorInfo
import ch.opum.tricktrack.MovementInfo
import ch.opum.tricktrack.R
import ch.opum.tricktrack.TripNotificationManager
import ch.opum.tricktrack.util.DistanceFormatter
import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DistanceUnit
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.util.PolylineUtils
import kotlin.math.abs
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.ScheduleSettings
import ch.opum.tricktrack.data.ScheduleTarget
import ch.opum.tricktrack.data.ScheduleTypeTarget
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import java.io.File
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

enum class TripType {
    ALL, BUSINESS, PERSONAL
}

data class FilterState(
    val type: TripType = TripType.ALL,
    val keyword: String = "",
    val startDate: Long? = null, // Timestamp for start of day
    val endDate: Long? = null,    // Timestamp for end of day
    val vehicleIds: Set<Int> = emptySet(),
)

data class TripGroup(
    val date: Long,
    val trips: List<TripWithVehicle>,
    val totalDistance: Double,
)

enum class CalculationError {
    NO_INTERNET,
    ADDRESS_NOT_FOUND,
    ROUTING_FAILED
}

class TripsViewModel(
    application: Application,
    private val repository: TripRepository,
    val userPreferencesRepository: UserPreferencesRepository,
    private val geocoderHelper: GeocoderHelper, // Inject GeocoderHelper
    private val favouritesRepository: FavouritesRepository,
) : AndroidViewModel(application) {

    private val distanceRepository = DistanceRepository(application)
    var isCalculating by mutableStateOf(value = false)
    var distanceInput by mutableStateOf("")

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isResolvingAddresses = false

    init {
        registerNetworkCallback()
        resolvePendingOfflineAddresses()
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val networkRequest = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        resolvePendingOfflineAddresses()
                    }
                }
                networkCallback = callback
                connectivityManager.registerNetworkCallback(networkRequest, callback)
            }
        } catch (e: Exception) {
            AppLogger.log("TripsViewModel", "Error registering network callback: ${e.message}")
        }
    }

    override fun onCleared() {
        try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            networkCallback = null
        } catch (_: Exception) {}
    }

    suspend fun getAddressFromLocation(lat: Double?, lng: Double?): String {
        return geocoderHelper.getAddressFromLocation(lat, lng)
    }

    private val pendingAddressMarkers: Set<String> by lazy {
        val context = getApplication<Application>()
        val supportedLanguageTags = listOf("en", "de", "fr", "it")
        val markers = mutableSetOf("Unknown Start", "Unknown End")

        // 1. Current runtime configuration locale
        try {
            val current = context.getString(R.string.address_pending_offline)
            markers.add(current)
            val currentPrefix = current.substringBefore(" (").trim()
            if (currentPrefix.isNotEmpty()) markers.add(currentPrefix)
        } catch (_: Exception) {}

        // 2. Prepopulate across all bundled app locales for cross-language compatibility
        for (lang in supportedLanguageTags) {
            try {
                val config = Configuration(context.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(lang))
                }
                val localizedString = context.createConfigurationContext(config)
                    .getString(R.string.address_pending_offline)
                val prefix = localizedString.substringBefore(" (").trim()
                if (prefix.isNotEmpty()) {
                    markers.add(prefix)
                }
                markers.add(localizedString)
            } catch (_: Exception) {}
        }
        markers
    }

    fun isPendingAddress(address: String?): Boolean {
        if (address.isNullOrBlank()) return false
        return pendingAddressMarkers.any { address.contains(it, ignoreCase = true) }
    }

    fun resolvePendingOfflineAddresses() {
        if (!geocoderHelper.isNetworkAvailable()) return

        viewModelScope.launch(Dispatchers.IO) {
            if (isResolvingAddresses) return@launch
            isResolvingAddresses = true
            try {
                val confirmed = repository.confirmedTrips.first().map { it.trip }
                val unconfirmed = repository.unconfirmedTrips.first().map { it.trip }
                val allTrips = confirmed + unconfirmed
                val pendingTrips = allTrips.filter {
                    (isPendingAddress(it.startLoc) && it.startLat != null && it.startLon != null) ||
                            (isPendingAddress(it.endLoc) && it.endLat != null && it.endLon != null)
                }

                if (pendingTrips.isEmpty()) return@launch

                val isSmartLocationEnabled = userPreferencesRepository.isSmartLocationEnabled.first()
                val smartLocationRadius = userPreferencesRepository.smartLocationRadius.first()
                val savedPlaces = repository.getSavedPlacesList()
                var anyUpdated = false

                pendingTrips.forEach { trip ->
                    val startNeedsResolution = isPendingAddress(trip.startLoc) && trip.startLat != null && trip.startLon != null
                    val endNeedsResolution = isPendingAddress(trip.endLoc) && trip.endLat != null && trip.endLon != null

                    if (startNeedsResolution || endNeedsResolution) {
                        var newStart = trip.startLoc
                        var newEnd = trip.endLoc

                        if (startNeedsResolution) {
                            val resolvedStart = geocoderHelper.getAddressFromLocation(trip.startLat, trip.startLon)
                            if (resolvedStart.isNotBlank() && !isPendingAddress(resolvedStart)) {
                                newStart = geocoderHelper.getSmartAddress(
                                    originalAddress = resolvedStart,
                                    lat = trip.startLat,
                                    lng = trip.startLon,
                                    favorites = savedPlaces,
                                    isEnabled = isSmartLocationEnabled,
                                    radius = smartLocationRadius
                                )
                            }
                        }

                        if (endNeedsResolution) {
                            val resolvedEnd = geocoderHelper.getAddressFromLocation(trip.endLat, trip.endLon)
                            if (resolvedEnd.isNotBlank() && !isPendingAddress(resolvedEnd)) {
                                newEnd = geocoderHelper.getSmartAddress(
                                    originalAddress = resolvedEnd,
                                    lat = trip.endLat,
                                    lng = trip.endLon,
                                    favorites = savedPlaces,
                                    isEnabled = isSmartLocationEnabled,
                                    radius = smartLocationRadius
                                )
                            }
                        }

                        if (newStart != trip.startLoc || newEnd != trip.endLoc) {
                            val updatedTrip = trip.copy(startLoc = newStart, endLoc = newEnd)
                            repository.updateTrip(updatedTrip)
                            anyUpdated = true
                        }
                    }
                }

                if (anyUpdated) {
                    AppLogger.log("TripsViewModel", "Auto-resolved pending offline addresses upon network reconnect.")
                }
            } catch (e: Exception) {
                AppLogger.log("TripsViewModel", "Error resolving pending offline addresses: ${e.message}")
            } finally {
                isResolvingAddresses = false
            }
        }
    }

    val distanceUnit: StateFlow<DistanceUnit> = userPreferencesRepository.distanceUnit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DistanceUnit.KM
        )

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilterState(),
    )

    val isFilterActive: StateFlow<Boolean> = _filterState.map {
        (it.type != TripType.ALL) || it.keyword.isNotEmpty() || (it.startDate != null) || (it.endDate != null) || it.vehicleIds.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    private val _pendingDeletedTrips = MutableStateFlow<List<TripWithVehicle>>(emptyList())
    val pendingDeletedTrips: StateFlow<List<TripWithVehicle>> = _pendingDeletedTrips.asStateFlow()

    val confirmedTrips = combine(repository.confirmedTrips, _filterState, _pendingDeletedTrips) { allTrips, filter, pending ->
        val pendingIds = pending.map { it.trip.id }.toSet()
        val activeTrips = allTrips.filter { it.trip.id !in pendingIds }
        activeTrips.filter { tripWithVehicle ->
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
                (trip.vehicleId != null) && filter.vehicleIds.contains(trip.vehicleId)
            }

            matchesType && matchesKeyword && matchesStartDate && matchesEndDate && matchesVehicle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val groupedTrips: StateFlow<List<TripGroup>> = combine(
        confirmedTrips,
        userPreferencesRepository.isSmartLocationEnabled,
        userPreferencesRepository.smartLocationRadius
    ) { trips, isSmartLocationEnabled, smartLocationRadius ->
        val savedPlaces = repository.getSavedPlacesList()
        trips.asSequence().map { item ->
            val trip = item.trip
            val smartStart = geocoderHelper.getSmartAddress(
                originalAddress = trip.startLoc,
                lat = trip.startLat,
                lng = trip.startLon,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val smartEnd = geocoderHelper.getSmartAddress(
                originalAddress = trip.endLoc,
                lat = trip.endLat,
                lng = trip.endLon,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )
            item.copy(trip = trip.copy(startLoc = smartStart, endLoc = smartEnd))
        }.groupBy {
            // Normalize date to the start of the day
            val cal = Calendar.getInstance()
            cal.apply {
                time = it.trip.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
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
        initialValue = emptyList(),
    )

    val unconfirmedTrips: StateFlow<List<TripWithVehicle>> = repository.unconfirmedTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _pendingDiscardedTrips = MutableStateFlow<List<TripWithVehicle>>(emptyList())
    val pendingDiscardedTrips: StateFlow<List<TripWithVehicle>> = _pendingDiscardedTrips.asStateFlow()

    val groupedReviewTrips: StateFlow<List<TripGroup>> = combine(
        unconfirmedTrips,
        _pendingDiscardedTrips,
        userPreferencesRepository.isSmartLocationEnabled,
        userPreferencesRepository.smartLocationRadius
    ) { trips, pending, isSmartLocationEnabled, smartLocationRadius ->
        val pendingIds = pending.map { it.trip.id }.toSet()
        val activeTrips = trips.filter { it.trip.id !in pendingIds }
        val savedPlaces = repository.getSavedPlacesList()
        activeTrips.map { item ->
            val trip = item.trip
            val smartStart = geocoderHelper.getSmartAddress(
                originalAddress = trip.startLoc,
                lat = trip.startLat,
                lng = trip.startLon,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val smartEnd = geocoderHelper.getSmartAddress(
                originalAddress = trip.endLoc,
                lat = trip.endLat,
                lng = trip.endLon,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )
            item.copy(trip = trip.copy(startLoc = smartStart, endLoc = smartEnd))
        }.groupBy {
            // Normalize date to the start of the day
            val cal = Calendar.getInstance()
            cal.apply {
                time = it.trip.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
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
        initialValue = emptyList(),
    )

    val totalDistanceFormatted: StateFlow<String> = combine(confirmedTrips, distanceUnit) { filteredTrips, unit ->
        val total = filteredTrips.sumOf { it.trip.distance }
        DistanceFormatter.formatShort(total, unit)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val tripCount: StateFlow<Int> = confirmedTrips.map { filteredTrips ->
        filteredTrips.size
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val allConfirmedTripsCount: StateFlow<Int> = repository.confirmedTrips.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val isTracking: StateFlow<Boolean> = LocationService.isTracking
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val lastMovementInfo: StateFlow<MovementInfo?> = LocationService.lastMovementInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val motionSensorInfo: StateFlow<MotionSensorInfo?> = LocationService.motionSensorInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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
            initialValue = ScheduleSettings(target = ScheduleTarget.AUTOMATIC, dailySchedules = emptyMap())
        )

    val scheduleSummary: StateFlow<String> = scheduleSettings.map { settings ->
        val enabledDays = settings.dailySchedules.filter { it.value.isEnabled }
        if (enabledDays.isEmpty()) return@map getApplication<Application>().getString(R.string.settings_schedule_no_active_days)

        if (!settings.isCustomizeIndividualDays) {
            val timeRange = "${formatTime(settings.globalStartHour, settings.globalStartMinute)}–${formatTime(settings.globalEndHour, settings.globalEndMinute)}"
            when (enabledDays.size) {
                7 -> getApplication<Application>().getString(R.string.settings_schedule_daily, timeRange)
                5 -> if (enabledDays.containsKey(DayOfWeek.MONDAY) &&
                        enabledDays.containsKey(DayOfWeek.FRIDAY)) {
                    getApplication<Application>().getString(R.string.settings_schedule_mon_fri, timeRange)
                } else {
                    getApplication<Application>().getString(R.string.settings_schedule_days_count, enabledDays.size, timeRange)
                }
                else -> getApplication<Application>().getString(R.string.settings_schedule_days_count, enabledDays.size, timeRange)
            }
        } else {
            getApplication<Application>().getString(R.string.settings_schedule_days_active, enabledDays.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private fun formatTime(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    val isAutoTrackingEnabled: StateFlow<Boolean> = userPreferencesRepository.isAutoTrackingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val isBluetoothTriggerEnabled: StateFlow<Boolean> = userPreferencesRepository.bluetoothTriggerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val bluetoothSummary: StateFlow<String> = userPreferencesRepository.selectedBluetoothDevices.map { devices ->
        val context = getApplication<Application>()
        if (devices.isEmpty()) context.getString(R.string.settings_bluetooth_summary_aa_only)
        else if (devices.size == 1) context.getString(R.string.settings_bluetooth_summary_one_plus_aa)
        else context.getString(R.string.settings_bluetooth_summary_many_plus_aa, devices.size)
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
        val hasMissing = statuses.any { !it.isGranted }
        if (hasMissing) PermissionHealthState.Missing
        else PermissionHealthState.AllGranted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PermissionHealthState.AllGranted)

    val isAllPermissionsGranted: StateFlow<Boolean> = permissionHealth.map { 
        it is PermissionHealthState.AllGranted 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasCompletedOnboarding: StateFlow<Boolean> = userPreferencesRepository.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setHasCompletedOnboarding(completed: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHasCompletedOnboarding(completed)
        }
    }

    val showSettingsHelp: StateFlow<Boolean> = userPreferencesRepository.showSettingsHelp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showInlineHelpHint: StateFlow<Boolean> = combine(
        userPreferencesRepository.showSettingsHelp,
        userPreferencesRepository.hasInteractedWithHelp
    ) { showHelp, interacted ->
        showHelp && !interacted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleShowSettingsHelp() {
        viewModelScope.launch {
            val current = showSettingsHelp.value
            userPreferencesRepository.setShowSettingsHelp(!current)
            userPreferencesRepository.setHasInteractedWithHelp(true)
        }
    }

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

    val minTripDistance: StateFlow<Int> = userPreferencesRepository.minTripDistance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 100
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

    val distanceMonitoringSummary: StateFlow<String> = combine(distanceMonitoringRadius, distanceUnit) { radius, unit ->
        val displayRadius = DistanceFormatter.convertMetersToDisplayRadius(radius, unit)
        val unitLabel = if (unit == DistanceUnit.KM) {
            getApplication<Application>().getString(R.string.unit_meters)
        } else {
            getApplication<Application>().getString(R.string.unit_feet)
        }
        getApplication<Application>().getString(R.string.settings_wakes_up_every_meters, displayRadius, unitLabel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val themeMode: StateFlow<String> = userPreferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "SYSTEM"
        )

    val mapTheme: StateFlow<String> = userPreferencesRepository.mapTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "AUTO"
        )

    val accentColorHex: StateFlow<Long> = userPreferencesRepository.accentColorHex
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0xFF6750A4L
        )

    val isDynamicColorEnabled: StateFlow<Boolean> = userPreferencesRepository.isDynamicColorEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )

    val specialTheme: StateFlow<String> = userPreferencesRepository.specialTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "NONE"
        )

    val isWinterModeEnabled: StateFlow<Boolean> = userPreferencesRepository.isWinterModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val scheduleTicker = flow {
        while (true) {
            emit(Unit)
            delay(30.seconds) // Update every 30 seconds
        }
    }

    val activeScheduleTypeTarget: StateFlow<ScheduleTypeTarget> = combine(
        isScheduleEnabled,
        scheduleSettings,
        scheduleTicker
    ) { enabled, settings, _ ->
        if (!enabled) return@combine ScheduleTypeTarget.NONE

        val now = Calendar.getInstance()
        val dayOfWeek = when (now[Calendar.DAY_OF_WEEK]) {
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            else -> return@combine ScheduleTypeTarget.NONE
        }

        val daySchedule = settings.dailySchedules[dayOfWeek] ?: return@combine ScheduleTypeTarget.NONE
        if (!daySchedule.isEnabled) return@combine settings.outsideTarget

        val currentTime = LocalTime.of(now[Calendar.HOUR_OF_DAY], now[Calendar.MINUTE])
        val (startH, startM) = if (settings.isCustomizeIndividualDays) {
            daySchedule.startHour to daySchedule.startMinute
        } else {
            settings.globalStartHour to settings.globalStartMinute
        }
        val (endH, endM) = if (settings.isCustomizeIndividualDays) {
            daySchedule.endHour to daySchedule.endMinute
        } else {
            settings.globalEndHour to settings.globalEndMinute
        }

        val startTime = LocalTime.of(startH, startM)
        val endTime = LocalTime.of(endH, endM)

        val isWithinTime = if (startTime.isBefore(endTime) || startTime == endTime) {
            !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime)
        } else {
            !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime)
        }

        if (isWithinTime) settings.insideTarget else settings.outsideTarget
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleTypeTarget.NONE)

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

    private suspend fun resolveCoordinates(
        address: String,
        savedPlaces: List<SavedPlace>,
        biasLat: Double? = null,
        biasLon: Double? = null
    ): Pair<Double, Double>? {
        val trimmed = address.trim()
        val matchedPlace = savedPlaces.firstOrNull {
            it.name.equals(trimmed, ignoreCase = true) ||
            it.address.equals(trimmed, ignoreCase = true) ||
            "${it.name}, ${it.address}".equals(trimmed, ignoreCase = true)
        }
        if (matchedPlace != null && (abs(matchedPlace.latitude) > 0.001 || abs(matchedPlace.longitude) > 0.001)) {
            return Pair(matchedPlace.latitude, matchedPlace.longitude)
        }
        return geocoderHelper.getCoordinatesFromAddress(trimmed, biasLat, biasLon)
    }

    fun calculateDistance(
        startAddress: String,
        endAddress: String,
        startCoordsBias: Pair<Double, Double>? = null,
        endCoordsBias: Pair<Double, Double>? = null,
        onSuccess: ((distanceKm: Double, startLat: Double, startLon: Double, endLat: Double, endLon: Double, routePolyline: String?) -> Unit)? = null,
        onError: ((reason: CalculationError) -> Unit)? = null
    ) {
        Log.d("TripsViewModel", "calculateDistance called with start: $startAddress, end: $endAddress")
        if (startAddress.isBlank() || endAddress.isBlank()) {
            return
        }

        if (!geocoderHelper.isNetworkAvailable()) {
            onError?.invoke(CalculationError.NO_INTERNET)
            return
        }

        isCalculating = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedPlaces = repository.getSavedPlacesList()

                // 1. Resolve start coordinates
                val startCoords = resolveCoordinates(startAddress, savedPlaces)
                    ?: (if (startCoordsBias != null && (abs(startCoordsBias.first) > 0.001 || abs(startCoordsBias.second) > 0.001)) startCoordsBias else null)

                // 2. Resolve end coordinates (proximity biased first, fallback to unbiased)
                val endCoords = (resolveCoordinates(endAddress, savedPlaces, startCoords?.first, startCoords?.second)
                    ?: resolveCoordinates(endAddress, savedPlaces, null, null))
                    ?: (if (endCoordsBias != null && (abs(endCoordsBias.first) > 0.001 || abs(endCoordsBias.second) > 0.001)) endCoordsBias else null)

                if (startCoords == null || endCoords == null) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke(CalculationError.ADDRESS_NOT_FOUND)
                    }
                    return@launch
                }

                // 3. Request driving distance and route geometry from OSRM
                val distance = distanceRepository.getDrivingDistance(
                    startCoords.first,
                    startCoords.second,
                    endCoords.first,
                    endCoords.second
                )

                if (distance == null) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke(CalculationError.ROUTING_FAILED)
                    }
                    return@launch
                }

                val roadPoints = distanceRepository.getOsrmRouteGeometry(
                    startCoords.first,
                    startCoords.second,
                    endCoords.first,
                    endCoords.second
                )
                val encodedPolyline = if (!roadPoints.isNullOrEmpty()) {
                    PolylineUtils.encode(roadPoints)
                } else null

                withContext(Dispatchers.Main) {
                    val unit = distanceUnit.value
                    val converted = DistanceFormatter.convert(distance, unit)
                    distanceInput = "%.2f".format(converted)
                    onSuccess?.invoke(
                        distance,
                        startCoords.first,
                        startCoords.second,
                        endCoords.first,
                        endCoords.second,
                        encodedPolyline
                    )
                }
            } catch (e: Exception) {
                Log.e("TripsViewModel", "Error calculating distance", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(CalculationError.ROUTING_FAILED)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isCalculating = false
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
                favouritesRepository.getVehicleById(trip.vehicleId)?.let { vehicle ->
                    favouritesRepository.updateVehicle(vehicle.copy(currentOdometer = maxOf(vehicle.currentOdometer, trip.endOdometer)))
                }
            }
            if (trip.id == 0L) {
                repository.insert(trip)
            } else {
                repository.updateTrip(trip)
            }
            if (trip.isConfirmed) {
                TripNotificationManager.cancelTripNotification(getApplication(), trip.id)
            }
        }
        _distance.value = 0.0 // Reset distance after adding trip
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            if (trip.endOdometer != null && trip.vehicleId != null) {
                favouritesRepository.getVehicleById(trip.vehicleId)?.let { vehicle ->
                    favouritesRepository.updateVehicle(vehicle.copy(currentOdometer = maxOf(vehicle.currentOdometer, trip.endOdometer)))
                }
            }
            repository.updateTrip(trip)
            if (trip.isConfirmed) {
                TripNotificationManager.cancelTripNotification(getApplication(), trip.id)
            }
        }
    }

    fun updateTripPolyline(tripId: Long, routePolyline: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trip = repository.getTripById(tripId)
            if (trip != null && trip.routePolyline.isNullOrBlank()) {
                repository.updateTrip(trip.copy(routePolyline = routePolyline))
            }
        }
    }

    fun updateTripResolvedData(
        tripId: Long,
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        polyline: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trip = repository.getTripById(tripId)
            if (trip != null) {
                val updated = trip.copy(
                    startLat = startLat,
                    startLon = startLon,
                    endLat = endLat,
                    endLon = endLon,
                    routePolyline = polyline
                )
                if (updated != trip) {
                    repository.updateTrip(updated)
                }
            }
        }
    }

    fun refreshTripMap(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            DistanceRepository.clearCache()
            val trip = repository.getTripById(tripId)
            if (trip != null) {
                val resetTrip = trip.copy(
                    startLat = null,
                    startLon = null,
                    endLat = null,
                    endLon = null,
                    routePolyline = null
                )
                repository.updateTrip(resetTrip)
            }
        }
    }

    fun stageDiscardTrip(tripWithVehicle: TripWithVehicle) {
        _pendingDiscardedTrips.update { current -> current + tripWithVehicle }
    }

    fun undoDiscardTrips() {
        _pendingDiscardedTrips.value = emptyList()
    }

    fun commitPendingDiscards() {
        val toDelete = _pendingDiscardedTrips.value
        if (toDelete.isNotEmpty()) {
            _pendingDiscardedTrips.value = emptyList()
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteTrips(toDelete)
                toDelete.forEach {
                    TripNotificationManager.cancelTripNotification(getApplication(), it.trip.id)
                }
            }
        }
    }

    fun stageDeleteTrip(tripWithVehicle: TripWithVehicle) {
        _pendingDeletedTrips.update { current -> current + tripWithVehicle }
    }

    fun undoDeleteTrips() {
        _pendingDeletedTrips.value = emptyList()
    }

    fun commitPendingDeletions() {
        val toDelete = _pendingDeletedTrips.value
        if (toDelete.isNotEmpty()) {
            _pendingDeletedTrips.value = emptyList()
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteTrips(toDelete)
                toDelete.forEach {
                    TripNotificationManager.cancelTripNotification(getApplication(), it.trip.id)
                }
            }
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrip(trip)
            TripNotificationManager.cancelTripNotification(getApplication(), trip.id)
        }
    }

    fun deleteFilteredTrips() {
        viewModelScope.launch(Dispatchers.IO) {
            val tripsToDelete = confirmedTrips.first()
            repository.deleteTrips(tripsToDelete)
            tripsToDelete.forEach {
                TripNotificationManager.cancelTripNotification(getApplication(), it.trip.id)
            }
        }
    }

    fun approveTrip(trip: Trip, finalType: TripType, endOdometer: Double? = null, description: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val typeString = when (finalType) {
                TripType.BUSINESS -> "Business"
                TripType.PERSONAL -> "Personal"
                TripType.ALL -> trip.type // Fallback
            }
            
            var updatedTrip = trip.copy(type = typeString, isConfirmed = true, description = description)
            
            if (endOdometer != null && trip.vehicleId != null) {
                val vehicle = favouritesRepository.getVehicleById(trip.vehicleId)
                if (vehicle != null) {
                    val distance = (endOdometer - vehicle.currentOdometer).coerceAtLeast(0.0)
                    updatedTrip = updatedTrip.copy(
                        distance = distance,
                        endOdometer = endOdometer
                    )
                    // Update vehicle odometer to the higher reading
                    favouritesRepository.updateVehicle(vehicle.copy(currentOdometer = maxOf(vehicle.currentOdometer, endOdometer)))
                }
            }
            
            repository.updateTrip(updatedTrip)
            TripNotificationManager.cancelTripNotification(getApplication(), trip.id)
        }
    }

    suspend fun exportAllTripsToCsv(
        context: Context,
        driverName: String?,
        companyName: String?,
        vehicleName: String?,
        exportAll: Boolean = false
    ): Uri? = withContext(Dispatchers.IO) {
        val trips = if (exportAll) repository.confirmedTrips.first() else confirmedTrips.first()
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

    fun exportTripsToPdf(exportAll: Boolean = false) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val trips = if (exportAll) repository.confirmedTrips.first() else confirmedTrips.first()
            val exportSettings = exportColumns.first()
            val rate = expenseRatePerKm.first()
            val currency = expenseCurrency.first()
            val isExpenseEnabled = expenseTrackingEnabled.first() && exportSettings.contains("EXPENSES")
            val includeDriver = exportIncludeDriver.first()
            val includeCompany = exportIncludeCompany.first()
            val includeVehicle = exportIncludeVehicle.first()
            val filter = filterState.first()

            val selectedVeh = if (includeVehicle) {
                selectedVehicle ?: if (filter.vehicleIds.size == 1) {
                    allVehicles.first().find { it.id == filter.vehicleIds.first() }
                } else null
            } else null

            val activeVehicleName = selectedVeh?.licensePlate
            val activeVehicleBrand = selectedVeh?.brand

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
                    vehicleName = activeVehicleName,
                    vehicleBrand = activeVehicleBrand,
                    distanceUnit = distanceUnit.value
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

    fun setMinTripDistance(distanceMeters: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setMinTripDistance(distanceMeters)
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

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            userPreferencesRepository.setDistanceUnit(unit)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setMapTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.setMapTheme(theme)
        }
    }

    fun setAccentColorHex(colorLong: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setAccentColorHex(colorLong)
        }
    }

    fun setIsDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setIsDynamicColorEnabled(enabled)
        }
    }

    fun setSpecialTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSpecialTheme(theme)
        }
    }

    fun setWinterModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setWinterModeEnabled(enabled)
        }
    }
}
