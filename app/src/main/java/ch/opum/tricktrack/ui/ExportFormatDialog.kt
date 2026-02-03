package ch.opum.tricktrack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.ui.settings.ExportConfigDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onExportCsvClicked: () -> Unit,
    onExportPdfClicked: () -> Unit,
    viewModel: TripsViewModel
) {
    val drivers by viewModel.allDrivers.collectAsState()
    val companies by viewModel.allCompanies.collectAsState()
    val vehicles by viewModel.allVehicles.collectAsState()

    var driverExpanded by remember { mutableStateOf(false) }
    var companyExpanded by remember { mutableStateOf(false) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    val includeDriver by viewModel.exportIncludeDriver.collectAsState()
    val includeCompany by viewModel.exportIncludeCompany.collectAsState()
    val includeVehicle by viewModel.exportIncludeVehicle.collectAsState()

    val hasDrivers by viewModel.hasDrivers.collectAsState()
    val hasCompanies by viewModel.hasCompanies.collectAsState()
    val hasVehicles by viewModel.hasVehicles.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }

    if (showConfigDialog) {
        ExportConfigDialog(
            viewModel = viewModel,
            onDismiss = { showConfigDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.export_trips_title))
                IconButton(onClick = { showConfigDialog = true }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = stringResource(R.string.settings_export_fields_configure_cd)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Driver Dropdown with Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includeDriver && hasDrivers,
                        onCheckedChange = { viewModel.toggleIncludeDriver() },
                        enabled = hasDrivers
                    )
                    ExposedDropdownMenuBox(
                        expanded = driverExpanded,
                        onExpandedChange = { if (includeDriver) driverExpanded = !driverExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = viewModel.selectedDriver?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.export_column_driver)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = includeDriver && hasDrivers
                        )
                        ExposedDropdownMenu(
                            expanded = driverExpanded,
                            onDismissRequest = { driverExpanded = false }
                        ) {
                            drivers.forEach { driver ->
                                DropdownMenuItem(
                                    text = { Text(driver.name) },
                                    onClick = {
                                        viewModel.selectedDriver = driver
                                        driverExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Company Dropdown with Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includeCompany && hasCompanies,
                        onCheckedChange = { viewModel.toggleIncludeCompany() },
                        enabled = hasCompanies
                    )
                    ExposedDropdownMenuBox(
                        expanded = companyExpanded,
                        onExpandedChange = { if (includeCompany) companyExpanded = !companyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = viewModel.selectedCompany?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.export_column_company)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = includeCompany && hasCompanies
                        )
                        ExposedDropdownMenu(
                            expanded = companyExpanded,
                            onDismissRequest = { companyExpanded = false }
                        ) {
                            companies.forEach { company ->
                                DropdownMenuItem(
                                    text = { Text(company.name) },
                                    onClick = {
                                        viewModel.selectedCompany = company
                                        companyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Vehicle Dropdown with Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includeVehicle && hasVehicles,
                        onCheckedChange = { viewModel.toggleIncludeVehicle() },
                        enabled = hasVehicles
                    )
                    ExposedDropdownMenuBox(
                        expanded = vehicleExpanded,
                        onExpandedChange = { if (includeVehicle) vehicleExpanded = !vehicleExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = viewModel.selectedVehicle?.licensePlate ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.export_column_vehicle)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = includeVehicle && hasVehicles
                        )
                        ExposedDropdownMenu(
                            expanded = vehicleExpanded,
                            onDismissRequest = { vehicleExpanded = false }
                        ) {
                            vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text(vehicle.licensePlate) },
                                    onClick = {
                                        viewModel.selectedVehicle = vehicle
                                        vehicleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ExportButton(
                    text = stringResource(R.string.export_as_csv),
                    icon = Icons.Default.Article,
                    onClick = {
                        onExportCsvClicked()
                        onDismiss()
                    }
                )
                ExportButton(
                    text = stringResource(R.string.export_as_pdf),
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        onExportPdfClicked()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@Composable
private fun ExportButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(text)
        }
    }
}
