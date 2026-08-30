package ch.opum.tricktrack.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.ui.DialogAcceptButton
import ch.opum.tricktrack.ui.DialogDeclineButton
import ch.opum.tricktrack.ui.TripsViewModel
import kotlinx.coroutines.launch

@Composable
fun ExportConfigDialog(
    viewModel: TripsViewModel,
    onDismiss: () -> Unit,
) {
    val exportColumns by viewModel.exportColumns.collectAsState()
    val expenseTrackingEnabled by viewModel.expenseTrackingEnabled.collectAsState()

    // A map to hold the display name and the key for each column
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

    // Temporary state for the checkboxes within the dialog
    var tempSelectedColumns by remember { mutableStateOf(exportColumns) }

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
            Text(
                text = stringResource(R.string.settings_export_fields_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogDeclineButton(onClick = onDismiss)
                Spacer(modifier = Modifier.width(12.dp))
                DialogAcceptButton(
                    onClick = {
                        viewModel.setExportColumns(tempSelectedColumns)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                )
            }
        }
    }
}
