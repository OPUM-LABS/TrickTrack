package ch.opum.tricktrack.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.opum.tricktrack.GeocoderHelper
import ch.opum.tricktrack.LocationService
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.ScheduleSettings
import ch.opum.tricktrack.data.ScheduleTarget
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.TripRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import ch.opum.tricktrack.logging.AppLogger
import ch.opum.tricktrack.ui.settings.PermissionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.DayOfWeek
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
    val endDate: Long? = null    // Timestamp for end of day
)

data class TripGroup(
    val date: Long,
    val trips: List<Trip>,
    val totalDistance: Double
)

class TripsViewModel(
    application: Application,
    private val repository: TripRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geocoderHelper: GeocoderHelper // Inject GeocoderHelper
) : AndroidViewModel(application) {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilterState()
    )

    val isFilterActive: StateFlow<Boolean> = _filterState.map {
        it.type != TripType.ALL || it.keyword.isNotEmpty() || it.startDate != null || it.endDate != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val confirmedTrips = combine(repository.confirmedTrips, _filterState) { allTrips, filter ->
        allTrips.filter { trip ->
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

            matchesType && matchesKeyword && matchesStartDate && matchesEndDate
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
        trips.map { trip ->
            val smartStart = geocoderHelper.getSmartAddress(
                originalAddress = trip.startLoc,
                lat = trip.startLat,
                lng = trip.startLon,
                favorites = repository.getAllSavedPlacesBlocking(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val smartEnd = geocoderHelper.getSmartAddress(
                originalAddress = trip.endLoc,
                lat = trip.endLat,
                lng = trip.endLon,
                favorites = repository.getAllSavedPlacesBlocking(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )
            trip.copy(startLoc = smartStart, endLoc = smartEnd)
        }.groupBy {
            // Normalize date to the start of the day
            val cal = Calendar.getInstance()
            cal.time = it.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.map { (date, tripsOnDate) ->
            TripGroup(
                date = date,
                trips = tripsOnDate,
                totalDistance = tripsOnDate.sumOf { it.distance }
            )
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unconfirmedTrips: StateFlow<List<Trip>> = repository.unconfirmedTrips
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
        trips.map { trip ->
            val smartStart = geocoderHelper.getSmartAddress(
                originalAddress = trip.startLoc,
                lat = trip.startLat,
                lng = trip.startLon,
                favorites = repository.getAllSavedPlacesBlocking(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val smartEnd = geocoderHelper.getSmartAddress(
                originalAddress = trip.endLoc,
                lat = trip.endLat,
                lng = trip.endLon,
                favorites = repository.getAllSavedPlacesBlocking(),
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )
            trip.copy(startLoc = smartStart, endLoc = smartEnd)
        }.groupBy {
            // Normalize date to the start of the day
            val cal = Calendar.getInstance()
            cal.time = it.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.map { (date, tripsOnDate) ->
            TripGroup(
                date = date,
                trips = tripsOnDate.sortedByDescending { it.date },
                totalDistance = tripsOnDate.sumOf { it.distance }
            )
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // New StateFlow for total distance label
    val totalDistanceLabel: StateFlow<String> = confirmedTrips.map { filteredTrips ->
        val total = filteredTrips.sumOf { it.distance }
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

    var showSummaryDialog by mutableStateOf(false)
        private set

    // Changed distance to StateFlow
    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance.asStateFlow()

    var startAddress by mutableStateOf("Loading...")
        private set

    var endAddress by mutableStateOf("Loading...")
        private set

    var startLat by mutableStateOf<Double?>(null)
        private set
    var startLon by mutableStateOf<Double?>(null)
        private set
    var endLat by mutableStateOf<Double?>(null)
        private set
    var endLon by mutableStateOf<Double?>(null)
        private set

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

    val isAutoTrackingEnabled: StateFlow<Boolean> = combine(
        userPreferencesRepository.isAutoTrackingEnabled,
        isScheduleEnabled,
        scheduleSettings
    ) { isAutoTracking, isSchedule, settings ->
        if (isSchedule) {
            settings.target == ScheduleTarget.AUTOMATIC || settings.target == ScheduleTarget.BOTH
        } else {
            isAutoTracking
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBluetoothTriggerEnabled: StateFlow<Boolean> = combine(
        userPreferencesRepository.bluetoothTriggerEnabled,
        isScheduleEnabled,
        scheduleSettings
    ) { isBluetoothOn, isSchedule, settings ->
        if (isSchedule) {
            settings.target == ScheduleTarget.BLUETOOTH || settings.target == ScheduleTarget.BOTH
        } else {
            isBluetoothOn
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    // Event to request background location permission from the UI
    private val _permissionEvent = MutableSharedFlow<Unit>()
    val permissionEvent: SharedFlow<Unit> = _permissionEvent.asSharedFlow()

    // Flag to indicate if a permission request is pending due to auto-tracking toggle
    private val _pendingPermissionRequest = MutableStateFlow(false)
    val pendingPermissionRequest: StateFlow<Boolean> = _pendingPermissionRequest.asStateFlow()

    private var distanceJob: Job? = null

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

    private val _permissionsStatus = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsStatus: StateFlow<List<PermissionItem>> = _permissionsStatus.asStateFlow()

    val isAllPermissionsGranted: StateFlow<Boolean> = _permissionsStatus.map { permissions ->
        permissions.all { it.isGranted }
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

    val isAutomaticSwitchEnabled: StateFlow<Boolean> = isScheduleEnabled.map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isBluetoothSwitchEnabled: StateFlow<Boolean> = isScheduleEnabled.map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isBluetoothDeviceSelectionEnabled: StateFlow<Boolean> = combine(
        isBluetoothTriggerEnabled,
        isScheduleEnabled,
        scheduleSettings
    ) { isBluetoothEnabled, isScheduleEnabled, settings ->
        isBluetoothEnabled || (isScheduleEnabled && (settings.target == ScheduleTarget.BLUETOOTH || settings.target == ScheduleTarget.BOTH))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    // New: Total Expense for the entire filtered list
    val totalExpense: StateFlow<Float> = combine(
        confirmedTrips,
        expenseRatePerKm,
        expenseTrackingEnabled
    ) { trips, rate, enabled ->
        if (enabled) {
            val totalDistanceKm = trips.sumOf { it.distance }.toFloat()
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
    }

    fun checkPermissions(context: Context) {
        val permissions = mutableListOf(
            PermissionItem(
                context.getString(R.string.permission_precise_location),
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(
                PermissionItem(
                    context.getString(R.string.permission_background_location),
                    ContextCompat.checkSelfPermission(
                        context,
                        "android.permission.ACCESS_BACKGROUND_LOCATION"
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(
                PermissionItem(
                    context.getString(R.string.permission_bluetooth),
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        // Add Notification Permission Check for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(
                PermissionItem(
                    context.getString(R.string.permission_notifications),
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        _permissionsStatus.value = permissions
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

        viewModelScope.launch {
            val startLocation = LocationService.startLocation.first()
            val endLocation = LocationService.lastLocation.first()
            val isSmartLocationEnabled = userPreferencesRepository.isSmartLocationEnabled.first()
            val smartLocationRadius = userPreferencesRepository.smartLocationRadius.first()
            val savedPlaces = repository.getAllSavedPlacesBlocking()

            startLat = startLocation?.latitude
            startLon = startLocation?.longitude
            endLat = endLocation?.latitude
            endLon = endLocation?.longitude

            val geocodedStartAddress = startLocation?.let {
                geocoderHelper.getAddressFromLocation(it.latitude, it.longitude)
            } ?: "Unknown Start"

            val geocodedEndAddress = endLocation?.let {
                geocoderHelper.getAddressFromLocation(it.latitude, it.longitude)
            } ?: "Unknown End"

            startAddress = geocoderHelper.getSmartAddress(
                originalAddress = geocodedStartAddress,
                lat = startLat,
                lng = startLon,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            endAddress = geocoderHelper.getSmartAddress(
                originalAddress = geocodedEndAddress,
                lat = endLat,
                lng = endLon,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            showSummaryDialog = true
        }
    }

    fun onToggleAutoTracking(
        checked: Boolean,
        hasBackgroundLocationPermission: Boolean
    ) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoTrackingEnabled(checked)
            applyScheduleChanges()
        }
    }

    fun addTrip(
        startLoc: String,
        endLoc: String,
        finalDistance: Double,
        type: String,
        description: String?,
        date: Date,
        endDate: Long,
        startLat: Double?,
        startLon: Double?,
        endLat: Double?,
        endLon: Double?,
        isConfirmed: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val distanceInKm = finalDistance / 1000.0
            repository.insert(
                Trip(
                    startLoc = startLoc,
                    endLoc = endLoc,
                    distance = distanceInKm,
                    type = type,
                    description = description,
                    date = date,
                    endDate = endDate,
                    startLat = startLat,
                    startLon = startLon,
                    endLat = endLat,
                    endLon = endLon,
                    isConfirmed = isConfirmed
                )
            )
        }
        showSummaryDialog = false
        _distance.value = 0.0 // Reset distance after adding trip
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTrip(trip)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrip(trip)
        }
    }

    fun deleteFilteredTrips() {
        viewModelScope.launch(Dispatchers.IO) {
            val tripsToDelete = confirmedTrips.first()
            repository.deleteTrips(tripsToDelete)
        }
    }

    fun approveTrip(trip: Trip, finalType: TripType) {
        viewModelScope.launch(Dispatchers.IO) {
            val typeString = when (finalType) {
                TripType.BUSINESS -> "Business"
                TripType.PERSONAL -> "Personal"
                TripType.ALL -> trip.type // Fallback
            }
            val updatedTrip = trip.copy(type = typeString, isConfirmed = true)
            repository.updateTrip(updatedTrip)
        }
    }

    fun discardTrip(trip: Trip) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrip(trip)
        }
    }

    fun dismissSummaryDialog() {
        showSummaryDialog = false
        _distance.value = 0.0 // Reset distance after dismissing dialog
    }

    suspend fun exportAllTripsToCsv(context: Context): Uri? = withContext(Dispatchers.IO) {
        val trips = confirmedTrips.first()
        val columns = exportColumns.first()
        val isExpenseEnabled = expenseTrackingEnabled.first() && columns.contains("EXPENSES")
        val rate = expenseRatePerKm.first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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

        val csvHeader = headers.joinToString(",") + "\n"

        val csvBody = trips.joinToString(separator = "\n") { trip ->
            fun escape(s: String) = "\"${s.replace("\"", "\"\"")}\""
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
            val exportSettings = userPreferencesRepository.exportColumns.first()
            val rate = expenseRatePerKm.first()
            val currency = expenseCurrency.first()
            val isExpenseEnabled = expenseTrackingEnabled.first() && exportSettings.contains("EXPENSES")

            val pdfFile = withContext(Dispatchers.IO) {
                PdfGenerator().generateTripReport(
                    context = context,
                    trips = trips,
                    columns = exportSettings,
                    isExpenseEnabled = isExpenseEnabled,
                    expenseRate = rate,
                    expenseCurrency = currency
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
}
