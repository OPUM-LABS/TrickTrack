package ch.opum.tricktrack.logging

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Writes the in-app log file.
 *
 * log() never touches the disk itself: it only queues the line. A single background thread
 * appends everything queued so far in one write, so callers (main thread, GPS and sensor
 * callbacks) never block on file I/O, and bursts of log lines cost one disk write instead of many.
 * Because only that one thread touches the files, writes and rotation can't interleave.
 */
object AppLogger {

    private const val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2 MB
    private const val CURRENT_LOG_FILE = "TrickTrack_current_log.txt"
    private const val OLD_LOG_FILE = "TrickTrack_old_log_backup.txt"
    private const val READ_TIMEOUT_SECONDS = 5L

    // DateTimeFormatter is thread-safe, unlike SimpleDateFormat.
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private val writer: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AppLogger").apply { isDaemon = true }
    }

    private val lock = Any()
    private val pending = StringBuilder() // guarded by lock
    private var flushScheduled = false // guarded by lock

    @Volatile
    private var logDir: File? = null

    fun init(context: Context) {
        val dir = File(context.filesDir, "logs")
        writer.execute { if (!dir.exists()) dir.mkdirs() }
        logDir = dir
        log("AppLogger", "Logger initialized")
    }

    fun log(tag: String, message: String) {
        Log.d(tag, message)

        val line = "${LocalDateTime.now().format(timestampFormat)} $tag: $message\n"
        synchronized(lock) {
            pending.append(line)
            if (flushScheduled) return
            flushScheduled = true
        }
        writer.execute(::flushPending)
    }

    /** Runs on the writer thread only. */
    private fun flushPending() {
        val text: String
        synchronized(lock) {
            text = pending.toString()
            pending.setLength(0)
            flushScheduled = false
        }
        val dir = logDir ?: return // log() before init(): the line is dropped from the file but still in logcat
        if (text.isEmpty()) return
        try {
            val logFile = File(dir, CURRENT_LOG_FILE)
            if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                val oldLogFile = File(dir, OLD_LOG_FILE)
                if (oldLogFile.exists()) {
                    oldLogFile.delete()
                }
                logFile.renameTo(oldLogFile)
            }
            logFile.appendText(text)
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to write log file: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Returns the old and current log file contents, including lines still queued.
     * Blocks briefly while the writer thread catches up, so call it off the main thread when possible.
     */
    fun getAllLogs(): String {
        val dir = logDir ?: return ""
        return try {
            writer.submit<String> {
                flushPending()
                val oldLogFile = File(dir, OLD_LOG_FILE)
                val currentLogFile = File(dir, CURRENT_LOG_FILE)
                val oldLogs = if (oldLogFile.exists()) oldLogFile.readText() else ""
                val currentLogs = if (currentLogFile.exists()) currentLogFile.readText() else ""
                oldLogs + currentLogs
            }.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to read log files: ${e.javaClass.simpleName}")
            ""
        }
    }

    /** Deletes both log files, including lines still queued. */
    fun clearLogs() {
        val dir = logDir ?: return
        try {
            writer.submit {
                synchronized(lock) { pending.setLength(0) }
                File(dir, OLD_LOG_FILE).delete()
                File(dir, CURRENT_LOG_FILE).delete()
            }.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to clear log files: ${e.javaClass.simpleName}")
        }
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
