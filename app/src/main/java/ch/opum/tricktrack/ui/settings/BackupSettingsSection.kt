package ch.opum.tricktrack.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsSection(
    viewModel: SettingsViewModel
) {
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState(initial = false)
    val backupFrequency by viewModel.backupFrequency.collectAsState(initial = "DAILY")
    val backupDayOfWeek by viewModel.backupDayOfWeek.collectAsState(initial = Calendar.MONDAY)
    val backupDayOfMonth by viewModel.backupDayOfMonth.collectAsState(initial = 1)
    val backupFolderUri by viewModel.backupFolderUri.collectAsState(initial = null)

    var frequencyExpanded by remember { mutableStateOf(false) }
    var dayOfWeekExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setBackupFolderUri(uri)
            }
        }
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Automatic Backups", modifier = Modifier.weight(1f))
            Switch(
                checked = autoBackupEnabled,
                onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
            )
        }

        if (autoBackupEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { launcher.launch(null) }) {
                Text("Select Backup Folder")
            }

            backupFolderUri?.let {
                Text("Selected folder: ${getFolderName(context, it)}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                TextField(
                    value = backupFrequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Backup Frequency") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded)
                    },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Daily") },
                        onClick = {
                            viewModel.setBackupFrequency("DAILY")
                            frequencyExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Weekly") },
                        onClick = {
                            viewModel.setBackupFrequency("WEEKLY")
                            frequencyExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Monthly") },
                        onClick = {
                            viewModel.setBackupFrequency("MONTHLY")
                            frequencyExpanded = false
                        }
                    )
                }
            }

            when (backupFrequency) {
                "WEEKLY" -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = dayOfWeekExpanded,
                        onExpandedChange = { dayOfWeekExpanded = !dayOfWeekExpanded }
                    ) {
                        TextField(
                            value = dayOfWeekToString(backupDayOfWeek),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day of Week") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayOfWeekExpanded)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dayOfWeekExpanded,
                            onDismissRequest = { dayOfWeekExpanded = false }
                        ) {
                            (Calendar.MONDAY..Calendar.SATURDAY).forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(dayOfWeekToString(day)) },
                                    onClick = {
                                        viewModel.setBackupDayOfWeek(day)
                                        dayOfWeekExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(dayOfWeekToString(Calendar.SUNDAY)) },
                                onClick = {
                                    viewModel.setBackupDayOfWeek(Calendar.SUNDAY)
                                    dayOfWeekExpanded = false
                                }
                            )
                        }
                    }
                }
                "MONTHLY" -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Day of Month: $backupDayOfMonth")
                    Slider(
                        value = backupDayOfMonth.toFloat(),
                        onValueChange = { viewModel.setBackupDayOfMonth(it.toInt()) },
                        valueRange = 1f..28f,
                        steps = 26
                    )
                }
            }
        }
    }
}

private fun dayOfWeekToString(day: Int): String {
    return when (day) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        Calendar.SUNDAY -> "Sunday"
        else -> ""
    }
}

private fun getFolderName(context: Context, uriString: String): String {
    val uri = Uri.parse(uriString)
    val documentFile = DocumentFile.fromTreeUri(context, uri)
    return documentFile?.name ?: uriString
}