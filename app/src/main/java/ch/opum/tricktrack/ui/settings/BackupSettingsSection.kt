package ch.opum.tricktrack.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import java.net.URLDecoder
import java.util.Calendar
import androidx.core.net.toUri

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

    LocalContext.current

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
            Text(stringResource(R.string.settings_auto_backup_enable), modifier = Modifier.weight(1f))
            Switch(
                checked = autoBackupEnabled,
                onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
            )
        }

        if (autoBackupEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { launcher.launch(null) }) {
                Text(stringResource(R.string.settings_auto_backup_folder))
            }

            backupFolderUri?.let {
                Text(stringResource(R.string.settings_auto_backup_folder_selected, it.toUri().toUserFriendlyString()))
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                TextField(
                    value = when (backupFrequency) {
                        "DAILY" -> stringResource(R.string.settings_auto_backup_frequency_daily)
                        "WEEKLY" -> stringResource(R.string.settings_auto_backup_frequency_weekly)
                        "MONTHLY" -> stringResource(R.string.settings_auto_backup_frequency_monthly)
                        else -> ""
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_auto_backup_frequency)) },
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
                        text = { Text(stringResource(R.string.settings_auto_backup_frequency_daily)) },
                        onClick = {
                            viewModel.setBackupFrequency("DAILY")
                            frequencyExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_auto_backup_frequency_weekly)) },
                        onClick = {
                            viewModel.setBackupFrequency("WEEKLY")
                            frequencyExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_auto_backup_frequency_monthly)) },
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
                            label = { Text(stringResource(R.string.settings_auto_backup_day_of_week)) },
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
                    Text(stringResource(R.string.settings_auto_backup_day_of_month, backupDayOfMonth))
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

fun Uri.toUserFriendlyString(): String {
    try {
        // Android SAF URIs usually look like: content://com.android.externalstorage.documents/tree/primary%3ADownloads%2FBackup
        val pathPart = this.path?.split(":")?.lastOrNull() ?: return "Unknown Location"

        val decodedPath = URLDecoder.decode(pathPart, "UTF-8")

        return if (this.toString().contains("primary")) {
            "Internal Storage > $decodedPath"
        } else {
            // It's likely an SD Card or External Drive
            "SD Card > $decodedPath"
        }
    } catch (e: Exception) {
        return "Custom Folder"
    }
}