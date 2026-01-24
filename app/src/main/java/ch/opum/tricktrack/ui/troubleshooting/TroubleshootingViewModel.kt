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
import java.io.BufferedReader
import java.io.InputStreamReader

class TroubleshootingViewModel(application: Application) :
    AndroidViewModel(application) {

    val logLines = mutableStateListOf<String>()

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            val content = getLogcatLogs()
            logLines.clear()
            logLines.addAll(content)
        }
    }

    private suspend fun getLogcatLogs(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("logcat -d")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val log = mutableListOf<String>()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    log.add(line!!)
                }
                log
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            try {
                Runtime.getRuntime().exec("logcat -c")
                loadLogs() // Refresh to show empty logs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shareLogs(activityContext: Context) { // Accept Activity Context here
        viewModelScope.launch {
            val uri = AppLogger.exportLogs(getApplication()) // Use application context for file operations
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
