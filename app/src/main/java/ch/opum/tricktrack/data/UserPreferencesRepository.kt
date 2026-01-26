package ch.opum.tricktrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Currency
import java.util.Locale

// At the top level of your file, declare the DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_AUTO_TRACKING_ENABLED = booleanPreferencesKey("is_auto_tracking_enabled")
        val KEY_BLUETOOTH_TRIGGER_ENABLED = booleanPreferencesKey("bluetooth_trigger_enabled")
        val KEY_SELECTED_BLUETOOTH_DEVICES = stringSetPreferencesKey("selected_bluetooth_devices")
        val DEFAULT_IS_BUSINESS = booleanPreferencesKey("default_is_business")
        val EXPENSE_TRACKING_ENABLED = booleanPreferencesKey("expense_tracking_enabled")
        val EXPENSE_RATE_PER_KM = floatPreferencesKey("expense_rate_per_km")
        val EXPENSE_CURRENCY = stringPreferencesKey("expense_currency")
        val EXPORT_COLUMNS = stringSetPreferencesKey("export_columns")
        val KEY_SMART_LOCATION_ENABLED = booleanPreferencesKey("smart_location_enabled")
        val KEY_SMART_LOCATION_RADIUS = intPreferencesKey("smart_location_radius")
        val KEY_SCHEDULE_TARGET = stringPreferencesKey("schedule_target")
        val IS_SCHEDULE_ENABLED = booleanPreferencesKey("is_schedule_enabled")
        val KEY_SNAPSHOT_AUTO_TRACKING = booleanPreferencesKey("snapshot_auto_tracking")
        val KEY_SNAPSHOT_BLUETOOTH = booleanPreferencesKey("snapshot_bluetooth")
        val STILLNESS_TIMER_S = intPreferencesKey("stillness_timer_s")
        val MIN_SPEED_KMH = intPreferencesKey("min_speed_kmh")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val BACKUP_DAY_OF_WEEK = intPreferencesKey("backup_day_of_week")
        val BACKUP_DAY_OF_MONTH = intPreferencesKey("backup_day_of_month")
        val BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")


        fun trackingDayEnabled(day: DayOfWeek) = booleanPreferencesKey("tracking_day_enabled_${day.name}")
        fun trackingStartHour(day: DayOfWeek) = intPreferencesKey("tracking_start_hour_${day.name}")
        fun trackingStartMinute(day: DayOfWeek) = intPreferencesKey("tracking_start_minute_${day.name}")
        fun trackingEndHour(day: DayOfWeek) = intPreferencesKey("tracking_end_hour_${day.name}")
        fun trackingEndMinute(day: DayOfWeek) = intPreferencesKey("tracking_end_minute_${day.name}")
    }

    val isAutoTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_AUTO_TRACKING_ENABLED] ?: false
        }

    suspend fun setAutoTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AUTO_TRACKING_ENABLED] = enabled
        }
    }

    val bluetoothTriggerEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.KEY_BLUETOOTH_TRIGGER_ENABLED] ?: false
        }

    suspend fun setBluetoothTriggerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_BLUETOOTH_TRIGGER_ENABLED] = enabled
        }
    }

    val selectedBluetoothDevices: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.KEY_SELECTED_BLUETOOTH_DEVICES] ?: emptySet()
        }

    suspend fun toggleBluetoothDevice(address: String) {
        context.dataStore.edit { preferences ->
            val currentDevices =
                preferences[PreferencesKeys.KEY_SELECTED_BLUETOOTH_DEVICES] ?: emptySet()
            val newDevices = if (currentDevices.contains(address)) {
                currentDevices - address
            } else {
                currentDevices + address
            }
            preferences[PreferencesKeys.KEY_SELECTED_BLUETOOTH_DEVICES] = newDevices
        }
    }

    val defaultIsBusiness: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_IS_BUSINESS] ?: true
        }

    suspend fun updateDefaultTripType(isBusiness: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_IS_BUSINESS] = isBusiness
        }
    }

    val expenseTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.EXPENSE_TRACKING_ENABLED] ?: false
        }

    suspend fun setExpenseTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPENSE_TRACKING_ENABLED] = enabled
        }
    }

    val expenseRatePerKm: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.EXPENSE_RATE_PER_KM] ?: 0.0f
        }

    suspend fun setExpenseRatePerKm(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPENSE_RATE_PER_KM] = rate
        }
    }

    val expenseCurrency: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.EXPENSE_CURRENCY]
                ?: Currency.getInstance(Locale.getDefault()).symbol
        }

    suspend fun setExpenseCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPENSE_CURRENCY] = currency
        }
    }

    val exportColumns: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.EXPORT_COLUMNS] ?: setOf("DATE", "TIME", "START_LOCATION", "END_LOCATION", "DISTANCE", "TYPE", "EXPENSES")
        }

    suspend fun setExportColumns(columns: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPORT_COLUMNS] = columns
        }
    }

    val exportSettings: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.EXPORT_COLUMNS] ?: setOf("DATE", "TIME", "START_LOCATION", "END_LOCATION", "DISTANCE", "TYPE", "EXPENSES")
        }

    val isSmartLocationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.KEY_SMART_LOCATION_ENABLED] ?: true
        }

    suspend fun setSmartLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SMART_LOCATION_ENABLED] = enabled
        }
    }

    val smartLocationRadius: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.KEY_SMART_LOCATION_RADIUS] ?: 150
        }

    suspend fun setSmartLocationRadius(radius: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SMART_LOCATION_RADIUS] = radius
        }
    }

    val isScheduleEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_SCHEDULE_ENABLED] ?: false
        }

    suspend fun setScheduleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            if (enabled) {
                val autoTracking = isAutoTrackingEnabled.first()
                val bluetooth = bluetoothTriggerEnabled.first()
                preferences[PreferencesKeys.KEY_SNAPSHOT_AUTO_TRACKING] = autoTracking
                preferences[PreferencesKeys.KEY_SNAPSHOT_BLUETOOTH] = bluetooth
            } else {
                val snapshotAuto = preferences[PreferencesKeys.KEY_SNAPSHOT_AUTO_TRACKING] ?: false
                val snapshotBluetooth = preferences[PreferencesKeys.KEY_SNAPSHOT_BLUETOOTH] ?: false
                preferences[PreferencesKeys.IS_AUTO_TRACKING_ENABLED] = snapshotAuto
                preferences[PreferencesKeys.KEY_BLUETOOTH_TRIGGER_ENABLED] = snapshotBluetooth
            }
            preferences[PreferencesKeys.IS_SCHEDULE_ENABLED] = enabled
        }
    }

    val scheduleSettings: Flow<ScheduleSettings> = context.dataStore.data.map { preferences ->
        val targetString = preferences[PreferencesKeys.KEY_SCHEDULE_TARGET]
        val target = targetString?.let { ScheduleTarget.valueOf(it) } ?: ScheduleTarget.AUTOMATIC

        val dailySchedules = DayOfWeek.entries.associateWith { day ->
            DaySchedule(
                isEnabled = preferences[PreferencesKeys.trackingDayEnabled(day)] ?: true,
                startHour = preferences[PreferencesKeys.trackingStartHour(day)] ?: 0,
                startMinute = preferences[PreferencesKeys.trackingStartMinute(day)] ?: 0,
                endHour = preferences[PreferencesKeys.trackingEndHour(day)] ?: 23,
                endMinute = preferences[PreferencesKeys.trackingEndMinute(day)] ?: 59
            )
        }
        ScheduleSettings(target, dailySchedules)
    }

    suspend fun updateScheduleSettings(settings: ScheduleSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SCHEDULE_TARGET] = settings.target.name
            settings.dailySchedules.forEach { (day, schedule) ->
                preferences[PreferencesKeys.trackingDayEnabled(day)] = schedule.isEnabled
                preferences[PreferencesKeys.trackingStartHour(day)] = schedule.startHour
                preferences[PreferencesKeys.trackingStartMinute(day)] = schedule.startMinute
                preferences[PreferencesKeys.trackingEndHour(day)] = schedule.endHour
                preferences[PreferencesKeys.trackingEndMinute(day)] = schedule.endMinute
            }
        }
    }

    val stillnessTimer: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.STILLNESS_TIMER_S] ?: 60
        }

    suspend fun setStillnessTimer(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STILLNESS_TIMER_S] = seconds
        }
    }

    val minSpeed: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MIN_SPEED_KMH] ?: 15
        }

    suspend fun setMinSpeed(speed: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MIN_SPEED_KMH] = speed
        }
    }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false
        }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    val backupFrequency: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BACKUP_FREQUENCY] ?: "DAILY"
        }

    suspend fun setBackupFrequency(frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_FREQUENCY] = frequency
        }
    }

    val backupDayOfWeek: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BACKUP_DAY_OF_WEEK] ?: Calendar.MONDAY
        }

    suspend fun setBackupDayOfWeek(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_DAY_OF_WEEK] = day
        }
    }

    val backupDayOfMonth: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BACKUP_DAY_OF_MONTH] ?: 1
        }

    suspend fun setBackupDayOfMonth(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_DAY_OF_MONTH] = day
        }
    }

    val backupFolderUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BACKUP_FOLDER_URI]
        }

    suspend fun setBackupFolderUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_FOLDER_URI] = uri
        }
    }

    suspend fun getAllPreferences(): Map<String, String> {
        val preferences = context.dataStore.data.first()
        val map = mutableMapOf<String, String>()
        preferences.asMap().forEach { (key, value) ->
            map[key.name] = value.toString()
        }
        return map
    }
}
