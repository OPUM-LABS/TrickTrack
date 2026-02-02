package ch.opum.tricktrack.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.ui.TripsViewModel

@Composable
fun ExportConfigDialog(
    viewModel: TripsViewModel,
    onDismiss: () -> Unit
) {
    val exportColumns by viewModel.exportColumns.collectAsState()
    val expenseTrackingEnabled by viewModel.expenseTrackingEnabled.collectAsState()
    val hasDrivers by viewModel.hasDrivers.collectAsState()
    val hasCompanies by viewModel.hasCompanies.collectAsState()
    val hasVehicles by viewModel.hasVehicles.collectAsState()
    val includeDriver by viewModel.exportIncludeDriver.collectAsState()
    val includeCompany by viewModel.exportIncludeCompany.collectAsState()
    val includeVehicle by viewModel.exportIncludeVehicle.collectAsState()

    // A map to hold the display name and the key for each column
    val allColumns = remember {
        mapOf(
            "DATE" to R.string.export_column_date,
            "TIME" to R.string.export_column_time,
            "START_LOCATION" to R.string.export_column_start_location,
            "END_LOCATION" to R.string.export_column_end_location,
            "DISTANCE" to R.string.export_column_distance,
            "TYPE" to R.string.export_column_type
        )
    }

    // Temporary state for the checkboxes within the dialog
    var tempSelectedColumns by remember { mutableStateOf(exportColumns) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_export_fields_title)) },
        text = {
            Column {
                allColumns.forEach { (key, stringResId) ->
                    val isEnabled = key != "DATE"
                    val isChecked = tempSelectedColumns.contains(key)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) {
                                tempSelectedColumns = if (isChecked) {
                                    tempSelectedColumns - key
                                } else {
                                    tempSelectedColumns + key
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null, // Handled by the row's clickable modifier
                            enabled = isEnabled
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(stringResId),
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Driver
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasDrivers) {
                            viewModel.toggleIncludeDriver()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = includeDriver && hasDrivers,
                        onCheckedChange = null,
                        enabled = hasDrivers
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.export_column_driver),
                            color = if (hasDrivers) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        if (!hasDrivers) {
                            Text(
                                text = stringResource(R.string.export_no_entries),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Company
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasCompanies) {
                            viewModel.toggleIncludeCompany()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = includeCompany && hasCompanies,
                        onCheckedChange = null,
                        enabled = hasCompanies
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.export_column_company),
                            color = if (hasCompanies) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        if (!hasCompanies) {
                            Text(
                                text = stringResource(R.string.export_no_entries),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Vehicle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasVehicles) {
                            viewModel.toggleIncludeVehicle()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = includeVehicle && hasVehicles,
                        onCheckedChange = null,
                        enabled = hasVehicles
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.export_column_vehicle),
                            color = if (hasVehicles) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        if (!hasVehicles) {
                            Text(
                                text = stringResource(R.string.export_no_entries),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Expenses
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = expenseTrackingEnabled) {
                            tempSelectedColumns = if (tempSelectedColumns.contains("EXPENSES")) {
                                tempSelectedColumns - "EXPENSES"
                            } else {
                                tempSelectedColumns + "EXPENSES"
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = tempSelectedColumns.contains("EXPENSES"),
                        onCheckedChange = null,
                        enabled = expenseTrackingEnabled
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.export_column_expenses),
                        color = if (expenseTrackingEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.setExportColumns(tempSelectedColumns)
                onDismiss()
            }) {
                Text(stringResource(R.string.button_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
