package ch.opum.tricktrack.ui.review

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.CarBrandHelper
import ch.opum.tricktrack.data.DistanceUnit
import ch.opum.tricktrack.data.TripWithVehicle
import ch.opum.tricktrack.data.VehicleEntity
import ch.opum.tricktrack.ui.LicensePlateBadge
import ch.opum.tricktrack.ui.StyledAddress
import ch.opum.tricktrack.ui.ThousandsSeparatorTransformation
import ch.opum.tricktrack.ui.TimelineNode
import ch.opum.tricktrack.ui.TripType
import ch.opum.tricktrack.ui.TripsViewModel
import ch.opum.tricktrack.util.DistanceFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(viewModel: TripsViewModel) {
    val groupedTrips by viewModel.groupedReviewTrips.collectAsState()
    val isOdometerModeEnabled by viewModel.isOdometerModeEnabled.collectAsState()
    val allVehicles by viewModel.allVehicles.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()

    if (groupedTrips.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.review_no_trips),
                style = MaterialTheme.typography.bodyLarge, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            groupedTrips.forEach { group ->
                stickyHeader {
                    ReviewListHeader(
                        date = group.date,
                        tripCount = group.trips.size,
                        totalDistance = group.totalDistance,
                        isOdometerModeEnabled = isOdometerModeEnabled,
                        distanceUnit = distanceUnit
                    )
                }
                items(group.trips, key = { it.trip.id }) { tripWithVehicle ->
                    ReviewTripCard(
                        tripWithVehicle = tripWithVehicle,
                        allVehicles = allVehicles,
                        isOdometerModeEnabled = isOdometerModeEnabled,
                        distanceUnit = distanceUnit,
                        onApprove = { finalType, selectedVehicle, endOdometer, description ->
                            viewModel.approveTrip(
                                trip = tripWithVehicle.trip.copy(vehicleId = selectedVehicle?.id),
                                finalType = finalType,
                                endOdometer = endOdometer,
                                description = description
                            )
                        },
                        onDiscard = {
                            viewModel.deleteTrip(tripWithVehicle.trip)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewListHeader(
    date: Long,
    tripCount: Int,
    totalDistance: Double,
    isOdometerModeEnabled: Boolean,
    distanceUnit: DistanceUnit
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yy", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateFormatter.format(Date(date)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        val formattedDistance = DistanceFormatter.formatShort(totalDistance, distanceUnit)
        val distanceText = if (isOdometerModeEnabled) {
            // In odometer mode, total distance might be slightly different if user hasn't entered all yet
            // But we display what we have.
            stringResource(R.string.review_trip_count_and_distance, tripCount, formattedDistance)
        } else {
            stringResource(R.string.review_trip_count_and_distance, tripCount, formattedDistance)
        }
        Text(
            text = distanceText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReviewTripCard(
    tripWithVehicle: TripWithVehicle,
    allVehicles: List<VehicleEntity>,
    isOdometerModeEnabled: Boolean,
    distanceUnit: DistanceUnit,
    onApprove: (TripType, VehicleEntity?, Double?, String?) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trip = tripWithVehicle.trip
    var selectedType by remember { mutableStateOf(if (trip.type == "Business") TripType.BUSINESS else TripType.PERSONAL) }
    var selectedVehicle by remember(tripWithVehicle) { mutableStateOf(tripWithVehicle.vehicle) }
    var vehicleExpanded by remember { mutableStateOf(value = false) }
    var odometerText by remember(tripWithVehicle, distanceUnit) { 
        mutableStateOf(
            trip.endOdometer?.let {
                val converted = DistanceFormatter.convert(it, distanceUnit)
                "%.0f".format(converted)
            } ?: ""
        ) 
    }
    var note by remember { mutableStateOf("") }

    val context = LocalContext.current
    val odometerValue = odometerText.toDoubleOrNull() ?: 0.0
    val isOdometerError = isOdometerModeEnabled && selectedVehicle != null && 
        (odometerValue < DistanceFormatter.convert(selectedVehicle!!.currentOdometer, distanceUnit))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            // Header: Type, Vehicle, and Distance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tripTypes = listOf(stringResource(R.string.trip_type_business), stringResource(R.string.trip_type_personal))
                    val icons = listOf(Icons.Default.Work, Icons.Default.Person)
                    
                    Icon(
                        imageVector = if (selectedType == TripType.BUSINESS) Icons.Default.Work else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedType == TripType.BUSINESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Box(
                            modifier = Modifier
                                .clickable { vehicleExpanded = true }
                        ) {
                            if (selectedVehicle != null) {
                                LicensePlateBadge(selectedVehicle!!, showDropdownIndicator = true)
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(R.string.favourites_tab_vehicles),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = vehicleExpanded,
                            onDismissRequest = { vehicleExpanded = false }
                        ) {
                            allVehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        selectedVehicle = vehicle
                                        vehicleExpanded = false
                                    }
                                )
                            }
                            if (selectedVehicle != null) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.clear_text), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        selectedVehicle = null
                                        vehicleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    if (!trip.isAutomatic) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Manual Trip",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (trip.trigger == "BLUETOOTH") {
                         Spacer(modifier = Modifier.width(8.dp))
                         Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Triggered",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isOdometerModeEnabled) {
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { newValue ->
                            if ((newValue.length <= 8) && newValue.all { char -> char.isDigit() }) {
                                odometerText = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.end_odometer_label)) },
                        modifier = Modifier.width(140.dp),
                        isError = isOdometerError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorTransformation(),
                        suffix = { Text(DistanceFormatter.getUnitSuffix(distanceUnit)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = DistanceFormatter.format(trip.distance, distanceUnit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isOdometerModeEnabled && (selectedVehicle != null)) {
                val calcDistance = (odometerValue - DistanceFormatter.convert(selectedVehicle!!.currentOdometer, distanceUnit)).coerceAtLeast(0.0)
                val formattedCalc = DistanceFormatter.format(calcDistance, distanceUnit)
                Text(
                    text = "Calculated: $formattedCalc",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOdometerError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp, end = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val tripTypes = listOf(stringResource(R.string.trip_type_business), stringResource(R.string.trip_type_personal))
                val icons = listOf(Icons.Default.Work, Icons.Default.Person)
                tripTypes.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = tripTypes.size
                        ),
                        onClick = { selectedType = if (index == 0) TripType.BUSINESS else TripType.PERSONAL },
                        selected = (index == 0) == (selectedType == TripType.BUSINESS),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline
                        ),
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

            // Timeline Content
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Column 1: Visual Timeline
                TimelineNode()

                // Column 2: Data
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val timeFormatter = SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale)

                    // Start Point
                    StyledAddress(
                        time = timeFormatter.format(trip.date),
                        address = trip.startLoc
                    )

                    // End Point
                    StyledAddress(
                        time = timeFormatter.format(Date(trip.endDate)),
                        address = trip.endLoc
                    )
                }
            }


            // Footer Action Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (note.isEmpty()) {
                                Text(
                                    text = "Note...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            BasicTextField(
                                value = note,
                                onValueChange = { note = it },
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = onDiscard,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.review_discard_button),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = { onApprove(selectedType, selectedVehicle, DistanceFormatter.toKm(odometerValue, distanceUnit) , note) },
                        enabled = !isOdometerError,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.review_approve_trip),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
