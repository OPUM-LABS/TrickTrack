package ch.opum.tricktrack.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.opum.tricktrack.R
import ch.opum.tricktrack.ui.troubleshooting.TroubleshootingViewModel
import kotlinx.coroutines.delay

@Composable
fun LogsDialog(onDismiss: () -> Unit, viewModel: TroubleshootingViewModel) {
    val logLines = viewModel.logLines
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.lastIndex)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadLogs()
            delay(5000)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Crucial for custom width
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Fill 90% of the now flexible Dialog's width
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.logs_title), style = MaterialTheme.typography.titleLarge)
                    Row {
                        IconButton(onClick = { viewModel.loadLogs() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_logs_cd))
                        }
                        IconButton(onClick = { viewModel.shareLogs(context) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_logs_cd))
                        }
                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_logs_cd))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        items(logLines) { logEntry ->
                            Text(
                                text = logEntry,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.button_close))
                    }
                }
            }
        }
    }
}
