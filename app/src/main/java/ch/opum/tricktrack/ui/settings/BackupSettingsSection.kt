package ch.opum.tricktrack.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import ch.opum.tricktrack.R
import ch.opum.tricktrack.ui.TimePickerDialog
import ch.opum.tricktrack.ui.components.SettingHelpBox
import java.net.URLDecoder
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsSection(
    viewModel: SettingsViewModel,
    showSettingsHelp: Boolean = false
) {
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState(initial = false)
    val backupFrequency by viewModel.backupFrequency.collectAsState(initial = "DAILY")
    val backupDayOfWeek by viewModel.backupDayOfWeek.collectAsState(initial = Calendar.MONDAY)
    val backupDayOfMonth by viewModel.backupDayOfMonth.collectAsState(initial = 1)
    val backupFolderUri by viewModel.backupFolderUri.collectAsState(initial = null)
    val backupTimeHour by viewModel.backupTimeHour.collectAsState(initial = 2)
    val backupTimeMinute by viewModel.backupTimeMinute.collectAsState(initial = 0)

    var dayOfWeekExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setBackupFolderUri(uri)
            }
        }
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = backupTimeHour,
            initialMinute = backupTimeMinute
        )

        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = stringResource(R.string.settings_auto_backup_time),
            confirmButton = {
                Button(onClick = {
                    viewModel.setBackupTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_auto_backup_enable),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = autoBackupEnabled,
                onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
            )
        }
        if (showSettingsHelp) {
            SettingHelpBox(helpText = stringResource(R.string.settings_help_auto_backup_toggle))
        }

        if (autoBackupEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            val isFolderMissing = backupFolderUri.isNullOrBlank()
            val cardBorderColor = if (isFolderMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            val textColor = if (isFolderMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            val iconTint = if (isFolderMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            // Interactive Backup Folder Surface Card
            Surface(
                onClick = { launcher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isFolderMissing) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_auto_backup_folder),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFolderMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val folderText = backupFolderUri?.toUri()?.toUserFriendlyString() ?: stringResource(R.string.settings_no_folder_selected)
                        Text(
                            text = folderText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = stringResource(R.string.settings_auto_backup_folder),
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (showSettingsHelp) {
                SettingHelpBox(helpText = stringResource(R.string.settings_help_auto_backup_folder_select))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Frequency Segmented Button Row
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_auto_backup_frequency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val frequencyOptions = listOf(
                    "DAILY" to stringResource(R.string.settings_auto_backup_frequency_daily),
                    "WEEKLY" to stringResource(R.string.settings_auto_backup_frequency_weekly),
                    "MONTHLY" to stringResource(R.string.settings_auto_backup_frequency_monthly)
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    frequencyOptions.forEachIndexed { index, (freqKey, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = frequencyOptions.size),
                            onClick = { viewModel.setBackupFrequency(freqKey) },
                            selected = backupFrequency == freqKey,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                activeBorderColor = Color.Transparent,
                                inactiveContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                disabledActiveBorderColor = Color.Transparent,
                                disabledInactiveBorderColor = Color.Transparent
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Text(label, maxLines = 1, modifier = Modifier.basicMarquee())
                        }
                    }
                }
            }
            if (showSettingsHelp) {
                SettingHelpBox(helpText = stringResource(R.string.settings_help_auto_backup_frequency))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backup Time Card
            val formattedTime = String.format(LocalLocale.current.platformLocale, "%02d:%02d", backupTimeHour, backupTimeMinute)
            Surface(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_auto_backup_time_approx),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = stringResource(R.string.settings_auto_backup_time_approx),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (showSettingsHelp) {
                SettingHelpBox(helpText = stringResource(R.string.settings_help_auto_backup_time_schedule))
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(16.dp),
                            colors = ExposedDropdownMenuDefaults.textFieldColors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
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
                    Text(
                        stringResource(R.string.settings_auto_backup_day_of_month, backupDayOfMonth),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
        val pathPart = this.path?.split(":")?.lastOrNull() ?: return "Unknown Location"
        val decodedPath = URLDecoder.decode(pathPart, "UTF-8")

        return if (this.toString().contains("primary")) {
            "Internal Storage > $decodedPath"
        } else {
            "SD Card > $decodedPath"
        }
    } catch (_: Exception) {
        return "Custom Folder"
    }
}
