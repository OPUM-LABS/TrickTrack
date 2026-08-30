package ch.opum.tricktrack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.CarBrandHelper
import ch.opum.tricktrack.data.VehicleEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.compose.ui.platform.LocalLocale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentFilterState: FilterState,
    allVehicles: List<VehicleEntity>,
    onApplyFilter: (FilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    var keyword by remember { mutableStateOf(currentFilterState.keyword) }
    var selectedType by remember { mutableStateOf(currentFilterState.type.takeIf { it != TripType.ALL }) }
    var startDate by remember { mutableStateOf(currentFilterState.startDate) }
    var endDate by remember { mutableStateOf(currentFilterState.endDate) }
    var selectedVehicleIds by remember { mutableStateOf(currentFilterState.vehicleIds) }

    var showRangePicker by remember { mutableStateOf(value = false) }
    var vehicleExpanded by remember { mutableStateOf(false) }

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
                text = stringResource(R.string.filter_trips_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            ClearableTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(stringResource(R.string.filter_keyword_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.filter_trip_type_label), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val cornerRadius = 50.dp

                // "All" Button
                TripTypeButton(
                    text = stringResource(R.string.filter_trip_type_all),
                    isSelected = selectedType == null,
                    onClick = { selectedType = null },
                    shape = RoundedCornerShape(
                        topStart = cornerRadius,
                        bottomStart = cornerRadius
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .then(if (selectedType == null) Modifier.zIndex(1f) else Modifier)
                )

                // "Business" Button
                TripTypeButton(
                    text = stringResource(R.string.trip_type_business),
                    isSelected = selectedType == TripType.BUSINESS,
                    onClick = { selectedType = TripType.BUSINESS },
                    shape = RectangleShape,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (selectedType == TripType.BUSINESS) Modifier.zIndex(1f) else Modifier)
                )

                // "Personal" Button
                TripTypeButton(
                    text = stringResource(R.string.trip_type_personal),
                    isSelected = selectedType == TripType.PERSONAL,
                    onClick = { selectedType = TripType.PERSONAL },
                    shape = RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius),
                    modifier = Modifier
                        .weight(1f)
                        .then(if (selectedType == TripType.PERSONAL) Modifier.zIndex(1f) else Modifier)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = vehicleExpanded,
                onExpandedChange = { vehicleExpanded = it }
            ) {
                val context = LocalContext.current
                val displayText = if (selectedVehicleIds.isEmpty()) {
                    stringResource(R.string.filter_trip_type_all)
                } else if (selectedVehicleIds.size == 1) {
                    allVehicles.find { it.id == selectedVehicleIds.first() }?.licensePlate ?: ""
                } else {
                    stringResource(R.string.vehicles_selected_count, selectedVehicleIds.size)
                }

                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.favourites_tab_vehicles)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedVehicleIds.isNotEmpty()) {
                                IconButton(onClick = { selectedVehicleIds = emptySet() }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = vehicleExpanded,
                    onDismissRequest = { vehicleExpanded = false }
                ) {
                    allVehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedVehicleIds.contains(vehicle.id),
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val itemIconResId = vehicle.brand?.let { CarBrandHelper.getBrandIconResId(context, it) } ?: 0
                                    if (itemIconResId != 0) {
                                        Icon(
                                            painter = painterResource(id = itemIconResId),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(vehicle.licensePlate)
                                }
                            },
                            onClick = {
                                selectedVehicleIds = if (selectedVehicleIds.contains(vehicle.id)) {
                                    selectedVehicleIds - vehicle.id
                                } else {
                                    selectedVehicleIds + vehicle.id
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DateRangeSelectionField(
                startDate = startDate,
                endDate = endDate,
                onClick = {
                showRangePicker = true
            }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogResetButton(onClick = {
                    keyword = ""
                    selectedType = null
                    startDate = null
                    endDate = null
                    selectedVehicleIds = emptySet()
                })
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialogDeclineButton(onClick = onDismiss)
                    DialogAcceptButton(onClick = {
                        val endOfDay = endDate?.let {
                            val calendar = Calendar.getInstance()
                            calendar.apply {
                                timeInMillis = it
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis
                        }

                        val newState = FilterState(
                            keyword = keyword,
                            type = selectedType ?: TripType.ALL,
                            startDate = startDate,
                            endDate = endOfDay,
                            vehicleIds = selectedVehicleIds
                        )
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onApplyFilter(newState)
                            }
                        }
                    })
                }
            }
        }
    }

    if (showRangePicker) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate ?: today,
            initialSelectedEndDateMillis = endDate ?: today
        )

        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = dateRangePickerState.selectedStartDateMillis
                    endDate = dateRangePickerState.selectedEndDateMillis
                    showRangePicker = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = stringResource(R.string.filter_date_range_label),
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                headline = {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.titleMedium
                    ) {
                        DateRangePickerDefaults.DateRangePickerHeadline(
                            selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                            selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                            displayMode = dateRangePickerState.displayMode,
                            dateFormatter = DatePickerDefaults.dateFormatter(),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                },
                showModeToggle = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DateRangeSelectionField(
    startDate: Long?,
    endDate: Long?,
    onClick: () -> Unit
) {
    val formatter = SimpleDateFormat("dd/MM/yyyy", LocalLocale.current.platformLocale)
    val dateText = if ((startDate != null) && (endDate != null)) {
        "${formatter.format(Date(startDate))} - ${formatter.format(Date(endDate))}"
    } else if (startDate != null) {
        formatter.format(Date(startDate))
    } else {
        ""
    }

    Box {
        OutlinedTextField(
            value = dateText,
            onValueChange = {},
            label = { Text(stringResource(R.string.filter_date_range_label)) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
fun TripTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = shape,
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier
    ) {
        Text(text)
    }
}
