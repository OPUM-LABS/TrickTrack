package ch.opum.tricktrack

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.UiModeManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import androidx.core.content.IntentCompat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.location.Location
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.DistanceUnit
import ch.opum.tricktrack.logging.AppLogger
import ch.opum.tricktrack.ui.TripTrigger
import ch.opum.tricktrack.util.DistanceFormatter
import ch.opum.tricktrack.util.PolylineUtils
import org.osmdroid.util.GeoPoint
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.math.sqrt

data class MovementInfo(
    val timestamp: Long = System.currentTimeMillis(),
    val distanceMeters: Float,
    val speedKmh: Double,
    val speedThresholdKmh: Int,
    val counter: Int
)

data class MotionSensorInfo(
    val timestamp: Long = System.currentTimeMillis(),
    val sensorName: String,
    val isMotionDetected: Boolean,
    val isGpsActive: Boolean,
    val statusText: String,
)

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
    private var lastLocationTime: Long = 0
    private var lastReportedSpeed: Double = 0.0
    private var currentDistanceUnit: DistanceUnit = DistanceUnit.KM
    private var currentMinSpeed: Int = 10
    private val recordedWaypoints = mutableListOf<GeoPoint>()

    private lateinit var sensorManager: SensorManager
    private var significantMotionSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var triggerEventListener: TriggerEventListener? = null
    private var accelerometerListener: SensorEventListener? = null
    private var isGpsElevated: Boolean = false


    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        significantMotionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
        if (significantMotionSensor == null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        TripNotificationManager.createNotificationChannel(this)
        
        applicationScope.launch {
            userPreferencesRepository.distanceUnit.collect { unit ->
                currentDistanceUnit = unit
                if (_isTracking.value) {
                    updateNotification(_distance.value)
                }
            }
        }

        applicationScope.launch {
            userPreferencesRepository.minSpeed.collect { speed ->
                currentMinSpeed = speed
            }
        }

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
        if (_isTracking.value && (_currentTripTrigger.value == TripTrigger.MANUAL) && (intent?.action == ACTION_BLUETOOTH_CONNECTED)) {
            AppLogger.log("LocationService", "Ignoring Bluetooth connection because a manual trip is in progress.")
            return START_STICKY
        }

        val trigger = intent?.let { IntentCompat.getSerializableExtra(it, "trigger", TripTrigger::class.java) }

        trigger?.let {
            _currentTripTrigger.value = it
        }

        when (intent?.action) {
            ACTION_START_MONITORING, // Initial start or schedule change
            ACTION_BLUETOOTH_CONNECTED,
            ACTION_BLUETOOTH_DISCONNECTED,
            -> {
                applicationScope.launch {
                    if (isMonitoring) {
                        stopMonitoringInternal()
                    }
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
                    stopTripAndPrepareForSummary()
                }
            }
            ACTION_STOP_MONITORING -> { // Explicit stop monitoring command
                applicationScope.launch {
                    stopMonitoring() // This stops the service entirely
                }
            }
            ACTION_STILL_DRIVING_YES -> {
                applicationScope.launch {
                    restartStillnessTimer()
                }
            }
            ACTION_STILL_DRIVING_NO -> {
                applicationScope.launch {
                    stopAutoTripAndSaveForReview()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun evaluateTrackingState() {
        // If a manual trip is active, we don't interfere with it.
        if (_isTracking.value && (_currentTripTrigger.value == TripTrigger.MANUAL)) {
            AppLogger.log("LocationService", "Manual trip active, not evaluating automatic/bluetooth state.")
            return
        }

        val isBtTriggerEnabled = userPreferencesRepository.bluetoothTriggerEnabled.first()
        val isAutoTrackingEnabled = userPreferencesRepository.isAutoTrackingEnabled.first()
        val isScheduleEnabled = userPreferencesRepository.isScheduleEnabled.first()

        val selectedDevices = userPreferencesRepository.selectedBluetoothDevices.first()
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isCarMode = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR
        val isAnySelectedDeviceConnected = selectedDevices.any { bluetoothRepository.isDeviceConnected(it) } || isCarMode
        
        // Determine if Bluetooth or Auto tracking should be active based on schedule or direct settings
        val isWithinSchedule = if (isScheduleEnabled) isWithinSchedule() else true

        val shouldBluetoothBeActive = isBtTriggerEnabled && isWithinSchedule
        val shouldAutoTrackBeActive = isAutoTrackingEnabled && isWithinSchedule

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
            stopTripAndPrepareForSummary() // This will call evaluateTrackingState again, which will then decide the next state.
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
        val currentDayOfWeek = when (now[Calendar.DAY_OF_WEEK]) {
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

    private fun armMotionSensor() {
        disarmMotionSensor()
        val sensorName = if (significantMotionSensor != null) "Hardware Significant Motion" else if (accelerometerSensor != null) "Accelerometer" else "Low-Power Location"
        _motionSensorInfo.value = MotionSensorInfo(
            timestamp = System.currentTimeMillis(),
            sensorName = sensorName,
            isMotionDetected = false,
            isGpsActive = false,
            statusText = "Stationary (GPS Sleeping - Battery Saving Mode)"
        )

        if (significantMotionSensor != null) {
            triggerEventListener = object : TriggerEventListener() {
                override fun onTrigger(event: TriggerEvent?) {
                    AppLogger.log("LocationService", "Significant motion detected by hardware sensor!")
                    applicationScope.launch(Dispatchers.Main) {
                        onMotionDetected()
                    }
                }
            }
            sensorManager.requestTriggerSensor(triggerEventListener, significantMotionSensor)
        } else if (accelerometerSensor != null) {
            accelerometerListener = object : SensorEventListener {
                private var lastAccelTime: Long = 0
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    val now = System.currentTimeMillis()
                    if (now - lastAccelTime < 1000) return
                    lastAccelTime = now
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gVector = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
                    if (abs(gVector) > 1.8) {
                        AppLogger.log("LocationService", "Motion detected by accelerometer!")
                        disarmMotionSensor()
                        applicationScope.launch(Dispatchers.Main) {
                            onMotionDetected()
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun disarmMotionSensor() {
        triggerEventListener?.let {
            if (significantMotionSensor != null) {
                sensorManager.cancelTriggerSensor(it, significantMotionSensor)
            }
        }
        triggerEventListener = null

        accelerometerListener?.let {
            sensorManager.unregisterListener(it)
        }
        accelerometerListener = null
    }

    private fun onMotionDetected() {
        if (!isMonitoring || _isTracking.value) return
        AppLogger.log("LocationService", "Motion sensor triggered! Elevating to High-Accuracy GPS for speed check.")
        isGpsElevated = true
        val sensorName = if (significantMotionSensor != null) "Hardware Significant Motion" else "Accelerometer"
        _motionSensorInfo.value = MotionSensorInfo(
            timestamp = System.currentTimeMillis(),
            sensorName = sensorName,
            isMotionDetected = true,
            isGpsActive = true,
            statusText = "Motion Detected! High-Accuracy GPS Active"
        )
        upgradeToHighAccuracyGps()
    }

    @SuppressLint("MissingPermission")
    private fun upgradeToHighAccuracyGps() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .build()

        fusedLocationClient.removeLocationUpdates(locationCallback)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper(),
        )
    }

    @SuppressLint("MissingPermission")
    private fun downgradeToLowPowerMonitoring() {
        if (!isMonitoring || _isTracking.value) return
        AppLogger.log("LocationService", "Speed below threshold. Returning to Low-Power Motion Detection.")
        isGpsElevated = false
        armMotionSensor()

        applicationScope.launch {
            val radius = userPreferencesRepository.distanceMonitoringRadius.first()
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000)
                .setMinUpdateDistanceMeters(radius.toFloat())
                .build()

            withContext(Dispatchers.Main) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper(),
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun startMonitoring() {
        if (isMonitoring) {
            AppLogger.log("LocationService", "Monitoring already active.")
            return
        }
        isMonitoring = true
        _isTracking.value = false // Ensure trip tracking is off
        previousMonitoringLocation = null
        highSpeedCounter = 0
        isGpsElevated = false
        AppLogger.log("LocationService", "Starting monitoring and resetting state.")
        val notification = NotificationCompat.Builder(this, "monitoring_channel")
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.waiting_for_movement))
            .setSmallIcon(R.drawable.tricktrack_outline)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

        val isDistanceMonitoringEnabled = userPreferencesRepository.isDistanceMonitoringEnabled.first()
        val radius = userPreferencesRepository.distanceMonitoringRadius.first()

        if (isDistanceMonitoringEnabled) {
            armMotionSensor()
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000)
                .setMinUpdateDistanceMeters(radius.toFloat())
                .build()

            fusedLocationClient.removeLocationUpdates(locationCallback)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
        } else {
            disarmMotionSensor()
            _motionSensorInfo.value = MotionSensorInfo(
                timestamp = System.currentTimeMillis(),
                sensorName = "Continuous GPS",
                isMotionDetected = true,
                isGpsActive = true,
                statusText = "Continuous High-Accuracy GPS"
            )
            upgradeToHighAccuracyGps()
        }
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
        disarmMotionSensor()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isMonitoring = false
        isGpsElevated = false
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
        recordedWaypoints.clear()
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
            Looper.getMainLooper(),
        )
    }

    private fun handleMonitoringLocation(location: Location) {
        if (isStartingTrip) {
            AppLogger.log("LocationService", "Already starting a trip, ignoring new location update for monitoring.")
            return
        }

        // If location updates fired because of distance displacement, elevate to High-Accuracy GPS
        val isDistanceMonitoringEnabled = runCatching {
            runBlocking { userPreferencesRepository.isDistanceMonitoringEnabled.first() }
        }.getOrDefault(false)

        if (isDistanceMonitoringEnabled && !isGpsElevated && previousMonitoringLocation != null) {
            val dist = previousMonitoringLocation!!.distanceTo(location)
            if (dist > 5.0) { // Displaced
                onMotionDetected()
            }
        }

        previousMonitoringLocation?.let { prevLocation ->
            val timeDifference = (location.time - prevLocation.time) / 1000.0 // in seconds
            if (timeDifference > 1) { // Guard against zero or near-zero time difference
                val distance = prevLocation.distanceTo(location)
                val speed = distance / timeDifference // m/s
                val speedKmh = speed * 3.6

                // Sanity check: Ignore impossible speeds (> 250 km/h) or poor accuracy caused by cell tower jumps / GPS glitches
                if (speedKmh > 250.0 || (location.hasAccuracy() && location.accuracy > 100f)) {
                    AppLogger.log("LocationService", "Ignoring GPS glitch/teleportation. Speed: $speedKmh km/h, Accuracy: ${location.accuracy}")
                    return@let
                }

                val minSpeedValue = currentMinSpeed
                AppLogger.log("LocationService", "Monitoring: TimeDiff: ${timeDifference}s, Distance: ${distance}m, Speed: $speedKmh km/h (Threshold: $minSpeedValue km/h)")
                var currentCounter: Int
                if (speedKmh >= minSpeedValue) {
                    if (highSpeedCounter == 0) {
                        // This is the first detection of high speed. Cache this location.
                        potentialTripStartLocation = prevLocation
                        AppLogger.log("LocationService", "Potential trip start detected and cached.")
                    }
                    highSpeedCounter++
                    currentCounter = highSpeedCounter
                    AppLogger.log("LocationService", "High speed detected. Counter: $highSpeedCounter")
                    if (highSpeedCounter >= 2) {
                        isStartingTrip = true
                        disarmMotionSensor()
                        startAutoTrip()
                    }
                } else {
                    highSpeedCounter = 0
                    currentCounter = 0
                    potentialTripStartLocation = null // Clear the cache if speed drops
                    AppLogger.log("LocationService", "Speed below threshold ($minSpeedValue km/h). Resetting counter and clearing potential start location.")
                    if (isDistanceMonitoringEnabled && isGpsElevated) {
                        downgradeToLowPowerMonitoring()
                    }
                }

                _lastMovementInfo.value = MovementInfo(
                    timestamp = System.currentTimeMillis(),
                    distanceMeters = distance,
                    speedKmh = speedKmh,
                    speedThresholdKmh = minSpeedValue,
                    counter = currentCounter
                )
            }
        }
        if (previousMonitoringLocation == null) {
            _lastMovementInfo.value = MovementInfo(
                timestamp = System.currentTimeMillis(),
                distanceMeters = 0f,
                speedKmh = 0.0,
                speedThresholdKmh = currentMinSpeed,
                counter = 0
            )
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
        lastLocationTime = System.currentTimeMillis()

        if (_startLocation.value == null) {
            _startLocation.value = location
            recordedWaypoints.add(GeoPoint(location.latitude, location.longitude))
        }

        previousLocation?.let { prev ->
            if (location.accuracy < 35.0) {
                val distance = prev.distanceTo(location)
                if (distance > 2) { // Filter out GPS jitter
                    _distance.value += distance
                    recordedWaypoints.add(GeoPoint(location.latitude, location.longitude))
                    AppLogger.log(
                        "LocationService",
                        "Distance since last point: ${distance}m. Total distance: ${_distance.value}m."
                    )
                }
            }
        }
        _lastLocation.value = location
        updateNotification(_distance.value)

        // Priority: If Bluetooth is tracking, we don't need stillness detection (logic of automatic tracking)
        if (_currentTripTrigger.value == TripTrigger.BLUETOOTH) {
            return
        }

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
                            applicationScope.launch {
                                val minSpeedValueFlow = userPreferencesRepository.minSpeed.first()
                                val timeSinceLastLoc = System.currentTimeMillis() - lastLocationTime
                                
                                if (timeSinceLastLoc >= stillnessTimerValue && lastReportedSpeed > minSpeedValueFlow) {
                                    AppLogger.log("LocationService", "Stillness timer finished but signal might be lost. Asking user.")
                                    showStillDrivingNotification()
                                } else {
                                    AppLogger.log("LocationService", "Stillness timer finished, stopping auto trip.")
                                    stopAutoTripAndSaveForReview()
                                }
                            }
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
                        lastReportedSpeed = calculatedSpeedKmh

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

        val formattedDistance = DistanceFormatter.format(distance / 1000.0, currentDistanceUnit)
        val notificationText = getString(R.string.tracking_is_running, formattedDistance)

        return NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.tricktrack_logo)
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
            try {
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
                }
            } catch (e: Exception) {
                AppLogger.log("LocationService", "Error starting automatic trip: ${e.message}")
            } finally {
                isStartingTrip = false
            }
        }
    }

    private fun stopAutoTripAndSaveForReview() {
        AppLogger.log("LocationService", "Stopping automatic trip and saving for review.")
        _isTracking.value = false
        applicationScope.launch {
            saveTrip()
            evaluateTrackingState() // Re-evaluate state after trip ends
        }
    }

    private suspend fun stopTripAndPrepareForSummary() {
        AppLogger.log("LocationService", "Stopping trip for summary.")
        val wasTracking = _isTracking.value
        _isTracking.value = false
        stillnessTimer?.cancel()
        stillnessTimer = null
        isManualTrip = false
        isBluetoothTriggeredTrip = false
        if (wasTracking) {
            saveTrip()
        }
        applicationScope.launch {
            evaluateTrackingState() // Re-evaluate state after trip ends
        }
    }

    private suspend fun saveTrip() {
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
            val defaultVehicleId = userPreferencesRepository.defaultVehicleId.first().takeIf { it != -1 }
            val tripType = if (isBusinessDefault) "Business" else "Personal"

            val isConfirmed = false // All live-tracked trips require review before confirmation
            val encodedPolyline = if (recordedWaypoints.size >= 2) PolylineUtils.encode(recordedWaypoints) else null
            recordedWaypoints.clear()

            val trip = Trip(
                startLoc = startAddress,
                endLoc = endAddress,
                distance = finalDistance / 1000.0, // Convert to km
                type = tripType,
                description = "",
                date = tripStartDate ?: Date(),
                endDate = System.currentTimeMillis(),
                isConfirmed = isConfirmed,
                startLat = startLocation?.latitude,
                startLon = startLocation?.longitude,
                endLat = endLocation?.latitude,
                endLon = endLocation?.longitude,
                isAutomatic = _currentTripTrigger.value != TripTrigger.MANUAL,
                vehicleId = defaultVehicleId,
                trigger = _currentTripTrigger.value.name,
                routePolyline = encodedPolyline
            )
            val newId = repository.insert(trip)
            AppLogger.log("LocationService", "Trip saved with ID: $newId. Trigger: ${_currentTripTrigger.value}, Confirmed: $isConfirmed")

            if (!isConfirmed) {
                val tripWithId = trip.copy(id = newId)
                TripNotificationManager.sendTripReviewNotification(applicationContext, tripWithId, currentDistanceUnit)
            }
        } else {
            AppLogger.log(
                "LocationService",
                "Trip too short, not saving. Distance: $finalDistance meters"
            )
            if (_currentTripTrigger.value == TripTrigger.MANUAL) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        "Trip under 100m was not saved.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        disarmMotionSensor()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isTracking.value = false
        isMonitoring = false
        isGpsElevated = false
        AppLogger.log("LocationService", "Service destroyed.")
    }

    private fun showStillDrivingNotification() {
        val yesIntent = Intent(this, LocationService::class.java).apply { action = ACTION_STILL_DRIVING_YES }
        val noIntent = Intent(this, LocationService::class.java).apply { action = ACTION_STILL_DRIVING_NO }
        
        val yesPendingIntent = PendingIntent.getService(this, 10, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val noPendingIntent = PendingIntent.getService(this, 11, noIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle(getString(R.string.notification_still_driving_title))
            .setContentText(getString(R.string.notification_still_driving_text))
            .setSmallIcon(R.drawable.tricktrack_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(0, getString(R.string.yes), yesPendingIntent)
            .addAction(0, getString(R.string.no), noPendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(2, notification)
    }

    private suspend fun restartStillnessTimer() {
        notificationManager.cancel(2)
        val stillnessTimerValue = userPreferencesRepository.stillnessTimer.first() * 1000L
        withContext(Dispatchers.Main) {
            stillnessTimer?.cancel()
            stillnessTimer = object : CountDownTimer(stillnessTimerValue, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    applicationScope.launch {
                        val minSpeedValueFlow = userPreferencesRepository.minSpeed.first()
                        val timeSinceLastLoc = System.currentTimeMillis() - lastLocationTime
                        
                        if (timeSinceLastLoc >= stillnessTimerValue && lastReportedSpeed > minSpeedValueFlow) {
                            showStillDrivingNotification()
                        } else {
                            stopAutoTripAndSaveForReview()
                        }
                    }
                }
            }.start()
        }
    }

    private fun createNotificationChannels() {
        val trackingName = "Trip Tracking"
        val trackingDesc = "Active trip tracking updates"
        val trackingImportance = NotificationManager.IMPORTANCE_DEFAULT
        val trackingChannel = NotificationChannel("tracking_channel", trackingName, trackingImportance).apply {
            description = trackingDesc
        }

        val monitoringName = "Background Service"
        val monitoringDesc = "Background movement detection"
        val monitoringImportance = NotificationManager.IMPORTANCE_LOW
        val monitoringChannel = NotificationChannel("monitoring_channel", monitoringName, monitoringImportance).apply {
            description = monitoringDesc
            setShowBadge(false)
        }

        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(trackingChannel)
        notificationManager.createNotificationChannel(monitoringChannel)
    }

    companion object {
        const val ACTION_START_MANUAL = "ACTION_START_MANUAL"
        const val ACTION_START_AUTOMATIC = "ACTION_START_AUTOMATIC"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_MONITORING = "ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "ACTION_STOP_MONITORING"
        const val ACTION_BLUETOOTH_CONNECTED = "ACTION_BLUETOOTH_CONNECTED"
        const val ACTION_BLUETOOTH_DISCONNECTED = "ACTION_BLUETOOTH_DISCONNECTED"
        const val ACTION_STILL_DRIVING_YES = "ACTION_STILL_DRIVING_YES"
        const val ACTION_STILL_DRIVING_NO = "ACTION_STILL_DRIVING_NO"

        private val _distance = MutableStateFlow(0.0)
        val distance = _distance.asStateFlow()

        private val _startLocation = MutableStateFlow<Location?>(null)

        private val _lastLocation = MutableStateFlow<Location?>(null)

        private val _isTracking =
            MutableStateFlow(value = false)
        val isTracking = _isTracking.asStateFlow()

        private val _motionSensorInfo = MutableStateFlow<MotionSensorInfo?>(null)
        val motionSensorInfo = _motionSensorInfo.asStateFlow()

        private val _lastMovementInfo = MutableStateFlow<MovementInfo?>(null)
        val lastMovementInfo = _lastMovementInfo.asStateFlow()

        private val _currentTripTrigger = MutableStateFlow(TripTrigger.MANUAL)
    }
}

