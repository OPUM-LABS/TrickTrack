package ch.opum.tricktrack.ui.troubleshooting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TroubleshootingScreen(viewModel: TroubleshootingViewModel) {
    val logLines = viewModel.logLines
    val listState = rememberLazyListState() // State for LazyColumn

    // Auto-scroll to the last item when logLines change
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize() // Ensure the column fills the available space
            .padding(16.dp) // Apply padding to the column
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState // Assign the list state
        ) {
            items(logLines) { logEntry ->
                Text(
                    text = logEntry,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}
