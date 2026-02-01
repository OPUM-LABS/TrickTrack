package ch.opum.tricktrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import ch.opum.tricktrack.data.CompanyEntity
import ch.opum.tricktrack.data.DriverEntity
import ch.opum.tricktrack.data.VehicleEntity

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

    val includeDriver by viewModel.userPreferencesRepository.exportIncludeDriver.collectAsState(initial = true)
    val includeCompany by viewModel.userPreferencesRepository.exportIncludeCompany.collectAsState(initial = true)
    val includeVehicle by viewModel.userPreferencesRepository.exportIncludeVehicle.collectAsState(initial = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_trips_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = driverExpanded,
                        onExpandedChange = { if (includeDriver) driverExpanded = !driverExpanded }
                    ) {
                        TextField(
                            value = viewModel.selectedDriver?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Driver") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = includeDriver
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
                    if (!includeDriver) {
                        Text(
                            text = stringResource(R.string.export_disabled_in_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Column {
                    ExposedDropdownMenuBox(
                        expanded = companyExpanded,
                        onExpandedChange = { if (includeCompany) companyExpanded = !companyExpanded }
                    ) {
                        TextField(
                            value = viewModel.selectedCompany?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Company") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = includeCompany
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
                    if (!includeCompany) {
                        Text(
                            text = stringResource(R.string.export_disabled_in_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Column {
                    ExposedDropdownMenuBox(
                        expanded = vehicleExpanded,
                        onExpandedChange = { if (includeVehicle) vehicleExpanded = !vehicleExpanded }
                    ) {
                        TextField(
                            value = viewModel.selectedVehicle?.licensePlate ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = includeVehicle
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
                    if (!includeVehicle) {
                        Text(
                            text = stringResource(R.string.export_disabled_in_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
