package ch.opum.tricktrack

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ch.opum.tricktrack.data.ScheduleTarget
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.logging.AppLogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import ch.opum.tricktrack.TripNotificationManager
import ch.opum.tricktrack.GeocoderHelper
import ch.opum.tricktrack.ui.TripTrigger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val applicationScope by lazy { (application as TripApplication).applicationScope }
    private val userPreferencesRepository by lazy { (application as TripApplication).userPreferencesRepository }
    private val geocoderHelper by lazy { (application as TripApplication).geocoderHelper }
    private val bluetoothRepository by lazy { (application as TripApplication).bluetoothRepository }
    private var tripStartDate: Date? = null
    private var isManualTrip: Boolean = false
    private var isBluetoothTriggeredTrip: Boolean = false
    private var isMonitoring: Boolean = false // Indicates if we are actively monitoring for movement (not tracking a trip)
    private var stillnessTimer: CountDownTimer? = null
    private var previousMonitoringLocation: Location? = null
    private var highSpeedCounter = 0
    private var potentialTripStartLocation: Location? = null
    private var isStartingTrip = false
    private lateinit var notificationManager: NotificationManager


    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        TripNotificationManager.createNotificationChannel(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (isMonitoring) {
                        handleMonitoringLocation(location)
                    } else if (_isTracking.value) { // Only handle trip location if a trip is active
                        handleTripLocation(location)
                    }
                }
            }
        }
        AppLogger.log("LocationService", "onCreate")
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (_isTracking.value && _currentTripTrigger.value == TripTrigger.MANUAL && intent?.action == ACTION_BLUETOOTH_CONNECTED) {
            AppLogger.log("LocationService", "Ignoring Bluetooth connection because a manual trip is in progress.")
            return START_STICKY
        }

        val trigger = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra("trigger", TripTrigger::class.java)
        } else {
            intent?.getSerializableExtra("trigger") as? TripTrigger
        }

        if (trigger != null) {
            _currentTripTrigger.value = trigger
        }

        when (intent?.action) {
            ACTION_START_MONITORING, // Initial start or schedule change
            ACTION_BLUETOOTH_CONNECTED,
            ACTION_BLUETOOTH_DISCONNECTED -> {
                applicationScope.launch {
                    evaluateTrackingState()
                }
            }
            ACTION_START_MANUAL, ACTION_START_AUTOMATIC -> { // Manual start from UI
                isManualTrip = intent.action == ACTION_START_MANUAL
                isBluetoothTriggeredTrip = false
                applicationScope.launch {
                    // Ensure any monitoring is stopped before starting a trip
                    if (isMonitoring) {
                        stopMonitoringInternal()
                    }
                    startTrip()
                }
            }
            ACTION_STOP -> { // Manual stop from UI or stillness timer
                applicationScope.launch {
                    stopTripAndPrepareForSummary(true)
                }
            }
            ACTION_STOP_MONITORING -> { // Explicit stop monitoring command
                applicationScope.launch {
                    stopMonitoring() // This stops the service entirely
                }
            }
        }
        return START_STICKY
    }

    private suspend fun evaluateTrackingState() {
        // If a manual trip is active, we don't interfere with it.
        if (_isTracking.value && _currentTripTrigger.value == TripTrigger.MANUAL) {
            AppLogger.log("LocationService", "Manual trip active, not evaluating automatic/bluetooth state.")
            return
        }

        val isBtTriggerEnabled = userPreferencesRepository.bluetoothTriggerEnabled.first()
        val isAutoTrackingEnabled = userPreferencesRepository.isAutoTrackingEnabled.first()
        val isScheduleEnabled = userPreferencesRepository.isScheduleEnabled.first()
        val scheduleSettings = userPreferencesRepository.scheduleSettings.first()
        val scheduleTarget = scheduleSettings.target

        val selectedDevices = userPreferencesRepository.selectedBluetoothDevices.first()
        val isAnySelectedDeviceConnected = selectedDevices.any { bluetoothRepository.isDeviceConnected(it) }

        // Determine if Bluetooth or Auto tracking should be active based on schedule or direct settings
        val shouldBluetoothBeActive = if (isScheduleEnabled) {
            isWithinSchedule() && (scheduleTarget == ScheduleTarget.BLUETOOTH || scheduleTarget == ScheduleTarget.BOTH)
        } else {
            isBtTriggerEnabled
        }
        val shouldAutoTrackBeActive = if (isScheduleEnabled) {
            isWithinSchedule() && (scheduleTarget == ScheduleTarget.AUTOMATIC || scheduleTarget == ScheduleTarget.BOTH)
        } else {
            isAutoTrackingEnabled
        }

        AppLogger.log("LocationService", "Evaluating tracking state:")
        AppLogger.log("LocationService", "  shouldBluetoothBeActive: $shouldBluetoothBeActive")
        AppLogger.log("LocationService", "  shouldAutoTrackBeActive: $shouldAutoTrackBeActive")
        AppLogger.log("LocationService", "  isAnySelectedDeviceConnected: $isAnySelectedDeviceConnected")
        AppLogger.log("LocationService", "  _isTracking.value (trip active): ${_isTracking.value}")
        AppLogger.log("LocationService", "  isMonitoring (monitoring active): $isMonitoring")

        if (shouldBluetoothBeActive && isAnySelectedDeviceConnected) {
            // Bluetooth conditions met, start a trip
            AppLogger.log("LocationService", "Bluetooth conditions met. Starting/Continuing trip.")
            if (!_isTracking.value) {
                stopMonitoringInternal() // Stop monitoring if active
                isBluetoothTriggeredTrip = true
                _currentTripTrigger.value = TripTrigger.BLUETOOTH
                startTrip()
            }
        } else if (_isTracking.value) {
            // A trip is active (and not manual), but Bluetooth conditions are no longer met.
            // Or, if it was an auto trip and auto tracking is now disabled.
            AppLogger.log("LocationService", "Trip active, but conditions no longer met. Stopping trip.")
            stopTripAndPrepareForSummary(false) // This will call evaluateTrackingState again, which will then decide the next state.
        } else if (shouldAutoTrackBeActive) {
            // Auto tracking conditions met, start monitoring
            AppLogger.log("LocationService", "Auto tracking enabled. Starting/Continuing monitoring.")
            if (!isMonitoring) { // Only start monitoring if not already monitoring
                startMonitoring()
            }
        } else {
            // No conditions met for either Bluetooth or Auto tracking. Stop everything.
            AppLogger.log("LocationService", "No tracking conditions met. Stopping all tracking.")
            stopMonitoring() // This stops the service entirely
        }
    }

    private suspend fun isWithinSchedule(): Boolean {
        val now = Calendar.getInstance()
        val currentDayOfWeek = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> java.time.DayOfWeek.MONDAY
            Calendar.TUESDAY -> java.time.DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> java.time.DayOfWeek.THURSDAY
            Calendar.FRIDAY -> java.time.DayOfWeek.FRIDAY
            Calendar.SATURDAY -> java.time.DayOfWeek.SATURDAY
            Calendar.SUNDAY -> java.time.DayOfWeek.SUNDAY
            else -> return false // Should not happen
        }

        val scheduleSettings = userPreferencesRepository.scheduleSettings.first()
        val daySchedule = scheduleSettings.dailySchedules[currentDayOfWeek] ?: return false

        if (!daySchedule.isEnabled) {
            AppLogger.log("LocationService", "Tracking is disabled for $currentDayOfWeek.")
            return false
        }

        val currentTime = LocalTime.of(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        val startTime = LocalTime.of(daySchedule.startHour, daySchedule.startMinute)
        val endTime = LocalTime.of(daySchedule.endHour, daySchedule.endMinute)

        val isWithinTime = !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime)
        if (!isWithinTime) {
            AppLogger.log("LocationService", "Current time $currentTime is outside of schedule ($startTime - $endTime).")
        }
        return isWithinTime
    }

    @SuppressLint("MissingPermission")
    private fun startMonitoring() {
        if (isMonitoring) {
            AppLogger.log("LocationService", "Monitoring already active.")
            return
        }
        isMonitoring = true
        _isTracking.value = false // Ensure trip tracking is off
        previousMonitoringLocation = null
        highSpeedCounter = 0
        AppLogger.log("LocationService", "Starting monitoring and resetting state.")
        val notification = NotificationCompat.Builder(this, "location_service_channel")
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.waiting_for_movement))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .build()

        fusedLocationClient.removeLocationUpdates(locationCallback)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopMonitoring() {
        stopMonitoringInternal() // Stop location updates
        stopSelf() // Stop the service entirely
        AppLogger.log("LocationService", "Stopping monitoring and service.")
    }

    private fun stopMonitoringInternal() {
        if (!isMonitoring) {
            AppLogger.log("LocationService", "Monitoring not active, no need to stop internally.")
            return
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isMonitoring = false
        AppLogger.log("LocationService", "Stopped internal monitoring updates.")
    }

    @SuppressLint("MissingPermission")
    private fun startTrip() {
        if (_isTracking.value) {
            AppLogger.log("LocationService", "Trip already active.")
            return
        }
        isMonitoring = false // Ensure monitoring is off
        _isTracking.value = true
        _startLocation.value = null
        _lastLocation.value = null
        _distance.value = 0.0
        tripStartDate = Date()
        AppLogger.log("LocationService", "Starting location service for trip tracking.")

        val notification = buildNotification(0.0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }


        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .build()

        fusedLocationClient.removeLocationUpdates(locationCallback)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun handleMonitoringLocation(location: Location) {
        if (isStartingTrip) {
            AppLogger.log("LocationService", "Already starting a trip, ignoring new location update for monitoring.")
            return
        }

        previousMonitoringLocation?.let { prevLocation ->
            val timeDifference = (location.time - prevLocation.time) / 1000.0 // in seconds
            if (timeDifference > 1) { // Guard against zero or near-zero time difference
                val distance = prevLocation.distanceTo(location)
                val speed = distance / timeDifference // m/s
                val speedKmh = speed * 3.6
                AppLogger.log("LocationService", "Monitoring: TimeDiff: ${timeDifference}s, Distance: ${distance}m, Speed: $speedKmh km/h")
                if (speedKmh > 20) {
                    if (highSpeedCounter == 0) {
                        // This is the first detection of high speed. Cache this location.
                        potentialTripStartLocation = prevLocation
                        AppLogger.log("LocationService", "Potential trip start detected and cached.")
                    }
                    highSpeedCounter++
                    AppLogger.log("LocationService", "High speed detected. Counter: $highSpeedCounter")
                    if (highSpeedCounter >= 3) {
                        isStartingTrip = true
                        startAutoTrip()
                    }
                } else {
                    highSpeedCounter = 0
                    potentialTripStartLocation = null // Clear the cache if speed drops
                    AppLogger.log("LocationService", "Speed below threshold. Resetting counter and clearing potential start location.")
                }
            }
        }
        previousMonitoringLocation = location
    }

    private fun handleTripLocation(location: Location) {
        AppLogger.log(
            "LocationService",
            "New location: ${
                AppLogger.sanitizeLocation(
                    location.latitude,
                    location.longitude
                )
            }, Accuracy: ${location.accuracy}"
        )

        val previousLocation = _lastLocation.value

        if (_startLocation.value == null) {
            _startLocation.value = location
        }

        previousLocation?.let { prev ->
            if (location.accuracy < 35.0) {
                val distance = prev.distanceTo(location)
                if (distance > 2) { // Filter out GPS jitter
                    _distance.value += distance
                    AppLogger.log(
                        "LocationService",
                        "Distance since last point: ${distance}m. Total distance: ${_distance.value}m."
                    )
                }
            }
        }
        _lastLocation.value = location
        updateNotification(_distance.value)

        if (_currentTripTrigger.value != TripTrigger.AUTOMATIC) {
            if (_currentTripTrigger.value == TripTrigger.MANUAL) {
                AppLogger.log("LocationService", "Ignoring stillness because trip is Manual.")
            }
            return
        }

        applicationScope.launch {
            val stillnessTimerValue = userPreferencesRepository.stillnessTimer.first() * 1000L
            val minSpeedValue = userPreferencesRepository.minSpeed.first()

            withContext(Dispatchers.Main) {
                // Stillness detection logic
                if (stillnessTimer == null) {
                    // We are not currently in a stillness countdown. Start one.
                    stillnessTimer = object : CountDownTimer(stillnessTimerValue, 1000) {
                        override fun onTick(millisUntilFinished: Long) {}
                        override fun onFinish() {
                            AppLogger.log("LocationService", "Stillness timer finished, stopping auto trip.")
                            stopAutoTripAndSaveForReview()
                        }
                    }.start()
                    AppLogger.log("LocationService", "Stillness timer started.")
                }

                // Now, decide if the new location update should reset the timer.
                // A reset means we are confident the user is still driving.
                if (previousLocation != null) {
                    val timeDeltaSeconds = (location.time - previousLocation.time) / 1000.0

                    if (timeDeltaSeconds > 0) {
                        val distanceMeters = location.distanceTo(previousLocation)
                        // Manually calculate speed in km/h
                        val calculatedSpeedKmh = (distanceMeters / timeDeltaSeconds) * 3.6

                        AppLogger.log("LocationService", "Stillness check. Calculated Speed: %.2f km/h. TimeDelta: %.2fs. DistDelta: %.2fm.".format(calculatedSpeedKmh, timeDeltaSeconds, distanceMeters))

                        // If speed is high, we are definitely driving. Reset the timer.
                        if (calculatedSpeedKmh > minSpeedValue) {
                            AppLogger.log("LocationService", "Speed is > $minSpeedValue km/h. Resetting stillness timer.")
                            stillnessTimer?.cancel()
                            stillnessTimer = null // A new timer will start on the next location update.
                        }
                    }
                }
            }
        }
    }

    private fun buildNotification(distance: Double): android.app.Notification {
        val stopIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_STOP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val stopPendingIntent = PendingIntent.getActivity(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = getString(R.string.tracking_is_running, distance / 1000.0)

        return NotificationCompat.Builder(this, "location_service_channel")
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_record_dot)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, getString(R.string.stop), stopPendingIntent)
            .setOnlyAlertOnce(true)
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.my_error_red))
            .setWhen(tripStartDate?.time ?: System.currentTimeMillis())
            .setUsesChronometer(true)
            .setShowWhen(true)
            .build()
    }

    private fun updateNotification(distance: Double) {
        val notification = buildNotification(distance)
        notificationManager.notify(1, notification)
    }

    private fun startAutoTrip() {
        applicationScope.launch {
            // The check for Bluetooth priority is now handled in evaluateTrackingState()
            // This function just starts the auto trip if called.
            AppLogger.log("LocationService", "Starting automatic trip")
            highSpeedCounter = 0
            isManualTrip = false // Auto trips are not manual
            isBluetoothTriggeredTrip = false
            _currentTripTrigger.value = TripTrigger.AUTOMATIC
            withContext(Dispatchers.Main) {
                startTrip() // This will reset _startLocation to null
                _startLocation.value = potentialTripStartLocation // Immediately set it from the cache
                AppLogger.log(
                    "LocationService",
                    "Setting trip start location from cached value: ${
                        potentialTripStartLocation?.let {
                            AppLogger.sanitizeLocation(it.latitude, it.longitude)
                        }
                    }"
                )
                potentialTripStartLocation = null // Clear the cache
                isStartingTrip = false
            }
        }
    }

    private fun stopAutoTripAndSaveForReview() {
        AppLogger.log("LocationService", "Stopping automatic trip and saving for review.")
        _isTracking.value = false
        applicationScope.launch {
            saveTrip(isManualStop = false)
            evaluateTrackingState() // Re-evaluate state after trip ends
        }
    }

    private suspend fun stopTripAndPrepareForSummary(isManualStop: Boolean) {
        AppLogger.log("LocationService", "Stopping trip for summary.")
        val wasTracking = _isTracking.value
        _isTracking.value = false
        stillnessTimer?.cancel()
        stillnessTimer = null
        isManualTrip = false
        isBluetoothTriggeredTrip = false
        if (wasTracking) {
            saveTrip(isManualStop)
        }
        applicationScope.launch {
            evaluateTrackingState() // Re-evaluate state after trip ends
        }
    }

    private suspend fun saveTrip(isManualStop: Boolean) {
        val finalDistance = _distance.value
        if (finalDistance > 100) { // Only save if distance is more than 100 meters
            val startLocation = _startLocation.value
            val endLocation = _lastLocation.value
            val repository = (application as TripApplication).repository

            val isSmartLocationEnabled = userPreferencesRepository.isSmartLocationEnabled.first()
            val smartLocationRadius = userPreferencesRepository.smartLocationRadius.first()
            val savedPlaces = repository.getAllSavedPlaces().first()

            val geocodedStartAddress = startLocation?.let {
                geocoderHelper.getAddressFromLocation(it.latitude, it.longitude)
            } ?: "Unknown Start"

            val geocodedEndAddress = endLocation?.let {
                geocoderHelper.getAddressFromLocation(it.latitude, it.longitude)
            } ?: "Unknown End"

            val startAddress = geocoderHelper.getSmartAddress(
                originalAddress = geocodedStartAddress,
                lat = startLocation?.latitude,
                lng = startLocation?.longitude,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val endAddress = geocoderHelper.getSmartAddress(
                originalAddress = geocodedEndAddress,
                lat = endLocation?.latitude,
                lng = endLocation?.longitude,
                favorites = savedPlaces,
                isEnabled = isSmartLocationEnabled,
                radius = smartLocationRadius
            )

            val isBusinessDefault = userPreferencesRepository.defaultIsBusiness.first()
            val tripType = if (isBusinessDefault) "Business" else "Personal"

            val isConfirmed = _currentTripTrigger.value == TripTrigger.MANUAL || isManualStop

            val trip = Trip(
                startLoc = startAddress,
                endLoc = endAddress,
                distance = finalDistance / 1000.0, // Convert to km
                type = tripType,
                description = "",
                date = tripStartDate ?: Date(),
                endDate = System.currentTimeMillis(),
                isConfirmed = isConfirmed,
                isAutomatic = _currentTripTrigger.value == TripTrigger.AUTOMATIC
            )
            repository.insert(trip)
            AppLogger.log("LocationService", "Trip saved. Confirmed: $isConfirmed")

            if (!isConfirmed) {
                TripNotificationManager.sendTripReviewNotification(applicationContext, trip)
            }
        } else {
            AppLogger.log(
                "LocationService",
                "Trip too short, not saving. Distance: $finalDistance meters"
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isTracking.value = false
        isMonitoring = false
        AppLogger.log("LocationService", "Service destroyed.")
    }

    private fun createNotificationChannel() {
        val name = "Location Service"
        val descriptionText = "Channel for location service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("location_service_channel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START_MANUAL = "ACTION_START_MANUAL"
        const val ACTION_START_AUTOMATIC = "ACTION_START_AUTOMATIC"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_MONITORING = "ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "ACTION_STOP_MONITORING"
        const val ACTION_BLUETOOTH_CONNECTED = "ACTION_BLUETOOTH_CONNECTED"
        const val ACTION_BLUETOOTH_DISCONNECTED = "ACTION_BLUETOOTH_DISCONNECTED"

        private val _distance = MutableStateFlow(0.0)
        val distance = _distance.asStateFlow()

        private val _startLocation = MutableStateFlow<Location?>(null)
        val startLocation = _startLocation.asStateFlow()

        private val _lastLocation = MutableStateFlow<Location?>(null)
        val lastLocation = _lastLocation.asStateFlow()

        private val _isTracking =
            MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()

        private val _currentTripTrigger = MutableStateFlow(TripTrigger.MANUAL)
        val currentTripTrigger = _currentTripTrigger.asStateFlow()

        private val _tripSavedForSummary = MutableSharedFlow<Trip>()
        val tripSavedForSummary = _tripSavedForSummary.asSharedFlow()
    }
}
