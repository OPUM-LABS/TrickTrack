package ch.opum.tricktrack.logging

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ch.opum.tricktrack.R
import ch.opum.tricktrack.TripApplication
import ch.opum.tricktrack.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DebugExporter {

    private const val SUPPORT_EMAIL = "app@opum.ch"

    suspend fun createAndShareDebugPackage(context: Context) {
        val zipFile = generateDebugZip(context) ?: return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            zipFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "TrickTrack Debug Package - ${zipFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Hello TrickTrack Support,\n\nAttached is my diagnostic package to help troubleshoot an issue.\n\n")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserTitle = context.getString(R.string.debug_export_chooser_title)
        val chooser = Intent.createChooser(intent, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    @SuppressLint("StringFormatInvalid")
    private suspend fun generateDebugZip(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pInfo.versionName ?: "unknown"

            val zipFileName = "TrickTrack_Debug_v${versionName}_$dateStr.zip"
            val zipFile = File(context.cacheDir, zipFileName)
            if (zipFile.exists()) zipFile.delete()

            val app = context.applicationContext as TripApplication
            val prefsRepo = app.userPreferencesRepository
            val db = app.database
            val appPrefs = AppPreferences(context)

            // 1. System Info
            val systemInfo = buildString {
                appendLine("=== TRICKTRACK SYSTEM INFO ===")
                appendLine("App Version Name: $versionName")
                appendLine("App Version Code: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode}")
                appendLine("Package Name: ${context.packageName}")
                appendLine("Android OS Release: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Manufacturer: ${Build.MANUFACTURER}")
                appendLine("Device Model: ${Build.MODEL}")
                appendLine("Product: ${Build.PRODUCT}")
                appendLine("System Locale: ${Locale.getDefault()}")
            }

            // 2. Permission Health
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val permissionsHealth = buildString {
                appendLine("=== PERMISSION & SYSTEM HEALTH ===")
                appendLine("ACCESS_FINE_LOCATION: ${ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == 0}")
                appendLine("ACCESS_COARSE_LOCATION: ${ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == 0}")
                appendLine("ACCESS_BACKGROUND_LOCATION: ${
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == 0
                    else true
                }")
                appendLine("Battery Optimizations Ignored: ${pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false}")
                appendLine("POST_NOTIFICATIONS: ${
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == 0
                    else true
                }")
                appendLine("BLUETOOTH_CONNECT: ${
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == 0
                    else true
                }")
            }

            // 3. Settings Summary
            val settingsSummary = buildString {
                appendLine("=== APP SETTINGS SUMMARY ===")
                appendLine("Auto Tracking Enabled: ${prefsRepo.isAutoTrackingEnabled.first()}")
                appendLine("Bluetooth Trigger Enabled: ${prefsRepo.bluetoothTriggerEnabled.first()}")
                appendLine("Distance Monitoring Enabled: ${prefsRepo.isDistanceMonitoringEnabled.first()}")
                appendLine("Distance Monitoring Radius: ${prefsRepo.distanceMonitoringRadius.first()}m")
                appendLine("Minimum Speed: ${prefsRepo.minSpeed.first()} km/h")
                appendLine("Minimum Trip Distance: ${prefsRepo.minTripDistance.first()}m")
                appendLine("Stillness Timer: ${prefsRepo.stillnessTimer.first()}s")
                appendLine("Odometer Mode Enabled: ${prefsRepo.isOdometerModeEnabled.first()}")
                appendLine("Distance Unit: ${prefsRepo.distanceUnit.first()}")
                appendLine("Expense Tracking Enabled: ${prefsRepo.expenseTrackingEnabled.first()}")
                appendLine("Expense Rate: ${prefsRepo.expenseRatePerKm.first()} / ${prefsRepo.expenseCurrency.first()}")
                appendLine("Schedule Enabled: ${prefsRepo.isScheduleEnabled.first()}")
                appendLine("OSRM Server URL: ${appPrefs.getOsrmUrl()}")
                appendLine("Photon Server URL: ${appPrefs.getPhotonUrl()}")
            }

            // 4. Database Stats
            val dbStats = buildString {
                appendLine("=== DATABASE STATISTICS ===")
                appendLine("Confirmed Trips: ${db.tripDao().getConfirmedTrips().first().size}")
                appendLine("Unconfirmed Trips: ${db.tripDao().getUnconfirmedTrips().first().size}")
                appendLine("Vehicles Count: ${db.vehicleDao().getAll().first().size}")
                appendLine("Drivers Count: ${db.driverDao().getAll().first().size}")
                appendLine("Companies Count: ${db.companyDao().getAll().first().size}")
                appendLine("Saved Places Count: ${db.savedPlaceDao().getAll().first().size}")
            }

            // 5. App Logs
            val appLogs = AppLogger.getAllLogs()

            // Zip Compression
            FileOutputStream(zipFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zos ->
                        addTextToZip(zos, "system_info.txt", systemInfo)
                        addTextToZip(zos, "permissions_health.txt", permissionsHealth)
                        addTextToZip(zos, "settings_summary.txt", settingsSummary)
                        addTextToZip(zos, "database_stats.txt", dbStats)
                        addTextToZip(zos, "app_logs.txt", appLogs)
                    }
                }
            }

            zipFile
        } catch (e: Exception) {
            AppLogger.log("DebugExporter", "Failed to generate debug zip: ${e.message}")
            null
        }
    }

    private fun addTextToZip(zos: ZipOutputStream, entryName: String, text: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(text.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }
}
