package ch.opum.tricktrack

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ch.opum.tricktrack.ui.theme.Grey10
import ch.opum.tricktrack.ui.theme.SpecialThemeHelper
import ch.opum.tricktrack.ui.theme.rememberCurrentHour
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
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
import ch.opum.tricktrack.ui.CalculationError
import ch.opum.tricktrack.ui.MergeValidationResult
import ch.opum.tricktrack.ui.TripsViewModel
import ch.opum.tricktrack.ui.ViewModelFactory
import ch.opum.tricktrack.ui.clearFocusOnTap
import ch.opum.tricktrack.ui.components.BulkEditTripsDialog
import ch.opum.tricktrack.ui.components.FullscreenMapSheet
import ch.opum.tricktrack.ui.components.MergeTripsDialog
import ch.opum.tricktrack.ui.components.LocalMapTheme
import ch.opum.tricktrack.ui.components.PencilHelpHint
import ch.opum.tricktrack.ui.components.TripMapView
import ch.opum.tricktrack.ui.components.LocalSnowObstacleRegistry
import ch.opum.tricktrack.ui.components.SnowObstacleRegistry
import ch.opum.tricktrack.ui.components.SnowOverlay
import ch.opum.tricktrack.ui.components.snowObstacle
import ch.opum.tricktrack.ui.navigation.Screen
import ch.opum.tricktrack.ui.onboarding.OnboardingScreen
import ch.opum.tricktrack.ui.place.AddEditPlaceDialog
import ch.opum.tricktrack.ui.place.FavouritesViewModel
import ch.opum.tricktrack.ui.place.PlacesListScreen
import ch.opum.tricktrack.ui.review.ReviewScreen
import ch.opum.tricktrack.ui.settings.SettingsScreen
import ch.opum.tricktrack.ui.theme.TrickTrackTheme
import ch.opum.tricktrack.ui.troubleshooting.TroubleshootingViewModel
import android.view.WindowManager
import ch.opum.tricktrack.util.DistanceFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
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
            val mapTheme by tripsViewModel.mapTheme.collectAsState()
            val accentColorHex by tripsViewModel.accentColorHex.collectAsState()
            val isDynamicColorEnabled by tripsViewModel.isDynamicColorEnabled.collectAsState()
            val specialTheme by tripsViewModel.specialTheme.collectAsState()

            TrickTrackTheme(
                themeMode = themeMode,
                accentColorHex = accentColorHex,
                dynamicColor = isDynamicColorEnabled,
                specialTheme = specialTheme
            ) {
                CompositionLocalProvider(LocalMapTheme provides mapTheme) {
                    val context = LocalContext.current
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
    val hasCompletedOnboarding by tripsViewModel.hasCompletedOnboarding.collectAsState()
    val isDynamicColorEnabled by tripsViewModel.isDynamicColorEnabled.collectAsState()
    val specialTheme by tripsViewModel.specialTheme.collectAsState()
    val accentColorHex by tripsViewModel.accentColorHex.collectAsState()
    val themeMode by tripsViewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }
    val isWinterModeEnabled by tripsViewModel.isWinterModeEnabled.collectAsState()
    val snowObstacleRegistry = remember { SnowObstacleRegistry() }
    val selectedTripIds by tripsViewModel.selectedTripIds.collectAsState()

    BackHandler(enabled = currentRoute == Screen.TripList.route && selectedTripIds.isNotEmpty()) {
        tripsViewModel.clearTripSelection()
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.TripList.route && selectedTripIds.isNotEmpty()) {
            tripsViewModel.clearTripSelection()
        }
    }

    val startDestination = remember(hasCompletedOnboarding) {
        if (hasCompletedOnboarding) Screen.TripList.route else Screen.Onboarding.route
    }

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
    var triggerAddInFavourites by remember { mutableIntStateOf(0) }
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
                val allConfirmed = tripsViewModel.confirmedTrips.value
                val matched = allConfirmed.find { it.trip.id == trip.id }
                if (matched != null) {
                    tripsViewModel.stageDeleteTrip(matched)
                } else {
                    tripsViewModel.deleteTrip(trip)
                }
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

    CompositionLocalProvider(LocalSnowObstacleRegistry provides snowObstacleRegistry) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
            if (currentRoute != Screen.Onboarding.route) {
                val currentHour = rememberCurrentHour()
                val headerGradient = SpecialThemeHelper.getHeaderGradient(specialTheme, currentHour)
                val rawAccentColor = if (accentColorHex == 0L) Color(0xFF6750A4L) else Color(accentColorHex.toInt())
                val isHeaderDark = if (headerGradient != null) {
                    SpecialThemeHelper.isGradientDark(headerGradient)
                } else if (isDynamicColorEnabled) {
                    isDarkTheme
                } else {
                    (0.299f * rawAccentColor.red + 0.587f * rawAccentColor.green + 0.114f * rawAccentColor.blue) < 0.6f
                }

                val topAppBarColors = if (headerGradient != null) {
                    val contentColor = if (isHeaderDark) Color.White else Grey10
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = contentColor,
                        actionIconContentColor = contentColor,
                        navigationIconContentColor = contentColor
                    )
                } else {
                    val contentColor = if (isHeaderDark) Color.White else Grey10
                    val containerColor = if (isDynamicColorEnabled) MaterialTheme.colorScheme.surface else rawAccentColor
                    val titleActionColor = if (isDynamicColorEnabled) MaterialTheme.colorScheme.onSurface else contentColor

                    TopAppBarDefaults.topAppBarColors(
                        containerColor = containerColor,
                        scrolledContainerColor = containerColor,
                        titleContentColor = titleActionColor,
                        actionIconContentColor = titleActionColor,
                        navigationIconContentColor = titleActionColor
                    )
                }

                val isSelectionMode = currentRoute == Screen.TripList.route && selectedTripIds.isNotEmpty()

                TopAppBar(
                    modifier = if (headerGradient != null) {
                        Modifier.background(Brush.horizontalGradient(headerGradient))
                    } else {
                        Modifier
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = { tripsViewModel.clearTripSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_cancel_selection)
                                )
                            }
                        }
                    },
                    title = {
                        val title = if (isSelectionMode) {
                            stringResource(R.string.selected_trips_count, selectedTripIds.size)
                        } else {
                            when (currentRoute) {
                                Screen.TripList.route -> stringResource(R.string.screen_title_trips)
                                Screen.Review.route -> stringResource(R.string.screen_title_review)
                                Screen.PlacesList.route -> stringResource(R.string.screen_title_favourites)
                                Screen.Settings.route -> stringResource(R.string.screen_title_settings)
                                else -> ""
                            }
                        }
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    colors = topAppBarColors,
                    actions = {
                        when (currentRoute) {
                            Screen.TripList.route -> {
                                if (isSelectionMode) {
                                    var showBulkEditDialog by remember { mutableStateOf(false) }
                                    var showMergeDialog by remember { mutableStateOf(false) }
                                    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
                                    val confirmedTrips by tripsViewModel.confirmedTrips.collectAsState()
                                    val distanceUnit by tripsViewModel.distanceUnit.collectAsState()
                                    val isOdometerModeEnabled by tripsViewModel.isOdometerModeEnabled.collectAsState()
                                    val allVehicles by tripsViewModel.allVehicles.collectAsState()

                                    IconButton(onClick = { showBulkEditDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.action_bulk_edit_trips)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val selectedTripsList = confirmedTrips
                                                .map { it.trip }
                                                .filter { it.id in selectedTripIds }
                                            val validation = tripsViewModel.validateMerge(
                                                selectedTrips = selectedTripsList,
                                                allTrips = confirmedTrips.map { it.trip }
                                            )
                                            when (validation) {
                                                is MergeValidationResult.Valid -> {
                                                    showMergeDialog = true
                                                }
                                                is MergeValidationResult.TooFewTrips -> {
                                                    Toast.makeText(context, R.string.merge_trips_consecutive_warning, Toast.LENGTH_SHORT).show()
                                                }
                                                is MergeValidationResult.DifferentVehicles -> {
                                                    Toast.makeText(context, R.string.merge_trips_same_vehicle_warning, Toast.LENGTH_SHORT).show()
                                                }
                                                is MergeValidationResult.NotConsecutive -> {
                                                    Toast.makeText(context, R.string.merge_trips_consecutive_warning, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MergeType,
                                            contentDescription = stringResource(R.string.action_merge_trips)
                                        )
                                    }

                                    IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.action_delete)
                                        )
                                    }

                                    if (showDeleteConfirmationDialog) {
                                        ConfirmationBottomSheet(
                                            title = stringResource(R.string.delete_selected_trips_title),
                                            message = stringResource(R.string.delete_selected_trips_confirmation, selectedTripIds.size),
                                            icon = Icons.Default.Delete,
                                            onConfirm = {
                                                tripsViewModel.deleteSelectedTrips()
                                                showDeleteConfirmationDialog = false
                                            },
                                            onDismiss = { showDeleteConfirmationDialog = false }
                                        )
                                    }

                                    if (showBulkEditDialog) {
                                        val selectedTripsList = remember(selectedTripIds, confirmedTrips) {
                                            confirmedTrips
                                                .map { it.trip }
                                                .filter { it.id in selectedTripIds }
                                        }
                                        BulkEditTripsDialog(
                                            selectedTrips = selectedTripsList,
                                            allVehicles = allVehicles,
                                            onDismiss = { showBulkEditDialog = false },
                                            onConfirm = { targetType, updateType, targetVehicleId, updateVehicle, targetDescription, updateDescription ->
                                                tripsViewModel.bulkUpdateSelectedTrips(
                                                    targetType = targetType,
                                                    updateType = updateType,
                                                    targetVehicleId = targetVehicleId,
                                                    updateVehicle = updateVehicle,
                                                    targetDescription = targetDescription,
                                                    updateDescription = updateDescription
                                                )
                                                showBulkEditDialog = false
                                            }
                                        )
                                    }

                                    if (showMergeDialog) {
                                        val selectedTripsList = remember(selectedTripIds, confirmedTrips) {
                                            confirmedTrips
                                                .map { it.trip }
                                                .filter { it.id in selectedTripIds }
                                                .sortedBy { it.date.time }
                                        }
                                        val vehicle = remember(selectedTripsList, allVehicles) {
                                            val vId = selectedTripsList.firstOrNull()?.vehicleId
                                            allVehicles.find { it.id == vId }
                                        }

                                        MergeTripsDialog(
                                            selectedTrips = selectedTripsList,
                                            vehicle = vehicle,
                                            distanceUnit = distanceUnit,
                                            isOdometerMode = isOdometerModeEnabled,
                                            onDismiss = { showMergeDialog = false },
                                            onConfirm = { finalType, finalDesc ->
                                                tripsViewModel.mergeSelectedTrips(
                                                    selectedTrips = selectedTripsList,
                                                    finalType = finalType,
                                                    finalDescription = finalDesc,
                                                    isOdometerMode = isOdometerModeEnabled,
                                                    onComplete = {
                                                        showMergeDialog = false
                                                    }
                                                )
                                            }
                                        )
                                    }
                                } else {
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
                                IconButton(
                                    onClick = { showFilterDialog = true },
                                    modifier = if (isFilterActive) {
                                        Modifier.background(
                                            color = LocalContentColor.current.copy(alpha = 0.20f),
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ) {
                                    if (isFilterActive) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = if (isHeaderDark) Color.White else Grey10
                                                )
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.FilterList,
                                                contentDescription = stringResource(R.string.action_filter),
                                                tint = LocalContentColor.current
                                            )
                                        }
                                    } else {
                                        Icon(
                                            Icons.Default.FilterList,
                                            contentDescription = stringResource(R.string.action_filter),
                                            tint = LocalContentColor.current
                                        )
                                    }
                                }
                                IconButton(onClick = { showExportDialog = true }) {
                                    Icon(Icons.Default.FileDownload, stringResource(R.string.action_export))
                                }

                                if (showExportDialog) {
                                    ExportFormatDialog(
                                        onDismiss = { showExportDialog = false },
                                        onExportCsvClicked = { exportAll ->
                                            scope.launch {
                                                val uri = tripsViewModel.exportAllTripsToCsv(
                                                    context = context,
                                                    driverName = tripsViewModel.selectedDriver?.name,
                                                    companyName = tripsViewModel.selectedCompany?.name,
                                                    vehicleName = tripsViewModel.selectedVehicle?.licensePlate,
                                                    exportAll = exportAll
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
                                        onExportPdfClicked = { exportAll ->
                                            tripsViewModel.exportTripsToPdf(exportAll = exportAll)
                                            showExportDialog = false
                                        },
                                        viewModel = tripsViewModel
                                    )
                                }

                                if (showFilterDialog) {
                                    val currentFilterState by tripsViewModel.filterState.collectAsState()
                                    val allVehicles by tripsViewModel.allVehicles.collectAsState()
                                    val showSettingsHelp by tripsViewModel.showSettingsHelp.collectAsState()
                                    val showInlineHelpHint by tripsViewModel.showInlineHelpHint.collectAsState()
                                    FilterDialog(
                                        currentFilterState = currentFilterState,
                                        allVehicles = allVehicles,
                                        showSettingsHelp = showSettingsHelp,
                                        showHelpHint = showInlineHelpHint,
                                        onToggleHelp = { tripsViewModel.toggleShowSettingsHelp() },
                                        onApplyFilter = { newFilterState ->
                                            tripsViewModel.updateFilter(newFilterState)
                                            showFilterDialog = false
                                        },
                                        onDismiss = { showFilterDialog = false }
                                    )
                                }

                                if (showDeleteConfirmationDialog) {
                                    val filteredTripsCount by tripsViewModel.tripCount.collectAsState()
                                    ConfirmationBottomSheet(
                                        title = stringResource(R.string.delete_filtered_trips_title),
                                        message = stringResource(R.string.delete_filtered_trips_confirmation, filteredTripsCount),
                                        icon = Icons.Default.Delete,
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
                        }
                        Screen.Settings.route -> {
                                val showSettingsHelp by tripsViewModel.showSettingsHelp.collectAsState()
                                val showInlineHelpHint by tripsViewModel.showInlineHelpHint.collectAsState()
                                if (showInlineHelpHint) {
                                    PencilHelpHint(onClick = { tripsViewModel.toggleShowSettingsHelp() })
                                }
                                IconButton(onClick = { tripsViewModel.toggleShowSettingsHelp() }) {
                                    Icon(
                                        imageVector = if (showSettingsHelp) Icons.AutoMirrored.Filled.Help else Icons.AutoMirrored.Outlined.HelpOutline,
                                        contentDescription = stringResource(R.string.action_toggle_help),
                                        tint = LocalContentColor.current
                                    )
                                }
                                IconButton(onClick = { showAboutDialog = true }) {
                                    Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.action_about))
                                }
                            }
                            Screen.PlacesList.route -> {
                                IconButton(onClick = { triggerAddInFavourites++ }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.favourites_add_item))
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute != Screen.Onboarding.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .clearFocusOnTap()
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        tripsViewModel.setHasCompletedOnboarding(true)
                        navController.navigate(Screen.TripList.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    tripsViewModel = tripsViewModel,
                    favouritesViewModel = favouritesViewModel
                )
            }
            composable(Screen.Review.route) {
                ReviewScreen(viewModel = tripsViewModel)
            }
            composable(Screen.TripList.route) {
                TripScreen(
                    tripsViewModel = tripsViewModel,
                    onTripClick = { trip -> selectedTripToEdit = trip },
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
                    },
                    addTrigger = triggerAddInFavourites,
                    onAddTriggerConsumed = { triggerAddInFavourites = 0 }
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

            if (isWinterModeEnabled) {
                SnowOverlay(
                    isDarkTheme = isDarkTheme
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
    onStartTrip: () -> Unit,
    navController: androidx.navigation.NavHostController
) {
    val groupedTrips by tripsViewModel.groupedTrips.collectAsState()
    val selectedTripIds by tripsViewModel.selectedTripIds.collectAsState()
    val isSelectionMode = selectedTripIds.isNotEmpty()
    val isFilterActive by tripsViewModel.isFilterActive.collectAsState()
    val currentFilterState by tripsViewModel.filterState.collectAsState()
    val distance by tripsViewModel.distance.collectAsState(initial = 0.0)
    val isTracking by tripsViewModel.isTracking.collectAsState(initial = false)
    val expenseTrackingEnabled by tripsViewModel.expenseTrackingEnabled.collectAsState()
    val expenseRatePerKm by tripsViewModel.expenseRatePerKm.collectAsState()
    val expenseCurrency by tripsViewModel.expenseCurrency.collectAsState()
    val totalExpense by tripsViewModel.totalExpense.collectAsState()
    val distanceUnit by tripsViewModel.distanceUnit.collectAsState()
    val totalDistanceFormatted by tripsViewModel.totalDistanceFormatted.collectAsState()
    val tripCount by tripsViewModel.tripCount.collectAsState()
    val pendingDeletedTrips by tripsViewModel.pendingDeletedTrips.collectAsState()
    val isOdometerModeEnabled by tripsViewModel.isOdometerModeEnabled.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val singleDeleteText = stringResource(R.string.trips_trip_deleted_single)
    val multipleDeleteFormat = stringResource(R.string.trips_trips_deleted_multiple)
    val undoText = stringResource(R.string.action_undo)

    // Commit any pending deletions when navigating away from TripScreen
    DisposableEffect(Unit) {
        onDispose {
            tripsViewModel.commitPendingDeletions()
        }
    }

    // Trigger Snackbar whenever pendingDeletedTrips count changes
    LaunchedEffect(pendingDeletedTrips.size) {
        if (pendingDeletedTrips.isNotEmpty()) {
            val count = pendingDeletedTrips.size
            val message = if (count == 1) {
                singleDeleteText
            } else {
                String.format(Locale.getDefault(), multipleDeleteFormat, count)
            }

            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoText,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                tripsViewModel.undoDeleteTrips()
            } else {
                tripsViewModel.commitPendingDeletions()
            }
        }
    }

    val listState = rememberLazyListState()
    var isAllCollapsed by remember { mutableStateOf(value = false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        // Fixed Top Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .snowObstacle("total_distance_summary"),
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
                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = stringResource(R.string.total_distance_overline),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalDistanceFormatted,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val subLineText = if (expenseTrackingEnabled) {
                        stringResource(R.string.trips_count_with_expenses, tripCount, totalExpense, expenseCurrency)
                    } else {
                        stringResource(R.string.trips_count_label, tripCount)
                    }
                    Text(
                        text = subLineText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                val buttonColors = if (isTracking) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
                val density = LocalDensity.current
                val buttonDensity = remember(density) {
                    Density(
                        density = density.density,
                        fontScale = density.fontScale.coerceAtMost(1.25f)
                    )
                }

                CompositionLocalProvider(LocalDensity provides buttonDensity) {
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
                        colors = buttonColors,
                        modifier = Modifier
                            .width(135.dp)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = if (isTracking) 2.dp else 4.dp)
                    ) {
                        val formattedLiveDistance = DistanceFormatter.formatShort(distance / 1000.0, distanceUnit)
                        if (isTracking) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.stop),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.stop),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            lineHeight = 14.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formattedLiveDistance,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            lineHeight = 12.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = stringResource(R.string.start_trip_button),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.start_trip_button),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fixed Active Filter Chips Bar
        if (isFilterActive) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        val locale = LocalLocale.current.platformLocale
                        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
                        val date = formatter.format(Date(currentFilterState.startDate!!))
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
                        val locale = LocalLocale.current.platformLocale
                        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
                        val date = formatter.format(Date(currentFilterState.endDate!!))
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

        // Scrollable List area with overlay button
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                var groupIndexCounter = 0
                groupedTrips.forEach { group ->
                    val currentHeaderIndex = groupIndexCounter
                    stickyHeader(key = group.date) {
                        val dailyTotalCost = if (expenseTrackingEnabled) {
                            group.trips.sumOf { it.trip.getEffectiveDistance(isOdometerModeEnabled) }.toFloat() * expenseRatePerKm
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
                                            delay(16.milliseconds)
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
                            if (expenseTrackingEnabled && dailyTotalCost > 0f) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val formattedDistance = DistanceFormatter.format(group.totalDistance, distanceUnit)
                                    Text(
                                        text = stringResource(R.string.trip_count_and_distance_label, group.trips.size, formattedDistance),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format(LocalLocale.current.platformLocale, "%.2f %s", dailyTotalCost, expenseCurrency),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                val formattedDistance = DistanceFormatter.format(group.totalDistance, distanceUnit)
                                Text(
                                    text = stringResource(R.string.trip_count_and_distance_label, group.trips.size, formattedDistance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                item(key = "group_content_${group.date}") {
                    Column {
                        AnimatedVisibility(
                            visible = !isAllCollapsed,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                group.trips.forEach { tripWithVehicle ->
                                    val isTripSelected = tripWithVehicle.trip.id in selectedTripIds
                                    TripItem(
                                        tripWithVehicle = tripWithVehicle,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isTripSelected,
                                        isOdometerMode = isOdometerModeEnabled,
                                        onClick = {
                                            if (isSelectionMode) {
                                                tripsViewModel.toggleTripSelection(tripWithVehicle.trip.id)
                                            } else {
                                                onTripClick(tripWithVehicle.trip)
                                            }
                                        },
                                        onLongClick = {
                                            if (isSelectionMode) {
                                                tripsViewModel.toggleTripSelection(tripWithVehicle.trip.id)
                                            } else {
                                                tripsViewModel.startTripSelection(tripWithVehicle.trip.id)
                                            }
                                        },
                                        expenseTrackingEnabled = expenseTrackingEnabled,
                                        expenseRatePerKm = expenseRatePerKm,
                                        expenseCurrency = expenseCurrency,
                                        distanceUnit = distanceUnit,
                                        onUpdatePolyline = { polyline ->
                                            tripsViewModel.updateTripPolyline(tripWithVehicle.trip.id, polyline)
                                        },
                                        onResolvedCoords = { sLat, sLon, eLat, eLon, polyline ->
                                            tripsViewModel.updateTripResolvedData(tripWithVehicle.trip.id, sLat, sLon, eLat, eLon, polyline)
                                        },
                                        onRefreshMap = {
                                            tripsViewModel.refreshTripMap(tripWithVehicle.trip.id)
                                        }
                                    )
                                }
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

            // Centered Empty State Overlay
            if (groupedTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.trips_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.trips_empty_help),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Floating "Scroll to Top" button
            val showButton by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 0 }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                AnimatedVisibility(
                    visible = showButton,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.scroll_to_top),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
    var routePolyline by remember { mutableStateOf(trip?.routePolyline) }
    var tripType by remember {
        mutableStateOf(
            trip?.type ?: if (defaultIsBusiness) "Business" else "Personal"
        )
    }
    var description by remember { mutableStateOf(trip?.description ?: "") }
    var isError by remember { mutableStateOf(false) }

    val isOdometerModeEnabled by tripsViewModel.isOdometerModeEnabled.collectAsState()
    val distanceUnit by tripsViewModel.distanceUnit.collectAsState()
    val expenseTrackingEnabled by tripsViewModel.expenseTrackingEnabled.collectAsState()
    val expenseRatePerKm by tripsViewModel.expenseRatePerKm.collectAsState()
    val expenseCurrency by tripsViewModel.expenseCurrency.collectAsState()

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

    val allVehicles by tripsViewModel.allVehicles.collectAsState()
    var selectedVehicle by remember(trip, allVehicles) {
        mutableStateOf(
            value = trip?.vehicleId?.let { id -> allVehicles.find { it.id == id } }
                ?: tripsViewModel.selectedVehicle
                ?: allVehicles.firstOrNull()
        )
    }
    var vehicleExpanded by remember { mutableStateOf(false) }

    val previousTrip = remember(selectedVehicle, selectedStartDate.value.timeInMillis, trip) {
        tripsViewModel.getPreviousConfirmedTrip(
            trip = trip,
            vehicleId = selectedVehicle?.id,
            tripDate = selectedStartDate.value.time
        )
    }
    val minAllowedStartOdoKm = previousTrip?.endOdometer

    val initialStartOdoKm = remember(trip, selectedVehicle, distanceUnit, selectedStartDate.value.timeInMillis) {
        tripsViewModel.getStartOdometerForTrip(
            trip = trip,
            vehicleId = selectedVehicle?.id,
            tripDate = trip?.date ?: selectedStartDate.value.time
        )
    }

    var startOdometerText by remember(trip, distanceUnit, selectedVehicle) {
        mutableStateOf(
            value = (trip?.startOdometer ?: initialStartOdoKm)?.let {
                val converted = DistanceFormatter.convert(it, distanceUnit)
                "%.0f".format(converted)
            } ?: ""
        )
    }

    var odometerText by remember(trip, distanceUnit, selectedVehicle) {
        mutableStateOf(
            value = trip?.endOdometer?.let {
                val converted = DistanceFormatter.convert(it, distanceUnit)
                "%.0f".format(converted) // Odometer usually doesn't show decimals in input
            } ?: run {
                val dist = trip?.gpsDistance ?: trip?.distance ?: 0.0
                if (trip != null && dist > 0 && initialStartOdoKm != null) {
                    val endKm = initialStartOdoKm + dist
                    val converted = DistanceFormatter.convert(endKm, distanceUnit)
                    "%.0f".format(converted)
                } else ""
            }
        )
    }

    val defaultChainStartKm = minAllowedStartOdoKm ?: selectedVehicle?.currentOdometer ?: 0.0
    val defaultChainStartConverted = DistanceFormatter.convert(defaultChainStartKm, distanceUnit)
    val defaultChainStartFormatted = "%.0f".format(defaultChainStartConverted)

    val startOdometerValue = startOdometerText.toDoubleOrNull()
    val endOdometerValue = odometerText.toDoubleOrNull()

    val enteredStartOdoKm = startOdometerValue?.let { DistanceFormatter.toKm(it, distanceUnit) }
    val effectiveStartOdoKm = enteredStartOdoKm ?: if (startOdometerText.isBlank()) defaultChainStartKm else null

    val endOdoKm = endOdometerValue?.let { DistanceFormatter.toKm(it, distanceUnit) }

    val isStartOdoBelowPrevious = minAllowedStartOdoKm != null && effectiveStartOdoKm != null && (effectiveStartOdoKm < minAllowedStartOdoKm - 0.001)
    val isEndOdoBelowStart = effectiveStartOdoKm != null && endOdoKm != null && (endOdoKm < effectiveStartOdoKm - 0.001)

    val isOdoError = isOdometerModeEnabled && (
        selectedVehicle == null ||
        effectiveStartOdoKm == null ||
        endOdoKm == null ||
        isEndOdoBelowStart ||
        isStartOdoBelowPrevious
    )

    // Use the ViewModel's distanceInput for the text field
    var distanceText by remember(tripsViewModel.distanceInput) { mutableStateOf(tripsViewModel.distanceInput) }

    LaunchedEffect(trip, distanceUnit) {
        trip?.let {
            val dist = it.gpsDistance ?: it.distance
            val converted = DistanceFormatter.convert(dist, distanceUnit)
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
    val calculateMissingBothAddressesToast = stringResource(R.string.calculate_missing_both_addresses)
    val calculateMissingStartAddressToast = stringResource(R.string.calculate_missing_start_address)
    val calculateMissingEndAddressToast = stringResource(R.string.calculate_missing_end_address)
    val calculateNoInternetToast = stringResource(R.string.calculate_no_internet)
    val calculateAddressNotFoundToast = stringResource(R.string.calculate_address_not_found)
    val calculateRoutingFailedToast = stringResource(R.string.calculate_routing_failed)

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
                .clearFocusOnTap()
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

            Spacer(modifier = Modifier.height(20.dp))

            val dateFormat = remember { SimpleDateFormat("EEE, d MMM yy", Locale.getDefault()) }

            // Card 1: Date Card
            Box(modifier = Modifier.fillMaxWidth().clearFocusOnTap()) {
                TextField(
                    value = dateFormat.format(selectedStartDate.value.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.date_label)
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker.value = true }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card 2: Departure Card (Start Time + Start Address)
            Surface(
                modifier = Modifier.fillMaxWidth().clearFocusOnTap(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Time Column (~22%)
                    Column(
                        modifier = Modifier
                            .weight(0.22f)
                            .fillMaxHeight()
                            .clickable { showStartTimePicker.value = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.start_time_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = timeFormatter.format(selectedStartDate.value.time),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Start Address Column (~78%)
                    Box(
                        modifier = Modifier
                            .weight(0.78f)
                            .padding(4.dp)
                    ) {
                        ClearableTextField(
                            value = startText,
                            onValueChange = {
                                startText = it
                                favouritesViewModel.searchAddress(it)
                                activeDropdown = "start"
                            },
                            label = { Text(stringResource(R.string.start_address_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    startTextFieldSize = coordinates.size.toSize()
                                },
                            singleLine = false,
                            isFilled = true,
                            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
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
                                        startText = suggestion.fullAddress
                                        startLat = suggestion.latitude
                                        startLon = suggestion.longitude
                                        favouritesViewModel.clearAddressSuggestions()
                                        activeDropdown = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card 3: Arrival Card (End Time + End Address)
            Surface(
                modifier = Modifier.fillMaxWidth().clearFocusOnTap(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // End Time Column (~22%)
                    Column(
                        modifier = Modifier
                            .weight(0.22f)
                            .fillMaxHeight()
                            .clickable { showEndTimePicker.value = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.end_time_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = timeFormatter.format(selectedEndDate.value.time),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // End Address Column (~78%)
                    Box(
                        modifier = Modifier
                            .weight(0.78f)
                            .padding(4.dp)
                    ) {
                        ClearableTextField(
                            value = endText,
                            onValueChange = {
                                endText = it
                                favouritesViewModel.searchAddress(it)
                                activeDropdown = "end"
                            },
                            label = { Text(stringResource(R.string.end_address_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    endTextFieldSize = coordinates.size.toSize()
                                },
                            singleLine = false,
                            isFilled = true,
                            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
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
                                        endText = suggestion.fullAddress
                                        endLat = suggestion.latitude
                                        endLon = suggestion.longitude
                                        favouritesViewModel.clearAddressSuggestions()
                                        activeDropdown = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val canRetryLookup = trip != null && (
                (startLat != null && startLon != null && tripsViewModel.isPendingAddress(startText)) ||
                (endLat != null && endLon != null && tripsViewModel.isPendingAddress(endText))
            )

            if (canRetryLookup) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = {
                        scope.launch {
                            val resolvedStart = if (tripsViewModel.isPendingAddress(startText) && startLat != null && startLon != null) {
                                tripsViewModel.getAddressFromLocation(startLat, startLon)
                            } else startText
                            val resolvedEnd = if (tripsViewModel.isPendingAddress(endText) && endLat != null && endLon != null) {
                                tripsViewModel.getAddressFromLocation(endLat, endLon)
                            } else endText

                            if (resolvedStart.isNotBlank() && !tripsViewModel.isPendingAddress(resolvedStart)) {
                                startText = resolvedStart
                            }
                            if (resolvedEnd.isNotBlank() && !tripsViewModel.isPendingAddress(resolvedEnd)) {
                                endText = resolvedEnd
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.action_retry_address_lookup)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_retry_address_lookup),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (isOdometerModeEnabled) {
                TextField(
                    value = startOdometerText,
                    onValueChange = { newValue ->
                        if ((newValue.length <= 8) && newValue.all { char -> char.isDigit() }) {
                            startOdometerText = newValue
                            isError = false
                        }
                    },
                    label = { Text(stringResource(R.string.start_odometer_label)) },
                    placeholder = { Text(defaultChainStartFormatted) },
                    isError = isError || isStartOdoBelowPrevious,
                    supportingText = {
                        if (isStartOdoBelowPrevious) {
                            val minFormatted = DistanceFormatter.format(minAllowedStartOdoKm, distanceUnit)
                            Text(
                                text = stringResource(R.string.error_start_odometer_less_than_previous, minFormatted),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && startOdometerText.isBlank()) {
                                startOdometerText = defaultChainStartFormatted
                                val currentDistKm = if (trip != null) {
                                    if (trip.startOdometer != null && trip.endOdometer != null) {
                                        trip.endOdometer - trip.startOdometer
                                    } else {
                                        trip.gpsDistance ?: trip.distance
                                    }
                                } else null
                                if (currentDistKm != null && currentDistKm > 0) {
                                    val newEndKm = defaultChainStartKm + currentDistKm
                                    val convertedEnd = DistanceFormatter.convert(newEndKm, distanceUnit)
                                    odometerText = "%.0f".format(convertedEnd)
                                }
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    suffix = { Text(DistanceFormatter.getUnitSuffix(distanceUnit)) },
                    trailingIcon = {
                        if (startOdometerText.isNotEmpty()) {
                            IconButton(onClick = {
                                if (startOdometerText != defaultChainStartFormatted) {
                                    val oldStartKm = startOdometerValue?.let { DistanceFormatter.toKm(it, distanceUnit) }
                                    startOdometerText = defaultChainStartFormatted
                                    val currentDistKm = if (trip != null) {
                                        if (trip.startOdometer != null && trip.endOdometer != null) {
                                            trip.endOdometer - trip.startOdometer
                                        } else {
                                            trip.gpsDistance ?: trip.distance
                                        }
                                    } else if (oldStartKm != null && endOdoKm != null && endOdoKm >= oldStartKm) {
                                        endOdoKm - oldStartKm
                                    } else null
                                    if (currentDistKm != null && currentDistKm > 0) {
                                        val newEndKm = defaultChainStartKm + currentDistKm
                                        val convertedEnd = DistanceFormatter.convert(newEndKm, distanceUnit)
                                        odometerText = "%.0f".format(convertedEnd)
                                    }
                                } else {
                                    startOdometerText = ""
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear_text)
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = odometerText,
                    onValueChange = { newValue ->
                        if ((newValue.length <= 8) && newValue.all { char -> char.isDigit() }) {
                            odometerText = newValue
                            isError = false
                        }
                    },
                    label = { Text(stringResource(R.string.end_odometer_label)) },
                    isError = isError || isEndOdoBelowStart,
                    supportingText = {
                        if (isEndOdoBelowStart) {
                            Text(
                                text = stringResource(R.string.error_end_odometer_less_than_start),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
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
                if (selectedVehicle != null) {
                    val calcDistanceKm = if (effectiveStartOdoKm != null && endOdoKm != null && endOdoKm >= effectiveStartOdoKm) {
                        endOdoKm - effectiveStartOdoKm
                    } else {
                        0.0
                    }
                    val formattedCalc = DistanceFormatter.format(calcDistanceKm, distanceUnit)
                    val tripCost = calcDistanceKm.toFloat() * expenseRatePerKm
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, end = 4.dp)
                    ) {
                        Text(
                            text = "Calculated: $formattedCalc",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOdoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        if (expenseTrackingEnabled && !isOdoError && odometerText.isNotBlank() && startOdometerText.isNotBlank()) {
                            Text(
                                text = String.format(LocalLocale.current.platformLocale, "%.2f %s", tripCost, expenseCurrency),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.weight(0.68f),
                        suffix = { Text(DistanceFormatter.getUnitSuffix(distanceUnit)) },
                        isFilled = true,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                    )
                    if (tripsViewModel.isCalculating) {
                        Box(
                            modifier = Modifier
                                .weight(0.32f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Surface(
                            onClick = {
                                val startBlank = startText.isBlank()
                                val endBlank = endText.isBlank()
                                if (startBlank && endBlank) {
                                    Toast.makeText(
                                        context,
                                        calculateMissingBothAddressesToast,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Surface
                                } else if (startBlank) {
                                    Toast.makeText(
                                        context,
                                        calculateMissingStartAddressToast,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Surface
                                } else if (endBlank) {
                                    Toast.makeText(
                                        context,
                                        calculateMissingEndAddressToast,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Surface
                                }

                                val startBias = if (startLat != null && startLon != null) Pair(startLat!!, startLon!!) else null
                                val endBias = if (endLat != null && endLon != null) Pair(endLat!!, endLon!!) else null

                                tripsViewModel.calculateDistance(
                                    startAddress = startText,
                                    endAddress = endText,
                                    startCoordsBias = startBias,
                                    endCoordsBias = endBias,
                                    onSuccess = { _, sLat, sLon, eLat, eLon, polyline ->
                                        startLat = sLat
                                        startLon = sLon
                                        endLat = eLat
                                        endLon = eLon
                                        routePolyline = polyline
                                        isError = false
                                    },
                                    onError = { error ->
                                        val msg = when (error) {
                                            CalculationError.NO_INTERNET -> calculateNoInternetToast
                                            CalculationError.ADDRESS_NOT_FOUND -> calculateAddressNotFoundToast
                                            CalculationError.ROUTING_FAILED -> calculateRoutingFailedToast
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(0.32f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, topStart = 0.dp, bottomStart = 0.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = stringResource(R.string.calculate_distance_button),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.calculate_distance_button),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }
                if (expenseTrackingEnabled && distanceText.isNotBlank()) {
                    val dist = distanceText.toDoubleOrNull() ?: 0.0
                    val distKm = DistanceFormatter.toKm(dist, distanceUnit)
                    val tripCost = distKm.toFloat() * expenseRatePerKm
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, end = 4.dp)
                    ) {
                        Text(
                            text = String.format(LocalLocale.current.platformLocale, "%.2f %s", tripCost, expenseCurrency),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = vehicleExpanded,
                onExpandedChange = { vehicleExpanded = it }
            ) {
                val context = LocalContext.current
                TextField(
                    value = selectedVehicle?.licensePlate ?: "",
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
                    }
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
            ClearableTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_optional_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                isFilled = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogDeclineButton(onClick = onDismiss)
                Spacer(modifier = Modifier.width(12.dp))
                DialogAcceptButton(
                    enabled = !isOdoError,
                    onClick = {
                        if (selectedEndDate.value.before(selectedStartDate.value)) {
                            Toast.makeText(
                                context,
                                endTimeBeforeStartTimeToast,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@DialogAcceptButton
                        }
                        val updatedDistance = if (isOdometerModeEnabled) {
                            if (effectiveStartOdoKm == null || endOdoKm == null || selectedVehicle == null || isEndOdoBelowStart || isStartOdoBelowPrevious) {
                                null
                            } else {
                                (endOdoKm - effectiveStartOdoKm).coerceAtLeast(0.0)
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
                            gpsDistance = if (isOdometerModeEnabled) (trip.gpsDistance ?: trip.distance) else updatedDistance,
                            date = selectedStartDate.value.time,
                            endDate = selectedEndDate.value.timeInMillis,
                            startLat = startLat,
                            startLon = startLon,
                            endLat = endLat,
                            endLon = endLon,
                            routePolyline = routePolyline,
                            vehicleId = selectedVehicle?.id,
                            startOdometer = if (isOdometerModeEnabled) {
                                effectiveStartOdoKm
                            } else {
                                trip.startOdometer
                            },
                            endOdometer = if (isOdometerModeEnabled) {
                                endOdoKm
                            } else {
                                trip.endOdometer
                            }
                        ) ?: Trip(
                            startLoc = startText,
                            endLoc = endText,
                            distance = updatedDistance,
                            gpsDistance = updatedDistance,
                            type = tripType,
                            description = description,
                            date = selectedStartDate.value.time,
                            endDate = selectedEndDate.value.timeInMillis,
                            startLat = startLat,
                            startLon = startLon,
                            endLat = endLat,
                            endLon = endLon,
                            routePolyline = routePolyline,
                            isConfirmed = true, // Default for manual add/edit
                            vehicleId = selectedVehicle?.id,
                            startOdometer = if (isOdometerModeEnabled) {
                                effectiveStartOdoKm
                            } else {
                                null
                            },
                            endOdometer = if (isOdometerModeEnabled) {
                                endOdoKm
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripItem(
    tripWithVehicle: TripWithVehicle,
    onClick: () -> Unit,
    expenseTrackingEnabled: Boolean,
    expenseRatePerKm: Float,
    expenseCurrency: String,
    distanceUnit: ch.opum.tricktrack.data.DistanceUnit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isOdometerMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onUpdatePolyline: ((String) -> Unit)? = null,
    onResolvedCoords: ((Double, Double, Double, Double, String?) -> Unit)? = null,
    onRefreshMap: (() -> Unit)? = null
) {
    val trip = tripWithVehicle.trip
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp) // Added horizontal padding
            .snowObstacle("trip_${trip.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                    val typeColor = MaterialTheme.colorScheme.primary
                    val typeIcon = if (isBusiness) Icons.Default.Work else Icons.Default.Person

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
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
                                imageVector = Icons.Default.TouchApp,
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

                    val effectiveDist = trip.getEffectiveDistance(isOdometerMode)
                    if (expenseTrackingEnabled) {
                        val tripCost = effectiveDist.toFloat() * expenseRatePerKm
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = DistanceFormatter.format(effectiveDist, distanceUnit),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format(LocalLocale.current.platformLocale, "%.2f %s", tripCost, expenseCurrency),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = DistanceFormatter.format(effectiveDist, distanceUnit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                val hasMapData = !trip.startLoc.isBlank() || !trip.endLoc.isBlank() || (trip.startLat != null && trip.startLon != null)
                var isMapExpanded by remember { mutableStateOf(false) }
                var showFullscreenMap by remember { mutableStateOf(false) }
                val bringIntoViewRequester = remember { BringIntoViewRequester() }

                LaunchedEffect(isMapExpanded) {
                    if (isMapExpanded) {
                        delay(200.milliseconds)
                        bringIntoViewRequester.bringIntoView()
                    }
                }

                // Timeline Content Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Column 1: Visual Timeline
                    TimelineNode()

                    // Column 2: Data
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val timeFormatter = SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale)

                        // Start Point
                        StyledAddress(
                            time = timeFormatter.format(trip.date),
                            address = trip.startLoc
                        )

                        // End Point Row with Inline Map Button (if no note)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                StyledAddress(
                                    time = timeFormatter.format(Date(trip.endDate)),
                                    address = trip.endLoc
                                )
                            }
                            if (hasMapData && trip.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = { isMapExpanded = !isMapExpanded },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isMapExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isMapExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = stringResource(R.string.action_view_map),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = if (isMapExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.action_view_map),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!trip.description.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = stringResource(R.string.description_cd),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = trip.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (hasMapData) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = { isMapExpanded = !isMapExpanded },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMapExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isMapExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = stringResource(R.string.action_view_map),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = if (isMapExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.action_view_map),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isMapExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
                ) {
                    TripMapView(
                        startLat = trip.startLat,
                        startLon = trip.startLon,
                        endLat = trip.endLat,
                        endLon = trip.endLon,
                        startAddress = trip.startLoc,
                        endAddress = trip.endLoc,
                        routePolyline = trip.routePolyline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        border = null,
                        isInteractive = false,
                        onRouteCalculated = onUpdatePolyline,
                        onResolvedCoords = onResolvedCoords,
                        onRefresh = onRefreshMap,
                        onClick = { showFullscreenMap = true }
                    )
                }

                    if (showFullscreenMap) {
                        FullscreenMapSheet(
                            startLat = trip.startLat,
                            startLon = trip.startLon,
                            endLat = trip.endLat,
                            endLon = trip.endLon,
                            startAddress = trip.startLoc,
                            endAddress = trip.endLoc,
                            routePolyline = trip.routePolyline,
                            title = stringResource(R.string.trip_map_title),
                            onRouteCalculated = onUpdatePolyline,
                            onResolvedCoords = onResolvedCoords,
                            onRefresh = onRefreshMap,
                            onDismiss = { showFullscreenMap = false }
                        )
                    }
            }
        }
    }
}
