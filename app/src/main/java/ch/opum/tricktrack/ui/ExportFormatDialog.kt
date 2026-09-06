package ch.opum.tricktrack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onExportCsvClicked: (exportAll: Boolean) -> Unit,
    onExportPdfClicked: (exportAll: Boolean) -> Unit,
    viewModel: TripsViewModel,
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

    val isFilterActive by viewModel.isFilterActive.collectAsState()
    val filteredTrips by viewModel.confirmedTrips.collectAsState()
    val totalTripCount by viewModel.allConfirmedTripsCount.collectAsState()

    val exportColumns by viewModel.exportColumns.collectAsState()
    val expenseTrackingEnabled by viewModel.expenseTrackingEnabled.collectAsState()
    val showSettingsHelp by viewModel.showSettingsHelp.collectAsState()

    var selectedFormat by remember { mutableStateOf("PDF") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var exportAll by remember { mutableStateOf(!isFilterActive) }

    val allColumns = remember {
        mapOf(
            "DATE" to R.string.export_column_date,
            "TIME" to R.string.export_column_time,
            "START_LOCATION" to R.string.export_column_start_location,
            "END_LOCATION" to R.string.export_column_end_location,
            "DISTANCE" to R.string.export_column_distance,
            "TYPE" to R.string.export_column_type,
            "VEHICLE" to R.string.favourites_tab_vehicles
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
                IconButton(onClick = { viewModel.toggleShowSettingsHelp() }) {
                    Icon(
                        imageVector = if (showSettingsHelp) Icons.AutoMirrored.Filled.Help else Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.action_toggle_help),
                        tint = if (showSettingsHelp) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trip Scope Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.export_scope_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isFilterActive) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !exportAll,
                                onClick = { exportAll = false },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.export_scope_filtered_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.export_scope_filtered_count, filteredTrips.size, totalTripCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            SegmentedButton(
                                selected = exportAll,
                                onClick = { exportAll = true },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.export_scope_all_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.export_scope_all_count, totalTripCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.export_scope_all_count, totalTripCount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Format Selection Cards
            Text(
                text = stringResource(R.string.export_format_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PDF Format Card
                OutlinedCard(
                    onClick = {
                        selectedFormat = "PDF"
                        selectedTabIndex = 0 // Open Document Header tab when switching to PDF
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedFormat == "PDF") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (selectedFormat == "PDF") 2.dp else 1.dp,
                        color = if (selectedFormat == "PDF") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = if (selectedFormat == "PDF") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.export_format_pdf),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (showSettingsHelp) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.export_format_pdf_sub),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // CSV Format Card
                OutlinedCard(
                    onClick = {
                        selectedFormat = "CSV"
                        selectedTabIndex = 1 // Jump straight to Columns tab for CSV
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedFormat == "CSV") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (selectedFormat == "CSV") 2.dp else 1.dp,
                        color = if (selectedFormat == "CSV") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            tint = if (selectedFormat == "CSV") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.export_format_csv),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (showSettingsHelp) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.export_format_csv_sub),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two-Tabbed Options Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { if (selectedFormat != "CSV") selectedTabIndex = 0 },
                            enabled = selectedFormat != "CSV",
                            text = {
                                Text(
                                    text = stringResource(R.string.export_tab_header),
                                    color = if (selectedFormat == "CSV") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text(stringResource(R.string.export_tab_columns)) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (selectedTabIndex == 0 && selectedFormat != "CSV") {
                            // Tab 0: Document Header Info (Driver, Company, Vehicle Dropdowns)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            }
                        } else {
                            // Tab 1: Columns & Fields Checkboxes
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allColumns.forEach { (key, stringResId) ->
                                    val isMandatory = key == "DATE"
                                    val isChecked = exportColumns.contains(key)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isMandatory) {
                                                val newSet = if (isChecked) exportColumns - key else exportColumns + key
                                                viewModel.setExportColumns(newSet)
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null,
                                            enabled = !isMandatory
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(stringResId),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (!isMandatory) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                // Expenses
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = expenseTrackingEnabled) {
                                            val newSet = if (exportColumns.contains("EXPENSES")) exportColumns - "EXPENSES" else exportColumns + "EXPENSES"
                                            viewModel.setExportColumns(newSet)
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = exportColumns.contains("EXPENSES"),
                                        onCheckedChange = null,
                                        enabled = expenseTrackingEnabled
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.export_column_expenses),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (expenseTrackingEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (selectedFormat == "PDF") {
                        onExportPdfClicked(exportAll)
                    } else {
                        onExportCsvClicked(exportAll)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (selectedFormat == "PDF") Icons.Default.PictureAsPdf else Icons.AutoMirrored.Filled.Article,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(if (selectedFormat == "PDF") R.string.export_action_pdf else R.string.export_action_csv),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                DialogDeclineButton(onClick = onDismiss)
            }
        }
    }
}
