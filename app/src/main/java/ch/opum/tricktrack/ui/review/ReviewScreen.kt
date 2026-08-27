package ch.opum.tricktrack.ui.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.CarBrandHelper
import ch.opum.tricktrack.data.TripWithVehicle
import ch.opum.tricktrack.data.VehicleEntity
import ch.opum.tricktrack.ui.TripType
import ch.opum.tricktrack.ui.TripsViewModel
import ch.opum.tricktrack.ui.LicensePlateBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(viewModel: TripsViewModel) {
    val groupedTrips by viewModel.groupedReviewTrips.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<TripWithVehicle?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.review_discard_trip_title)) },
            text = { Text(stringResource(R.string.review_discard_trip_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog?.let { viewModel.discardTrip(it.trip) }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.review_discard_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    if (groupedTrips.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.review_no_trips), style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    } else {
        val allVehicles by viewModel.allVehicles.collectAsState()
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
                        totalDistance = group.totalDistance
                    )
                }
                items(group.trips, key = { it.trip.id }) { tripWithVehicle ->
                    ReviewTripCard(
                        tripWithVehicle = tripWithVehicle,
                        allVehicles = allVehicles,
                        onApprove = { finalType, selectedVehicle ->
                            viewModel.approveTrip(
                                trip = tripWithVehicle.trip.copy(vehicleId = selectedVehicle?.id),
                                finalType = finalType
                            )
                        },
                        onDiscard = { showDeleteDialog = tripWithVehicle },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewListHeader(date: Long, tripCount: Int, totalDistance: Double) {
    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
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
        Text(
            text = stringResource(R.string.review_trip_count_and_distance, tripCount, totalDistance),
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
    onApprove: (TripType, VehicleEntity?) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trip = tripWithVehicle.trip
    var selectedType by remember { mutableStateOf(if (trip.type == "Business") TripType.BUSINESS else TripType.PERSONAL) }
    var selectedVehicle by remember(tripWithVehicle) { mutableStateOf(tripWithVehicle.vehicle) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    val tripTypes = listOf(stringResource(R.string.trip_type_business), stringResource(R.string.trip_type_personal))
    val icons = listOf(Icons.Default.Work, Icons.Default.Person)
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Type Toggle and Distance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icons[if (selectedType == TripType.BUSINESS) 0 else 1],
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
                                    LicensePlateBadge(selectedVehicle!!)
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
                    }

                    Text(
                        text = "%.2f km".format(trip.distance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tripTypes.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tripTypes.size
                            ),
                            onClick = { selectedType = if (index == 0) TripType.BUSINESS else TripType.PERSONAL },
                            selected = (index == 0) == (selectedType == TripType.BUSINESS),
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
            }

            // Timeline Content
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.Top
            ) {
                // Column 1: Visual Timeline
                TimelineNode()

                // Column 2: Data
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    val timeFormatter = SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale)

                    // Start Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormatter.format(trip.date), // Assuming start time is the trip date
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trip.startLoc,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // End Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormatter.format(Date(trip.endDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trip.endLoc,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }


            // Footer Action Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    onClick = { onApprove(selectedType, selectedVehicle) },
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

@Composable
fun TimelineNode() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight()
            .width(40.dp)
    ) {
        val circleRadius = 8.dp
        val strokeWidth = 2.dp
        val lineColor = MaterialTheme.colorScheme.onSurface
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        // Start Circle
        Canvas(modifier = Modifier.size(circleRadius * 2)) {
            drawCircle(
                color = lineColor,
                radius = size.minDimension / 2,
                style = Stroke(width = strokeWidth.toPx())
            )
        }

        // Dotted Line
        Canvas(
            modifier = Modifier
                .weight(1f)
                .width(strokeWidth)
        ) {
            drawLine(
                color = lineColor,
                start = center.copy(y = 0f),
                end = center.copy(y = size.height),
                strokeWidth = strokeWidth.toPx(),
                pathEffect = pathEffect
            )
        }

        // End Circle
        Canvas(modifier = Modifier.size(circleRadius * 2)) {
            drawCircle(
                color = lineColor,
                radius = size.minDimension / 2,
                style = Stroke(width = strokeWidth.toPx())
            )
        }
    }
}