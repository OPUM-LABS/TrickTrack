package ch.opum.tricktrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.ui.settings.ExportConfigDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onExportCsvClicked: () -> Unit,
    onExportPdfClicked: () -> Unit,
    viewModel: TripsViewModel,
) {
    val drivers by viewModel.allDrivers.collectAsState()
    val companies by viewModel.allCompanies.collectAsState()
    val vehicles by viewModel.allVehicles.collectAsState()

    var driverExpanded by remember { mutableStateOf(value = false) }
    var companyExpanded by remember { mutableStateOf(false) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    val includeDriver by viewModel.exportIncludeDriver.collectAsState()
    val includeCompany by viewModel.exportIncludeCompany.collectAsState()
    val includeVehicle by viewModel.exportIncludeVehicle.collectAsState()

    val hasDrivers by viewModel.hasDrivers.collectAsState()
    val hasCompanies by viewModel.hasCompanies.collectAsState()
    val hasVehicles by viewModel.hasVehicles.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showConfigDialog) {
        ExportConfigDialog(
            viewModel = viewModel,
            onDismiss = {
                showConfigDialog = false
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.export_trips_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showConfigDialog = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = stringResource(R.string.settings_export_fields_configure_cd)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
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
                        OutlinedTextField(
                            value = viewModel.selectedDriver?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.export_column_driver)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            enabled = includeDriver && hasDrivers,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
                        OutlinedTextField(
                            value = viewModel.selectedCompany?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.export_column_company)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            enabled = includeCompany && hasCompanies,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
                        OutlinedTextField(
                            value = viewModel.selectedVehicle?.licensePlate ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.export_column_vehicle)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            enabled = includeVehicle && hasVehicles,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
                    icon = Icons.AutoMirrored.Filled.Article,
                    onClick = {
                        onExportCsvClicked()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                )
                ExportButton(
                    text = stringResource(R.string.export_as_pdf),
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        onExportPdfClicked()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                DialogDeclineButton(onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ExportButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
