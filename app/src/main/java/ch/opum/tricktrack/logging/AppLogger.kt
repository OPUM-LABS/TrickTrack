package ch.opum.tricktrack.logging

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2 MB
    private const val CURRENT_LOG_FILE = "TrickTrack_current_log.txt"
    private const val OLD_LOG_FILE = "TrickTrack_old_log_backup.txt"

    private lateinit var logDir: File

    fun init(context: Context) {
        logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        log("AppLogger", "Logger initialized")
    }

    fun log(tag: String, message: String) {
        Log.d(tag, message)

        val logFile = File(logDir, CURRENT_LOG_FILE)
        if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
            val oldLogFile = File(logDir, OLD_LOG_FILE)
            if (oldLogFile.exists()) {
                oldLogFile.delete()
            }
            logFile.renameTo(oldLogFile)
        }

        val timestamp =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logFile.appendText("$timestamp $tag: $message\n")
    }

    fun getAllLogs(): String {
        val oldLogFile = File(logDir, OLD_LOG_FILE)
        val currentLogFile = File(logDir, CURRENT_LOG_FILE)

        val oldLogs = if (oldLogFile.exists()) oldLogFile.readText() else ""
        val currentLogs = if (currentLogFile.exists()) currentLogFile.readText() else ""

        return oldLogs + currentLogs
    }

    fun exportLogs(context: Context): Uri? {
        val exportFile = File(context.cacheDir, "TrickTrack_log_export.txt")
        exportFile.writeText(getAllLogs())
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", exportFile)
    }

    // New helper function for privacy masking
    fun sanitizeLocation(lat: Double, lng: Double): String {
        val sanitizedLat = "%.2f".format(Locale.getDefault(), lat)
        val sanitizedLng = "%.2f".format(Locale.getDefault(), lng)
        return "$sanitizedLat***, $sanitizedLng***"
    }
}