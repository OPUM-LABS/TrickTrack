package ch.opum.tricktrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.opum.tricktrack.data.CarBrandHelper
import ch.opum.tricktrack.data.Trip
import ch.opum.tricktrack.data.TripWithVehicle
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.ui.ClearableTextField
import ch.opum.tricktrack.ui.ConfirmationBottomSheet
import ch.opum.tricktrack.ui.DialogAcceptButton
import ch.opum.tricktrack.ui.DialogDeclineButton
import ch.opum.tricktrack.ui.ExportFormatDialog
import ch.opum.tricktrack.ui.FilterDialog
import ch.opum.tricktrack.ui.LicensePlateBadge
import ch.opum.tricktrack.ui.StyledAddress
import ch.opum.tricktrack.ui.ThousandsSeparatorTransformation
import ch.opum.tricktrack.ui.TimePickerDialog
import ch.opum.tricktrack.ui.TimelineNode
import ch.opum.tricktrack.ui.TripTrigger
import ch.opum.tricktrack.ui.TripType
import ch.opum.tricktrack.ui.TripsViewModel
import ch.opum.tricktrack.ui.ViewModelFactory
import ch.opum.tricktrack.ui.navigation.Screen
import ch.opum.tricktrack.ui.place.AddEditPlaceDialog
import ch.opum.tricktrack.ui.place.FavouritesViewModel
import ch.opum.tricktrack.ui.place.PlacesListScreen
import ch.opum.tricktrack.ui.review.ReviewScreen
import ch.opum.tricktrack.ui.settings.SettingsScreen
import ch.opum.tricktrack.ui.theme.TrickTrackTheme
import ch.opum.tricktrack.ui.troubleshooting.TroubleshootingViewModel
import ch.opum.tricktrack.util.DistanceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    private val _currentIntent = MutableStateFlow<Intent?>(null)
    val currentIntent: StateFlow<Intent?> = _currentIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        _currentIntent.value = intent // Set initial intent

        setContent {
            val tripsViewModel: TripsViewModel = viewModel(
                factory = ViewModelFactory(
                    application,
                    (application as TripApplication).repository,
                    (application as TripApplication).userPreferencesRepository,
                ),
            )
            val themeMode by tripsViewModel.themeMode.collectAsState()

            TrickTrackTheme(themeMode = themeMode) {
                val context = LocalContext.current
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Toast.makeText(context, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val application = context.applicationContext as TripApplication
                MainScreen(
                    currentIntent = currentIntent,
                    viewModelFactory = ViewModelFactory(
                        application,
                        application.repository,
                        application.userPreferencesRepository
                    )
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        _currentIntent.value = intent // Update the StateFlow with new intent
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    currentIntent: StateFlow<Intent?>,
    viewModelFactory: ViewModelFactory
) {
    val navController = rememberNavController()
    val tripsViewModel: TripsViewModel = viewModel(factory = viewModelFactory)
    val favouritesViewModel: FavouritesViewModel = viewModel(factory = viewModelFactory)
    val troubleshootingViewModel: TroubleshootingViewModel = viewModel(factory = viewModelFactory)
    val unconfirmedTrips by tripsViewModel.unconfirmedTrips.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val totalDistanceLabel by tripsViewModel.totalDistanceLabel.collectAsState()
    val tripCountLabel by tripsViewModel.tripCountLabel.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val latestIntent by currentIntent.collectAsState()

    LaunchedEffect(Unit) {
        tripsViewModel.pdfFileCreated.collect { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share trips PDF"
                ),
            )
        }
    }

    LaunchedEffect(latestIntent) {
        latestIntent?.let { intent ->
            if (intent.action == LocationService.ACTION_STOP) {
                tripsViewModel.stopTracking()
                navController.navigate(Screen.Review.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                // Consume the action so it doesn't trigger again
                intent.action = null
            } else if (intent.getBooleanExtra("NAVIGATE_TO_REVIEW", false)) {
                navController.navigate(Screen.Review.route) {
                    // Clear back stack to prevent navigating back to the previous screen
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                // Consume the extra so it doesn't trigger again on recomposition
                intent.removeExtra("NAVIGATE_TO_REVIEW")
            }
        }
    }

    // States and Launchers for TripScreen's FAB and related dialogs, moved to MainScreen
    var selectedTripToEdit by remember { mutableStateOf<Trip?>(null) }
    var showBackgroundLocationDialog by remember { mutableStateOf(value = false) }

    // State for PlacesListScreen dialog
    var showAddEditPlaceDialog by remember { mutableStateOf(value = false) }
    var selectedPlaceToEdit by remember { mutableStateOf<SavedPlace?>(null) }

    // State for Settings dialogs
    var showLogsDialog by remember { mutableStateOf(value = false) }
    var showAboutDialog by remember { mutableStateOf(value = false) }


    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Background location permission denied", Toast.LENGTH_SHORT)
                .show()
        }
    }

    val foregroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if ((permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) || (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)) {
            tripsViewModel.startTracking(TripTrigger.MANUAL)
        } else {
            Toast.makeText(
                context,
                "Foreground location permission denied. Cannot start manual trip.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        tripsViewModel.permissionEvent.collect {
            // Only request ACCESS_BACKGROUND_LOCATION on API 29 (Q) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLocationPermissionLauncher.launch("android.permission.ACCESS_BACKGROUND_LOCATION")
            }
        }
    }

    if (showBackgroundLocationDialog) {
        ConfirmationBottomSheet(
            title = stringResource(R.string.background_location_required_title),
            message = stringResource(R.string.background_location_required_text),
            onConfirm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Add this check for API 29 and 30
                    backgroundLocationPermissionLauncher.launch("android.permission.ACCESS_BACKGROUND_LOCATION")
                }
                showBackgroundLocationDialog = false
            },
            onDismiss = {
                showBackgroundLocationDialog = false
            },
        )
    }


    selectedTripToEdit?.let { trip ->
        EditTripDialog(
            trip = trip,
            onDismiss = { selectedTripToEdit = null },
            onSave = { updatedTrip ->
                tripsViewModel.updateTrip(updatedTrip)
                selectedTripToEdit = null
            },
            onDelete = {
                tripsViewModel.deleteTrip(trip)
                selectedTripToEdit = null
            },
            favouritesViewModel = favouritesViewModel,
            tripsViewModel = tripsViewModel
        )
    }

    if (showAddEditPlaceDialog) {
        AddEditPlaceDialog(
            place = selectedPlaceToEdit,
            onDismiss = { showAddEditPlaceDialog = false },
            onSave = { name, address, latitude, longitude ->
                if (selectedPlaceToEdit == null) {
                    favouritesViewModel.addPlace(name, latitude, longitude)
                } else {
                    favouritesViewModel.updatePlace(
                        selectedPlaceToEdit!!,
                        name,
                        address,
                        latitude,
                        longitude
                    )
                }
                showAddEditPlaceDialog = false
            },
            onDelete = {
                selectedPlaceToEdit?.let { favouritesViewModel.deletePlace(it) }
                showAddEditPlaceDialog = false
            },
            favouritesViewModel = favouritesViewModel
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        currentRoute == Screen.TripList.route -> stringResource(R.string.screen_title_trips)
                        currentRoute == Screen.Review.route -> stringResource(R.string.screen_title_review)
                        currentRoute == Screen.PlacesList.route -> stringResource(R.string.screen_title_favourites)
                        currentRoute == Screen.Settings.route -> stringResource(R.string.screen_title_settings)
                        else -> ""
                    }
                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    when (currentRoute) {
                        Screen.TripList.route -> {
                            var showFilterDialog by remember { mutableStateOf(value = false) }
                            var showExportDialog by remember { mutableStateOf(value = false) }
                            val isFilterActive by tripsViewModel.isFilterActive.collectAsState()
                            var showAddManualTripDialog by remember { mutableStateOf(false) }
                            var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

                            IconButton(onClick = { showAddManualTripDialog = true }) {
                                Icon(Icons.Default.Add, stringResource(R.string.action_add_manual_trip))
                            }
                            IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                                Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
                            }
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    stringResource(R.string.action_filter),
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.secondary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(Icons.Default.Share, stringResource(R.string.action_export))
                            }

                            if (showExportDialog) {
                                ExportFormatDialog(
                                    onDismiss = { showExportDialog = false },
                                    onExportCsvClicked = {
                                        scope.launch {
                                            val uri = tripsViewModel.exportAllTripsToCsv(
                                                context = context,
                                                driverName = tripsViewModel.selectedDriver?.name,
                                                companyName = tripsViewModel.selectedCompany?.name,
                                                vehicleName = tripsViewModel.selectedVehicle?.licensePlate
                                            )
                                            uri?.let {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/csv"
                                                    putExtra(Intent.EXTRA_STREAM, it)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(
                                                        shareIntent,
                                                        "Share trips CSV"
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onExportPdfClicked = {
                                        tripsViewModel.exportTripsToPdf()
                                        showExportDialog = false
                                    },
                                    viewModel = tripsViewModel
                                )
                            }

                            if (showFilterDialog) {
                                val currentFilterState by tripsViewModel.filterState.collectAsState()
                                val allVehicles by tripsViewModel.allVehicles.collectAsState()
                                FilterDialog(
                                    currentFilterState = currentFilterState,
                                    allVehicles = allVehicles,
                                    onApplyFilter = { newFilterState ->
                                        tripsViewModel.updateFilter(newFilterState)
                                        showFilterDialog = false
                                    },
                                    onDismiss = { showFilterDialog = false }
                                )
                            }

                            if (showDeleteConfirmationDialog) {
                                ConfirmationBottomSheet(
                                    title = stringResource(R.string.delete_filtered_trips_title),
                                    message = stringResource(R.string.delete_filtered_trips_confirmation),
                                    onConfirm = {
                                        tripsViewModel.deleteFilteredTrips()
                                        showDeleteConfirmationDialog = false
                                    },
                                    onDismiss = { showDeleteConfirmationDialog = false }
                                )
                            }

                            if (showAddManualTripDialog) {
                                EditTripDialog(
                                    trip = null,
                                    onDismiss = { showAddManualTripDialog = false },
                                    onSave = { newTrip ->
                                        tripsViewModel.saveOrUpdateTrip(newTrip)
                                        showAddManualTripDialog = false
                                    },
                                    onDelete = { /* Not used in add mode */ },
                                    favouritesViewModel = favouritesViewModel,
                                    tripsViewModel = tripsViewModel
                                )
                            }
                        }
                        Screen.Settings.route -> {
                            IconButton(onClick = { showAboutDialog = true }) {
                                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.action_about))
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val items =
                    listOf(Screen.Review, Screen.TripList, Screen.PlacesList, Screen.Settings)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            if ((screen is Screen.Review) && unconfirmedTrips.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color(0xFFB00020),
                                            contentColor = Color.White
                                        ) {
                                            Text(unconfirmedTrips.size.toString())
                                        }
                                    }
                                ) {
                                    Icon(screen.icon, contentDescription = stringResource(screen.title))
                                }
                            } else {
                                Icon(screen.icon, contentDescription = stringResource(screen.title))
                            }
                        },
                        label = { Text(stringResource(screen.title)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.TripList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Review.route) {
                ReviewScreen(viewModel = tripsViewModel)
            }
            composable(Screen.TripList.route) {
                TripScreen(
                    tripsViewModel = tripsViewModel,
                    onTripClick = { trip -> selectedTripToEdit = trip },
                    totalDistanceLabel = totalDistanceLabel,
                    tripCountLabel = tripCountLabel,
                    onStartTrip = {
                        foregroundLocationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    navController = navController
                )
            }
            composable(Screen.PlacesList.route) {
                PlacesListScreen(
                    onAddPlace = {
                        selectedPlaceToEdit = null
                        showAddEditPlaceDialog = true
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = tripsViewModel,
                    troubleshootingViewModel = troubleshootingViewModel,
                    showAboutDialog = showAboutDialog,
                    onDismissAboutDialog = { showAboutDialog = false },
                    showLogsDialog = showLogsDialog,
                    onShowLogsDialog = { showLogsDialog = true },
                    onDismissLogsDialog = { showLogsDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripScreen(
    tripsViewModel: TripsViewModel,
    onTripClick: (Trip) -> Unit,
    totalDistanceLabel: String,
    tripCountLabel: String,
    onStartTrip: () -> Unit,
    navController: androidx.navigation.NavHostController
) {
    val groupedTrips by tripsViewModel.groupedTrips.collectAsState()
    val isFilterActive by tripsViewModel.isFilterActive.collectAsState()
    val currentFilterState by tripsViewModel.filterState.collectAsState()
    val distance by tripsViewModel.distance.collectAsState(initial = 0.0)
    val isTracking by tripsViewModel.isTracking.collectAsState(initial = false)
    val expenseTrackingEnabled by tripsViewModel.expenseTrackingEnabled.collectAsState()
    val expenseRatePerKm by tripsViewModel.expenseRatePerKm.collectAsState()
    val expenseCurrency by tripsViewModel.expenseCurrency.collectAsState()
    val totalExpense by tripsViewModel.totalExpense.collectAsState()
    val distanceUnit by tripsViewModel.distanceUnit.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var isAllCollapsed by remember { mutableStateOf(value = false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = totalDistanceLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tripCountLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expenseTrackingEnabled) {
                        Text(
                            text = stringResource(R.string.expenses_label, totalExpense, expenseCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val buttonColors = if (isTracking) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
                FilledTonalButton(
                    onClick = {
                        if (isTracking) {
                            tripsViewModel.stopTracking()
                            navController.navigate(Screen.Review.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            onStartTrip()
                        }
                    },
                    colors = buttonColors
                ) {
                    val formattedLiveDistance = DistanceFormatter.formatShort(distance / 1000.0, distanceUnit)
                    Icon(
                        imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.DirectionsCar,
                        contentDescription = if (isTracking) stringResource(R.string.stop_trip_button, formattedLiveDistance) else stringResource(R.string.start_trip_button)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isTracking) stringResource(R.string.stop_trip_button, formattedLiveDistance)
                        else stringResource(R.string.start_trip_button)
                    )
                }
            }
        }

        // Scrollable List area with overlay button
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isFilterActive) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentFilterState.type != TripType.ALL) {
                                InputChip(
                                    selected = true,
                                    onClick = { tripsViewModel.removeFilter(currentFilterState.copy(type = TripType.ALL)) },
                                    label = { Text(currentFilterState.type.name) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_filter_cd)
                                        )
                                    }
                                )
                            }
                            if (currentFilterState.keyword.isNotEmpty()) {
                                InputChip(
                                    selected = true,
                                    onClick = { tripsViewModel.removeFilter(currentFilterState.copy(keyword = "")) },
                                    label = { Text(currentFilterState.keyword) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_filter_cd)
                                        )
                                    }
                                )
                            }
                            if (currentFilterState.startDate != null) {
                                val date = SimpleDateFormat("dd/MM/yyyy", LocalLocale.current.platformLocale).format(
                                    Date(currentFilterState.startDate!!)
                                )
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        tripsViewModel.removeFilter(
                                            currentFilterState.copy(
                                                startDate = null
                                            )
                                        )
                                    },
                                    label = { Text(stringResource(R.string.from_date_label, date)) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_filter_cd)
                                        )
                                    }
                                )
                            }
                            if (currentFilterState.endDate != null) {
                                val date = SimpleDateFormat("dd/MM/yyyy", LocalLocale.current.platformLocale).format(
                                    Date(currentFilterState.endDate!!)
                                )
                                InputChip(
                                    selected = true,
                                    onClick = { tripsViewModel.removeFilter(currentFilterState.copy(endDate = null)) },
                                    label = { Text(stringResource(R.string.to_date_label, date)) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_filter_cd)
                                        )
                                    }
                                )
                            }
                            if (currentFilterState.vehicleIds.isNotEmpty()) {
                                val allVehicles by tripsViewModel.allVehicles.collectAsState()
                                val displayText = if (currentFilterState.vehicleIds.size == 1) {
                                    allVehicles.find { it.id == currentFilterState.vehicleIds.first() }?.licensePlate ?: ""
                                } else {
                                    stringResource(R.string.vehicles_selected_count, currentFilterState.vehicleIds.size)
                                }
                                InputChip(
                                    selected = true,
                                    onClick = { tripsViewModel.removeFilter(currentFilterState.copy(vehicleIds = emptySet())) },
                                    label = { Text(displayText) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_filter_cd)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                var groupIndexCounter = if (isFilterActive) 1 else 0
                groupedTrips.forEach { group ->
                    val currentHeaderIndex = groupIndexCounter
                    stickyHeader(key = group.date) {
                        val dailyTotalCost = if (expenseTrackingEnabled) {
                            group.trips.sumOf { it.trip.distance }.toFloat() * expenseRatePerKm
                        } else {
                            0.0f
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .clickable {
                                if (isAllCollapsed) {
                                    isAllCollapsed = false
                                    scope.launch {
                                        // Yield to let the list recompose with expanded items
                                        kotlinx.coroutines.yield()
                                        // Continuous lock for the duration of the unfold animation
                                        repeat(40) {
                                            listState.scrollToItem(currentHeaderIndex)
                                            kotlinx.coroutines.delay(16.milliseconds)
                                        }
                                    }
                                } else {
                                    isAllCollapsed = true
                                }
                            }
                                .padding(vertical = 16.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Date
                            val dateFormat = SimpleDateFormat("EEE, d MMM yy", LocalLocale.current.platformLocale)
                            Text(
                                text = dateFormat.format(Date(group.date)),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.weight(1f)) // Pushes content to the right

                            // Right side: Trip count, total distance, and optional total expense
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End // Explicitly align to end
                            ) {
                                val formattedDistance = DistanceFormatter.formatShort(group.totalDistance, distanceUnit)
                                Text(
                                    text = stringResource(R.string.trip_count_and_distance_label, group.trips.size, formattedDistance),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (expenseTrackingEnabled) {
                                    Text(
                                        text = stringResource(R.string.daily_total_cost_label, dailyTotalCost, expenseCurrency),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                item(key = "group_content_${group.date}") {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isAllCollapsed,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                                group.trips.forEach { tripWithVehicle ->
                                    TripItem(
                                        tripWithVehicle = tripWithVehicle,
                                        onClick = { onTripClick(tripWithVehicle.trip) },
                                        expenseTrackingEnabled = expenseTrackingEnabled,
                                        expenseRatePerKm = expenseRatePerKm,
                                        expenseCurrency = expenseCurrency,
                                        distanceUnit = distanceUnit
                                    )
                                }
                            }
                        }
                    }
                    groupIndexCounter += 2
                }

                // Overscroll Spacer: Allows any header to reach the top
                item {
                    Spacer(modifier = Modifier.height(800.dp)) 
                }
            }

            // Floating "Scroll to Top" button
            val showButton by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 0 }
            }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = showButton,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scroll_to_top))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripDialog(
    trip: Trip?,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit,
    onDelete: () -> Unit,
    favouritesViewModel: FavouritesViewModel,
    tripsViewModel: TripsViewModel
) {
    val isEditMode = trip != null
    val defaultIsBusiness by tripsViewModel.defaultIsBusiness.collectAsState()
    var startText by remember { mutableStateOf(trip?.startLoc ?: "") }
    var endText by remember { mutableStateOf(trip?.endLoc ?: "") }
    var startLat by remember { mutableStateOf(trip?.startLat) }
    var startLon by remember { mutableStateOf(trip?.startLon) }
    var endLat by remember { mutableStateOf(trip?.endLat) }
    var endLon by remember { mutableStateOf(trip?.endLon) }
    var tripType by remember {
        mutableStateOf(
            trip?.type ?: if (defaultIsBusiness) "Business" else "Personal"
        )
    }
    var description by remember { mutableStateOf(trip?.description ?: "") }
    var isError by remember { mutableStateOf(false) }

    val isOdometerModeEnabled by tripsViewModel.isOdometerModeEnabled.collectAsState()
    val distanceUnit by tripsViewModel.distanceUnit.collectAsState()

    var odometerText by remember(trip, distanceUnit) {
        mutableStateOf(
            value = trip?.endOdometer?.let {
                val converted = DistanceFormatter.convert(it, distanceUnit)
                "%.0f".format(converted) // Odometer usually doesn't show decimals in input
            } ?: ""
        )
    }

    val allVehicles by tripsViewModel.allVehicles.collectAsState()
    var selectedVehicle by remember(trip, allVehicles) {
        mutableStateOf(
            value = trip?.vehicleId?.let { id -> allVehicles.find { it.id == id } }
                ?: tripsViewModel.selectedVehicle
        )
    }
    var vehicleExpanded by remember { mutableStateOf(false) }

    // Use the ViewModel's distanceInput for the text field
    var distanceText by remember(tripsViewModel.distanceInput) { mutableStateOf(tripsViewModel.distanceInput) }

    LaunchedEffect(trip, distanceUnit) {
        trip?.let {
            val converted = DistanceFormatter.convert(it.distance, distanceUnit)
            distanceText = "%.2f".format(converted)
        }
    }

    // Clean up the ViewModel's distance when the dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            tripsViewModel.distanceInput = ""
        }
    }

    val addressSuggestions by favouritesViewModel.addressSuggestions.collectAsState()
    var startTextFieldSize by remember { mutableStateOf(Size.Zero) }
    var endTextFieldSize by remember { mutableStateOf(Size.Zero) }
    var activeDropdown by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val endTimeBeforeStartTimeToast = stringResource(R.string.end_time_before_start_time_toast)

    // State for Start Date and Time
    val startCalendar = Calendar.getInstance().apply {
        trip?.let {
            time = it.date
        }
    }
    val selectedStartDate = remember { mutableStateOf(startCalendar) }
    val showDatePicker = remember { mutableStateOf(false) }
    val showStartTimePicker = remember { mutableStateOf(false) }

    // State for End Time
    val endCalendar = Calendar.getInstance().apply {
        trip?.let {
            timeInMillis = it.endDate
        } ?: run {
            time = startCalendar.time
            add(Calendar.MINUTE, 15)
        }
    }
    val selectedEndDate = remember { mutableStateOf(endCalendar) }
    val showEndTimePicker = remember { mutableStateOf(value = false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (showDatePicker.value) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = selectedStartDate.value.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val newCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                            selectedStartDate.value.apply {
                                timeInMillis = newCal.timeInMillis
                            }
                            selectedEndDate.value.apply {
                                timeInMillis = newCal.timeInMillis
                            }
                        }
                        showDatePicker.value = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker.value) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedStartDate.value[Calendar.HOUR_OF_DAY],
            initialMinute = selectedStartDate.value[Calendar.MINUTE]
        )
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker.value = false },
            title = stringResource(R.string.start_time_label),
            confirmButton = {
                Button(onClick = {
                    selectedStartDate.value[Calendar.HOUR_OF_DAY] = timePickerState.hour
                    selectedStartDate.value[Calendar.MINUTE] = timePickerState.minute
                    showStartTimePicker.value = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker.value = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
            content = {
                TimePicker(state = timePickerState)
            }
        )
    }

    if (showEndTimePicker.value) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedEndDate.value[Calendar.HOUR_OF_DAY],
            initialMinute = selectedEndDate.value[Calendar.MINUTE]
        )
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker.value = false },
            title = stringResource(R.string.end_time_label),
            confirmButton = {
                Button(onClick = {
                    selectedEndDate.value[Calendar.HOUR_OF_DAY] = timePickerState.hour
                    selectedEndDate.value[Calendar.MINUTE] = timePickerState.minute
                    showEndTimePicker.value = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker.value = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
            content = {
                TimePicker(state = timePickerState)
            }
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
                    text = if (isEditMode) stringResource(R.string.edit_trip_title) else stringResource(R.string.add_manual_trip_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isEditMode) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_trip_cd),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val dateFormat = remember { SimpleDateFormat("EEE, d MMM yy", Locale.getDefault()) }
            Box {
                OutlinedTextField(
                    value = dateFormat.format(selectedStartDate.value.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker.value = true }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = timeFormatter.format(selectedStartDate.value.time),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.start_time_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showStartTimePicker.value = true }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = timeFormatter.format(selectedEndDate.value.time),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.end_time_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showEndTimePicker.value = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                ClearableTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        favouritesViewModel.searchAddress(it) // Pass String directly
                        activeDropdown = "start"
                    },
                    label = { Text(stringResource(R.string.start_address_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            startTextFieldSize = coordinates.size.toSize()
                        }
                )
                DropdownMenu(
                    expanded = addressSuggestions.isNotEmpty() && activeDropdown == "start",
                    onDismissRequest = { favouritesViewModel.clearAddressSuggestions() },
                    properties = PopupProperties(focusable = false),
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    modifier = Modifier
                        .width(with(LocalDensity.current) { startTextFieldSize.width.toDp() })
                        .requiredSizeIn(maxHeight = 200.dp)
                ) {
                    addressSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (suggestion.isFavorite) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = stringResource(R.string.place_favorite_cd),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Column {
                                        Text(
                                            suggestion.title,
                                            fontWeight = if (suggestion.isFavorite) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (suggestion.subtitle.isNotEmpty()) {
                                            Text(
                                                suggestion.subtitle,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                startText = suggestion.fullAddress // Assign String directly
                                startLat = suggestion.latitude
                                startLon = suggestion.longitude
                                favouritesViewModel.clearAddressSuggestions()
                                activeDropdown = null
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                ClearableTextField( // Using ClearableTextField
                    value = endText,
                    onValueChange = {
                        endText = it
                        favouritesViewModel.searchAddress(it) // Pass String directly
                        activeDropdown = "end"
                    },
                    label = { Text(stringResource(R.string.end_address_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            endTextFieldSize = coordinates.size.toSize()
                        }
                )
                DropdownMenu(
                    expanded = addressSuggestions.isNotEmpty() && activeDropdown == "end",
                    onDismissRequest = { favouritesViewModel.clearAddressSuggestions() },
                    properties = PopupProperties(focusable = false),
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    modifier = Modifier
                        .width(with(LocalDensity.current) { endTextFieldSize.width.toDp() })
                        .requiredSizeIn(maxHeight = 200.dp)
                ) {
                    addressSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (suggestion.isFavorite) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = stringResource(R.string.place_favorite_cd),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Column {
                                        Text(
                                            suggestion.title,
                                            fontWeight = if (suggestion.isFavorite) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (suggestion.subtitle.isNotEmpty()) {
                                            Text(
                                                suggestion.subtitle,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                endText = suggestion.fullAddress // Assign String directly
                                endLat = suggestion.latitude
                                endLon = suggestion.longitude
                                favouritesViewModel.clearAddressSuggestions()
                                activeDropdown = null
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isOdometerModeEnabled) {
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { newValue ->
                        if ((newValue.length <= 8) && newValue.all { char -> char.isDigit() }) {
                            odometerText = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.end_odometer_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    suffix = { Text(DistanceFormatter.getUnitSuffix(distanceUnit)) },
                    trailingIcon = {
                        if (odometerText.isNotEmpty()) {
                            IconButton(onClick = { odometerText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear_text)
                                )
                            }
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClearableTextField(
                        value = distanceText,
                        onValueChange = {
                            val sanitizedText =
                                it.replace(',', '.').filter { char -> char == '.' || char.isDigit() }
                            val dotCount = sanitizedText.count { char -> char == '.' }
                            if (dotCount <= 1) {
                                distanceText = sanitizedText
                            }
                            isError = false
                        },
                        label = { Text(stringResource(R.string.distance_km_label)) },
                        placeholder = { Text("0.0") },
                        isError = isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        suffix = { Text(DistanceFormatter.getUnitSuffix(distanceUnit)) }
                    )
                    if (tripsViewModel.isCalculating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        FilledTonalButton(
                            onClick = { tripsViewModel.calculateDistance(startText, endText) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = stringResource(R.string.calculate_distance_button),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.calculate_distance_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val tripTypes = listOf(stringResource(R.string.trip_type_business), stringResource(R.string.trip_type_personal))
            val icons = listOf(Icons.Default.Work, Icons.Default.Person)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tripTypes.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = tripTypes.size
                        ),
                        onClick = { tripType = if (index == 0) "Business" else "Personal" },
                        selected = (if (index == 0) "Business" else "Personal") == tripType,
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
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = vehicleExpanded,
                onExpandedChange = { vehicleExpanded = it }
            ) {
                val context = LocalContext.current
                OutlinedTextField(
                    value = selectedVehicle?.licensePlate ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.favourites_tab_vehicles)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedVehicle != null) {
                                IconButton(onClick = { selectedVehicle = null }) {
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            ClearableTextField( // Using ClearableTextField
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_optional_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogDeclineButton(onClick = onDismiss)
                Spacer(modifier = Modifier.width(12.dp))
                DialogAcceptButton(onClick = {
                    if (selectedEndDate.value.before(selectedStartDate.value)) {
                        Toast.makeText(
                            context,
                            endTimeBeforeStartTimeToast,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@DialogAcceptButton
                    }
                    val updatedDistance = if (isOdometerModeEnabled) {
                        val endOdo = odometerText.toDoubleOrNull() ?: 0.0
                        val endOdoKm = DistanceFormatter.toKm(endOdo, distanceUnit)
                        if (selectedVehicle != null) {
                            // For editing existing trips, we might want a different logic for start odometer
                            // but plan says recalculate distance relative to baseline
                            (endOdoKm - selectedVehicle!!.currentOdometer).coerceAtLeast(0.0)
                        } else {
                            trip?.distance ?: 0.0
                        }
                    } else {
                        val inputDistance = distanceText.toDoubleOrNull()
                        inputDistance?.let { DistanceFormatter.toKm(it, distanceUnit) }
                    }

                    if (updatedDistance == null) {
                        isError = true
                    } else {
                        val tripToSave = trip?.copy(
                            startLoc = startText, // Pass String directly
                            endLoc = endText, // Pass String directly
                            type = tripType,
                            description = description,
                            distance = updatedDistance,
                            date = selectedStartDate.value.time,
                            endDate = selectedEndDate.value.timeInMillis,
                            startLat = startLat,
                            startLon = startLon,
                            endLat = endLat,
                            endLon = endLon,
                            vehicleId = selectedVehicle?.id,
                            endOdometer = if (isOdometerModeEnabled) {
                                odometerText.toDoubleOrNull()?.let { DistanceFormatter.toKm(it, distanceUnit) }
                            } else {
                                trip.endOdometer
                            }
                        ) ?: Trip(
                            startLoc = startText,
                            endLoc = endText,
                            distance = updatedDistance,
                            type = tripType,
                            description = description,
                            date = selectedStartDate.value.time,
                            endDate = selectedEndDate.value.timeInMillis,
                            startLat = startLat,
                            startLon = startLon,
                            endLat = endLat,
                            endLon = endLon,
                            isConfirmed = true, // Default for manual add/edit
                            vehicleId = selectedVehicle?.id,
                            endOdometer = if (isOdometerModeEnabled) {
                                odometerText.toDoubleOrNull()?.let { DistanceFormatter.toKm(it, distanceUnit) }
                            } else {
                                null
                            }
                        )
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onSave(tripToSave)
                            }
                        }
                    }
                })
            }
        }
    }
}


@Composable
fun TripItem(
    tripWithVehicle: TripWithVehicle,
    onClick: () -> Unit,
    expenseTrackingEnabled: Boolean,
    expenseRatePerKm: Float,
    expenseCurrency: String,
    distanceUnit: ch.opum.tricktrack.data.DistanceUnit,
    modifier: Modifier = Modifier
) {
    val trip = tripWithVehicle.trip
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp) // Added horizontal padding
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isBusiness = trip.type == "Business"
                    val typeColor =
                        if (isBusiness) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    val typeIcon = if (isBusiness) Icons.Default.Work else Icons.Default.Person

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = if (isBusiness) stringResource(R.string.trip_type_business) else stringResource(R.string.trip_type_personal),
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        val vehicle = tripWithVehicle.vehicle
                        if (vehicle != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LicensePlateBadge(vehicle)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DistanceFormatter.format(trip.distance, distanceUnit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (expenseTrackingEnabled) {
                            val tripCost = trip.distance.toFloat() * expenseRatePerKm
                            Text(
                                text = stringResource(R.string.trip_cost_label, tripCost, expenseCurrency),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                // Timeline Content Row
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
                if (!trip.description.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = stringResource(R.string.description_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = trip.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
