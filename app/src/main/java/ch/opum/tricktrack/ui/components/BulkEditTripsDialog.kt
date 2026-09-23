package ch.opum.tricktrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.VehicleEntity
import ch.opum.tricktrack.ui.ClearableTextField
import ch.opum.tricktrack.ui.DialogAcceptButton
import ch.opum.tricktrack.ui.DialogDeclineButton
import ch.opum.tricktrack.data.CarBrandHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkEditTripsDialog(
    selectedTrips: List<Trip>,
    allVehicles: List<VehicleEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        targetType: String?,
        updateType: Boolean,
        targetVehicleId: Int?,
        updateVehicle: Boolean,
        targetDescription: String?,
        updateDescription: Boolean
    ) -> Unit
) {
    if (selectedTrips.isEmpty()) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Detect if all selected trips share the same type
    val unanimousType = remember(selectedTrips) {
        val types = selectedTrips.map { it.type }.distinct()
        if (types.size == 1) types.first() else null
    }

    // Detect if all selected trips share the same vehicle
    val unanimousVehicleId = remember(selectedTrips) {
        val vehicleIds = selectedTrips.map { it.vehicleId }.distinct()
        if (vehicleIds.size == 1) vehicleIds.first() else null
    }

    // Detect if all selected trips share the same description
    val unanimousDescription = remember(selectedTrips) {
        val descriptions = selectedTrips.map { it.description ?: "" }.distinct()
        if (descriptions.size == 1) descriptions.first() else null
    }

    var updateType by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(unanimousType ?: "Business") }

    var updateVehicle by remember { mutableStateOf(false) }
    var selectedVehicle by remember {
        mutableStateOf<VehicleEntity?>(
            unanimousVehicleId?.let { id -> allVehicles.find { it.id == id } }
        )
    }
    var vehicleExpanded by remember { mutableStateOf(false) }

    var updateDescription by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(unanimousDescription ?: "") }

    val hasChanges = updateType || updateVehicle || updateDescription

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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = stringResource(R.string.bulk_edit_trips_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.bulk_edit_trips_count, selectedTrips.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Section 1: Classification (Trip Type) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { updateType = !updateType }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = updateType,
                    onCheckedChange = { updateType = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bulk_edit_change_type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val tripTypes = listOf(stringResource(R.string.trip_type_business), stringResource(R.string.trip_type_personal))
            val icons = listOf(Icons.Default.Work, Icons.Default.Person)

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (updateType) 1f else 0.45f)
            ) {
                tripTypes.forEachIndexed { index, label ->
                    val isSelected = (if (index == 0) "Business" else "Personal") == selectedType
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tripTypes.size),
                        onClick = {
                            selectedType = if (index == 0) "Business" else "Personal"
                            updateType = true
                        },
                        selected = isSelected,
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        icon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = label,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Section 2: Vehicle ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { updateVehicle = !updateVehicle }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = updateVehicle,
                    onCheckedChange = { updateVehicle = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bulk_edit_change_vehicle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            ExposedDropdownMenuBox(
                expanded = vehicleExpanded,
                onExpandedChange = {
                    vehicleExpanded = it
                    if (it) updateVehicle = true
                },
                modifier = Modifier.alpha(if (updateVehicle) 1f else 0.45f)
            ) {
                TextField(
                    value = selectedVehicle?.licensePlate ?: if (updateVehicle) stringResource(R.string.bulk_edit_no_vehicle) else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.favourites_tab_vehicles)) },
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
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedVehicle != null) {
                                IconButton(onClick = {
                                    selectedVehicle = null
                                    updateVehicle = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.clear_text)
                                    )
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded)
                        }
                    },
                    leadingIcon = {
                        val iconResId = selectedVehicle?.brand?.let { CarBrandHelper.getBrandIconResId(context, it) } ?: 0
                        if (iconResId != 0) {
                            Icon(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = vehicleExpanded,
                    onDismissRequest = { vehicleExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.bulk_edit_no_vehicle))
                            }
                        },
                        onClick = {
                            selectedVehicle = null
                            updateVehicle = true
                            vehicleExpanded = false
                        }
                    )
                    allVehicles.forEach { veh ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val itemIconResId = veh.brand?.let { CarBrandHelper.getBrandIconResId(context, it) } ?: 0
                                    if (itemIconResId != 0) {
                                        Icon(
                                            painter = painterResource(id = itemIconResId),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(veh.licensePlate)
                                }
                            },
                            onClick = {
                                selectedVehicle = veh
                                updateVehicle = true
                                vehicleExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Section 3: Description ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { updateDescription = !updateDescription }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = updateDescription,
                    onCheckedChange = { updateDescription = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bulk_edit_change_description),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(modifier = Modifier.alpha(if (updateDescription) 1f else 0.45f)) {
                ClearableTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        updateDescription = true
                    },
                    label = { Text(stringResource(R.string.description_optional_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    isFilled = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogDeclineButton(onClick = onDismiss)
                Spacer(modifier = Modifier.width(12.dp))
                DialogAcceptButton(
                    enabled = hasChanges,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onConfirm(
                                    selectedType,
                                    updateType,
                                    selectedVehicle?.id,
                                    updateVehicle,
                                    description,
                                    updateDescription
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
