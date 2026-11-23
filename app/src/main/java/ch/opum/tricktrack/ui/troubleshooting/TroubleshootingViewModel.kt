package ch.opum.tricktrack.ui.troubleshooting

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.opum.tricktrack.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TroubleshootingViewModel(application: Application) :
    AndroidViewModel(application) {

    val logLines = mutableStateListOf<String>()

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            // Show the app's own log file rather than logcat: release builds strip Log.d from
            // logcat, and the file is also exactly what "Share" exports.
            val content = withContext(Dispatchers.IO) { AppLogger.getAllLogs().lines().filter { it.isNotBlank() } }
            logLines.clear()
            logLines.addAll(content)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AppLogger.clearLogs() }
            loadLogs() // Refresh to show empty logs
        }
    }

    fun shareLogs(activityContext: Context) { // Accept Activity Context here
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) { AppLogger.exportLogs(getApplication()) } // Application context for file operations
            uri?.let {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    // Removed FLAG_ACTIVITY_NEW_TASK as it's not needed with an Activity context for share intents
                }
                activityContext.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share logs"
                    )
                ) // Use Activity context to start
            } ?: run {
                Toast.makeText(getApplication(), "Failed to export logs", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
