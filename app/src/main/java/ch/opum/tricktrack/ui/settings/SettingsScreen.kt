package ch.opum.tricktrack.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ch.opum.tricktrack.R
import ch.opum.tricktrack.TripApplication
import ch.opum.tricktrack.data.DaySchedule
import ch.opum.tricktrack.data.ScheduleSettings
import ch.opum.tricktrack.permission.TrackingMode
import ch.opum.tricktrack.ui.ClearableTextField
import ch.opum.tricktrack.ui.ConfirmationBottomSheet
import ch.opum.tricktrack.ui.DialogAcceptButton
import ch.opum.tricktrack.ui.DialogDeclineButton
import ch.opum.tricktrack.ui.DialogResetButton
import ch.opum.tricktrack.ui.TimePickerDialog
import ch.opum.tricktrack.ui.TripsViewModel
import ch.opum.tricktrack.ui.components.ExpandableSettingsGroup
import ch.opum.tricktrack.ui.troubleshooting.TroubleshootingViewModel
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Date
import java.util.Locale

@Composable
fun rememberPermissionHelper(): (TrackingMode, onSuccess: () -> Unit) -> Unit {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(value = false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var onPositive by remember { mutableStateOf({}) }
    var confirmButtonText by remember { mutableStateOf("OK") }

    var currentTrackingMode by remember { mutableStateOf(TrackingMode.AUTO) }
    var onSuccessCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isRequestingLocation by remember { mutableStateOf(false) }

    var checkAndRequest: ((TrackingMode, () -> Unit) -> Unit)? by remember { mutableStateOf(null) }

    val requestMultiplePermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val successful = if (isRequestingLocation) {
                permissions.entries.any { it.value }
            } else {
                permissions.entries.all { it.value }
            }

            if (successful) {
                checkAndRequest?.invoke(currentTrackingMode, onSuccessCallback!!)
            }
        }

    val permDialogTitleBluetooth = stringResource(R.string.permission_dialog_title_bluetooth)
    val permDialogMessageBluetooth = stringResource(R.string.permission_dialog_message_bluetooth)
    val buttonOk = stringResource(R.string.button_ok)
    val permDialogTitleLocation = stringResource(R.string.permission_dialog_title_location)
    val permDialogMessageLocation = stringResource(R.string.permission_dialog_message_location)
    val permDialogTitleBackgroundLocation = stringResource(R.string.permission_dialog_title_background_location)
    val permDialogMessageBackgroundLocation = stringResource(R.string.permission_dialog_message_background_location)
    val openSettings = stringResource(R.string.open_settings)

    checkAndRequest = check@{ trackingMode, successCallback ->
        currentTrackingMode = trackingMode
        onSuccessCallback = successCallback

        if (needsBluetoothPermission(trackingMode) && !hasBluetoothPermissions(context)) {
            dialogTitle = permDialogTitleBluetooth
            dialogMessage = permDialogMessageBluetooth
            confirmButtonText = buttonOk
            onPositive = {
                isRequestingLocation = false
                requestBluetoothPermissions(requestMultiplePermissionsLauncher)
            }
            showDialog = true
            return@check
        }

        if (!hasForegroundLocationPermission(context)) {
            dialogTitle = permDialogTitleLocation
            dialogMessage = permDialogMessageLocation
            confirmButtonText = buttonOk
            onPositive = {
                isRequestingLocation = true
                requestForegroundLocation(requestMultiplePermissionsLauncher)
            }
            showDialog = true
            return@check
        }

        if (!hasBackgroundLocationPermission(context)) {
            dialogTitle = permDialogTitleBackgroundLocation
            dialogMessage = permDialogMessageBackgroundLocation
            confirmButtonText = openSettings
            onPositive = { openAppSettings(context) }
            showDialog = true
            return@check
        }

        if (!isBatteryOptimizationIgnored(context)) {
            dialogTitle = "Background Reliability"
            dialogMessage = "To ensure trips record while the screen is off, please update the battery setting:\n\n1. Tap 'Open Settings' below.\n2. Tap 'Battery'.\n3. Select 'Unrestricted'."
            confirmButtonText = "Open Settings"
            onPositive = { openAppSettings(context) }
            showDialog = true
            return@check
        }

        successCallback()
    }

    if (showDialog) {
        ConfirmationBottomSheet(
            title = dialogTitle,
            message = dialogMessage,
            onConfirm = {
                onPositive()
                showDialog = false
            },
            onDismiss = {
                showDialog = false
            },
        )
    }

    return { trackingMode, onSuccess -> checkAndRequest?.invoke(trackingMode, onSuccess) }
}

private fun hasForegroundLocationPermission(context: Context): Boolean {
    return (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED)
}

private fun hasBackgroundLocationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun hasBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun needsBluetoothPermission(trackingMode: TrackingMode): Boolean {
    return trackingMode == TrackingMode.BLUETOOTH || trackingMode == TrackingMode.BOTH
}

private fun requestForegroundLocation(launcher: ActivityResultLauncher<Array<String>>) {
    launcher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
}

private fun requestBluetoothPermissions(launcher: ActivityResultLauncher<Array<String>>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        launcher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}


@SuppressLint("ShowToast")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TripsViewModel,
    troubleshootingViewModel: TroubleshootingViewModel,
    showAboutDialog: Boolean,
    onDismissAboutDialog: () -> Unit,
    showLogsDialog: Boolean,
    onShowLogsDialog: () -> Unit,
    onDismissLogsDialog: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as TripApplication
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application, application.repository, application.userPreferencesRepository)
    )
    val scope = rememberCoroutineScope()

    val isAutoTrackingEnabled by viewModel.isAutoTrackingEnabled.collectAsState()
    val isBluetoothTriggerEnabled by viewModel.isBluetoothTriggerEnabled.collectAsState()
    val selectedBluetoothDevices by viewModel.selectedBluetoothDevices.collectAsState()
    val defaultIsBusiness by viewModel.defaultIsBusiness.collectAsState()
    val isAllPermissionsGranted by viewModel.isAllPermissionsGranted.collectAsState()
    val expenseTrackingEnabled by viewModel.expenseTrackingEnabled.collectAsState()
    val expenseRatePerKm by viewModel.expenseRatePerKm.collectAsState()
    val expenseCurrency by viewModel.expenseCurrency.collectAsState()
    val isSmartLocationEnabled by viewModel.isSmartLocationEnabled.collectAsState()
    val smartLocationRadius by viewModel.smartLocationRadius.collectAsState()
    val isScheduleEnabled by viewModel.isScheduleEnabled.collectAsState()
    val isDistanceMonitoringEnabled by viewModel.isDistanceMonitoringEnabled.collectAsState()
    val distanceMonitoringSummary by viewModel.distanceMonitoringSummary.collectAsState()
    val stillnessTimer by viewModel.stillnessTimer.collectAsState()
    val minSpeed by viewModel.minSpeed.collectAsState()
    var pairedDevices by remember { mutableStateOf<Set<BluetoothDevice>>(emptySet()) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showServerSettingsDialog by remember { mutableStateOf(false) } // New state for server settings

    val permissionHelper = rememberPermissionHelper()
    var isBatteryOptimizationIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isBluetoothTriggerEnabled) {
        if (isBluetoothTriggerEnabled) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val bluetoothManager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
                val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
                pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            }
        }
    }

    if (showDeviceDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDeviceDialog = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_bluetooth_select_devices_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(pairedDevices.toList()) { device ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleBluetoothDevice(device.address)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = device.name ?: stringResource(R.string.unknown_device),
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = selectedBluetoothDevices.contains(device.address),
                                onCheckedChange = null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                DialogAcceptButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showDeviceDialog = false
                        }
                    }
                )
                }
            }
        }
    }

    if (showPermissionSheet) {
        PermissionBottomSheet(
            onDismiss = { showPermissionSheet = false },
            viewModel = viewModel
        )
    }

    if (showLogsDialog) {
        LogsDialog(
            onDismiss = onDismissLogsDialog,
            viewModel = troubleshootingViewModel
        )
    }

    if (showAboutDialog) {
        val osmAttribution = stringResource(id = R.string.about_osm_attribution)
        val osmUrl = "https://www.openstreetmap.org/copyright"
        val osmAnnotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                pushLink(LinkAnnotation.Url(osmUrl))
                append(osmAttribution)
                pop()
            }
        }

        val copyrightText = stringResource(R.string.about_copyright)
        val githubUrl = "https://github.com/OPUM-LABS/TrickTrack"
        val copyrightAnnotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                pushLink(LinkAnnotation.Url(githubUrl))
                append(copyrightText)
                pop()
            }
        }

        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val versionName = packageInfo?.versionName ?: "1.0.0"

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissAboutDialog,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.tricktrack_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.about_version, versionName), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = osmAnnotatedString,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = copyrightAnnotatedString,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    stringResource(R.string.about_made_with_love), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismissAboutDialog()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_close))
                }
            }
        }
    }

    if (showScheduleDialog) {
        ScheduleBottomSheet(
            viewModel = viewModel,
            onDismiss = { showScheduleDialog = false }
        )
    }

    if (showServerSettingsDialog) {
        ServerSettingsDialog(
            onDismiss = { showServerSettingsDialog = false },
            context = context
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val themeMode by viewModel.themeMode.collectAsState()

        ExpandableSettingsGroup(
            title = stringResource(R.string.settings_appearance_title),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    val themeOptions = listOf(
                        stringResource(R.string.settings_theme_system) to "SYSTEM",
                        stringResource(R.string.settings_theme_light) to "LIGHT",
                        stringResource(R.string.settings_theme_dark) to "DARK"
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (label, mode) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = themeOptions.size
                                ),
                                onClick = { viewModel.setThemeMode(mode) },
                                selected = themeMode == mode
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        val permissionHealth by viewModel.permissionHealth.collectAsState()
        if (permissionHealth is PermissionHealthState.Missing) {
            PermissionWarningBanner(onAction = { showPermissionSheet = true })
            Spacer(modifier = Modifier.height(16.dp))
        }

        ExpandableSettingsGroup(
            title = stringResource(R.string.settings_tracking_settings_title),
            description = stringResource(R.string.settings_tracking_settings_description),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // Tracking Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isOdometerModeEnabled by viewModel.isOdometerModeEnabled.collectAsState()
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_odometer_mode_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isOdometerModeEnabled) MaterialTheme.colorScheme.onSurface 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = stringResource(R.string.settings_odometer_mode_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOdometerModeEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Switch(
                            checked = isOdometerModeEnabled,
                            onCheckedChange = { viewModel.setOdometerModeEnabled(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_automatic_tracking_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isAutoTrackingEnabled) MaterialTheme.colorScheme.onSurface 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = stringResource(R.string.settings_automatic_tracking_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAutoTrackingEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Switch(
                            checked = isAutoTrackingEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    permissionHelper(TrackingMode.AUTO) {
                                        viewModel.onToggleAutoTracking(true)
                                    }
                                } else {
                                    viewModel.onToggleAutoTracking(false)
                                }
                            }
                        )
                    }
                    if (isAutoTrackingEnabled && !isBatteryOptimizationIgnored) {
                        Spacer(modifier = Modifier.height(8.dp))
                        BatteryWarningCard {
                            openAppSettings(context)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val bluetoothSummary by viewModel.bluetoothSummary.collectAsState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = isBluetoothTriggerEnabled) { showDeviceDialog = true }
                        ) {
                            Text(
                                text = stringResource(R.string.settings_bluetooth_trigger_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isBluetoothTriggerEnabled) MaterialTheme.colorScheme.onSurface 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = if (isBluetoothTriggerEnabled) bluetoothSummary + stringResource(R.string.settings_tap_to_change)
                                       else stringResource(R.string.settings_bluetooth_trigger_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBluetoothTriggerEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Switch(
                            checked = isBluetoothTriggerEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    permissionHelper(TrackingMode.BLUETOOTH) {
                                        viewModel.setBluetoothTriggerEnabled(true)
                                    }
                                } else {
                                    viewModel.setBluetoothTriggerEnabled(false)
                                }
                            }
                        )
                    }
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var showDistanceDialog by remember { mutableStateOf(false) }

                        if (showDistanceDialog) {
                            val distanceMonitoringRadius by viewModel.distanceMonitoringRadius.collectAsState()
                            var tempRadius by remember(distanceMonitoringRadius) { 
                                mutableStateOf(distanceMonitoringRadius.toString()) 
                            }
                            val dSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            
                            ModalBottomSheet(
                                onDismissRequest = { showDistanceDialog = false },
                                sheetState = dSheetState,
                                dragHandle = { BottomSheetDefaults.DragHandle() }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .padding(bottom = 32.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_distance_monitoring_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.settings_distance_monitoring_description),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    OutlinedTextField(
                                        value = tempRadius,
                                        onValueChange = { newValue: String -> if (newValue.all { char: Char -> char.isDigit() }) tempRadius = newValue },
                                        label = { Text(stringResource(R.string.settings_distance_monitoring_radius_label)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        suffix = { Text("m") }
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DialogDeclineButton(onClick = { showDistanceDialog = false })
                                        Spacer(modifier = Modifier.width(12.dp))
                                        DialogAcceptButton(onClick = {
                                            viewModel.setDistanceMonitoringRadius(tempRadius.toIntOrNull() ?: 50)
                                            scope.launch { dSheetState.hide() }.invokeOnCompletion {
                                                if (!dSheetState.isVisible) showDistanceDialog = false
                                            }
                                        })
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = isDistanceMonitoringEnabled) { showDistanceDialog = true }
                        ) {
                            Text(
                                text = stringResource(R.string.settings_distance_monitoring_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDistanceMonitoringEnabled) MaterialTheme.colorScheme.onSurface 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = if (isDistanceMonitoringEnabled) distanceMonitoringSummary + stringResource(R.string.settings_tap_to_change)
                                       else stringResource(R.string.settings_distance_monitoring_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDistanceMonitoringEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Switch(
                            checked = isDistanceMonitoringEnabled,
                            onCheckedChange = { viewModel.setDistanceMonitoringEnabled(it) }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isScheduleActive by viewModel.isScheduleActive.collectAsState()
                        val scheduleSummary by viewModel.scheduleSummary.collectAsState()

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showScheduleDialog = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.settings_enable_schedule_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isScheduleEnabled) MaterialTheme.colorScheme.onSurface 
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                if (isScheduleEnabled) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isScheduleActive)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = if (isScheduleActive)
                                                stringResource(R.string.schedule_status_active)
                                            else stringResource(R.string.schedule_status_inactive),
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isScheduleActive)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isScheduleEnabled) scheduleSummary + stringResource(R.string.settings_tap_to_edit) 
                                       else stringResource(R.string.settings_tap_to_configure),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isScheduleEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Switch(
                            checked = isScheduleEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setScheduleEnabled(enabled)
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    var localStillnessTimer by remember(stillnessTimer) { mutableStateOf(stillnessTimer.toString()) }
                    var localMinSpeed by remember(minSpeed) { mutableStateOf(minSpeed.toString()) }

                    ClearableTextField(
                        value = localStillnessTimer,
                        onValueChange = { newValue ->
                            localStillnessTimer = newValue
                        },
                        label = { Text(stringResource(R.string.settings_stillness_timer_label)) },
                        placeholder = { Text("60") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    val seconds = localStillnessTimer.toIntOrNull() ?: 60
                                    viewModel.setStillnessTimer(seconds)
                                    localStillnessTimer = seconds.toString()
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ClearableTextField(
                        value = localMinSpeed,
                        onValueChange = { newValue ->
                            localMinSpeed = newValue
                        },
                        label = { Text(stringResource(R.string.settings_min_speed_label)) },
                        placeholder = { Text("15") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    val speed = localMinSpeed.toIntOrNull() ?: 15
                                    viewModel.setMinSpeed(speed)
                                    localMinSpeed = speed.toString()
                                }
                            }
                    )
                }
            }
        }

        ExpandableSettingsGroup(
            title = stringResource(R.string.settings_tracking_defaults_title),
            description = stringResource(R.string.settings_tracking_defaults_description),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // Trip Defaults
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_default_type_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val tripTypes = listOf(stringResource(R.string.trip_type_business), stringResource(R.string.trip_type_personal))
                    val icons = listOf(Icons.Default.Work, Icons.Default.Person)

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        tripTypes.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = tripTypes.size
                                ),
                                onClick = { viewModel.setDefaultTripType(index == 0) },
                                selected = (index == 0) == defaultIsBusiness,
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
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_smart_location_snapping_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSmartLocationEnabled) MaterialTheme.colorScheme.onSurface 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = stringResource(R.string.settings_smart_location_snapping_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSmartLocationEnabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Switch(
                            checked = isSmartLocationEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setSmartLocationEnabled(enabled)
                            }
                        )
                    }
                    if (isSmartLocationEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        var localRadius by remember(smartLocationRadius) { mutableStateOf(smartLocationRadius.toString()) }

                        ClearableTextField(
                            value = localRadius,
                            onValueChange = { newValue ->
                                localRadius = newValue
                            },
                            label = { Text(stringResource(R.string.settings_smart_location_radius_label)) },
                            placeholder = { Text("150") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        val radius = localRadius.toIntOrNull() ?: 150
                                        viewModel.setSmartLocationRadius(radius)
                                        localRadius = radius.toString()
                                    }
                                }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_calculate_expenses_title), 
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (expenseTrackingEnabled) MaterialTheme.colorScheme.onSurface 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        Switch(
                            checked = expenseTrackingEnabled,
                            onCheckedChange = { viewModel.setExpenseTracking(it) }
                        )
                    }

                    if (expenseTrackingEnabled) {
                        var localRate by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", expenseRatePerKm)) }
                        var localCurrency by remember(expenseCurrency) { mutableStateOf(expenseCurrency) }

                        LaunchedEffect(expenseRatePerKm) {
                            localRate = String.format(Locale.getDefault(), "%.2f", expenseRatePerKm)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClearableTextField(
                                value = localRate,
                                onValueChange = { localRate = it },
                                label = { Text(stringResource(R.string.settings_expense_rate_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            val rate = localRate.toFloatOrNull() ?: 0f
                                            localRate = String.format(Locale.getDefault(), "%.2f", rate)
                                            viewModel.setExpenseRate(rate)
                                        }
                                    }
                            )
                            ClearableTextField(
                                value = localCurrency,
                                onValueChange = { localCurrency = it },
                                label = { Text(stringResource(R.string.settings_expense_currency_label)) },
                                modifier = Modifier
                                    .width(100.dp)
                                    .onFocusChanged {
                                        if (!it.isFocused) {
                                            viewModel.setExpenseCurrency(localCurrency)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        ExpandableSettingsGroup(
            title = stringResource(R.string.settings_backup_restore_title),
            description = stringResource(R.string.settings_backup_restore_description),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    val exportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json"),
                        onResult = { uri ->
                        uri?.let {
                            settingsViewModel.createBackup(it) // Changed from exportBackup to createBackup
                        }
                    }
                    )

                    val importLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                        onResult = { uri ->
                            if (uri != null) {
                                settingsViewModel.restoreBackup(uri) // Changed from importBackup to restoreBackup
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                                exportLauncher.launch("tricktrack-backup_$timeStamp.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = stringResource(R.string.settings_backup_button)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_backup_button))
                        }

                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = stringResource(R.string.settings_restore_button)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_restore_button))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    BackupSettingsSection(viewModel = settingsViewModel)
                }
            }
        }

        ExpandableSettingsGroup(
            title = stringResource(R.string.settings_advanced_settings_title),
            description = stringResource(R.string.settings_advanced_settings_description),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // New Card for Server Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showServerSettingsDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.api_settings_title))
                        Text(
                            stringResource(R.string.api_settings_description), // Assuming you'll add this string resource
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = stringResource(R.string.api_settings_title))
                }
            }
        }

        ExpandableSettingsGroup(
            title = stringResource(R.string.settings_diagnostics_title),
            description = stringResource(R.string.settings_diagnostics_description),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPermissionSheet = true },
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_permissions_check_title), modifier = Modifier.weight(1f))
                    if (isAllPermissionsGranted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.settings_permissions_all_granted_cd),
                            tint = Color(0xFF4CAF50)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = stringResource(R.string.settings_permissions_action_needed_cd),
                                tint = Color(0xFFB00020)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_permissions_action_needed_text), color = Color(0xFFB00020))
                        }
                    }
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowLogsDialog() },
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_logs_title), modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = stringResource(R.string.settings_logs_title)
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryWarningCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.warning),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.battery_optimization_warning),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    viewModel: TripsViewModel,
    onDismiss: () -> Unit
) {
    val scheduleSettings by viewModel.scheduleSettings.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Temporary state for the schedule
    val tempSchedule = remember { mutableStateMapOf<DayOfWeek, DaySchedule>() }
    var globalStartTime by remember { mutableStateOf(8 to 0) }
    var globalEndTime by remember { mutableStateOf(17 to 0) }
    var customizeIndividualDays by remember { mutableStateOf(false) }

    LaunchedEffect(scheduleSettings) {
        if (scheduleSettings.dailySchedules.isNotEmpty()) {
            tempSchedule.clear()
            tempSchedule.putAll(scheduleSettings.dailySchedules)
            
            // Infer global time and customization state
            val first = scheduleSettings.dailySchedules.values.first()
            globalStartTime = first.startHour to first.startMinute
            globalEndTime = first.endHour to first.endMinute
            customizeIndividualDays = scheduleSettings.dailySchedules.values.any { 
                it.startHour != first.startHour || it.startMinute != first.startMinute ||
                it.endHour != first.endHour || it.endMinute != first.endMinute
            }
        }
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var dayForTimePicker by remember { mutableStateOf<DayOfWeek?>(null) }

    if (showStartTimePicker || showEndTimePicker) {
        val initialTime = if (dayForTimePicker == null) {
            if (showStartTimePicker) globalStartTime else globalEndTime
        } else {
            val daySched = tempSchedule[dayForTimePicker!!]!!
            if (showStartTimePicker) daySched.startHour to daySched.startMinute else daySched.endHour to daySched.endMinute
        }
        
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.first,
            initialMinute = initialTime.second
        )

        TimePickerDialog(
            onDismissRequest = { 
                showStartTimePicker = false
                showEndTimePicker = false
                dayForTimePicker = null
            },
            title = stringResource(if (showStartTimePicker) R.string.start_time_label else R.string.end_time_label),
            confirmButton = {
                Button(onClick = {
                    if (dayForTimePicker == null) {
                        if (showStartTimePicker) globalStartTime = timePickerState.hour to timePickerState.minute
                        else globalEndTime = timePickerState.hour to timePickerState.minute
                        
                        if (!customizeIndividualDays) {
                            tempSchedule.keys.forEach { day ->
                                tempSchedule[day] = tempSchedule[day]!!.copy(
                                    startHour = globalStartTime.first,
                                    startMinute = globalStartTime.second,
                                    endHour = globalEndTime.first,
                                    endMinute = globalEndTime.second
                                )
                            }
                        }
                    } else {
                        val day = dayForTimePicker!!
                        val current = tempSchedule[day]!!
                        tempSchedule[day] = if (showStartTimePicker) {
                            current.copy(startHour = timePickerState.hour, startMinute = timePickerState.minute)
                        } else {
                            current.copy(endHour = timePickerState.hour, endMinute = timePickerState.minute)
                        }
                    }
                    showStartTimePicker = false
                    showEndTimePicker = false
                    dayForTimePicker = null
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showStartTimePicker = false
                    showEndTimePicker = false
                    dayForTimePicker = null
                }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.settings_schedule_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Side-by-side Time Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeSelectionCard(
                    label = stringResource(R.string.schedule_from),
                    hour = globalStartTime.first,
                    minute = globalStartTime.second,
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
                TimeSelectionCard(
                    label = stringResource(R.string.schedule_to),
                    hour = globalEndTime.first,
                    minute = globalEndTime.second,
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Presets
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                            tempSchedule.keys.forEach { day ->
                                tempSchedule[day] = tempSchedule[day]!!.copy(isEnabled = weekdays.contains(day))
                            }
                        },
                        label = { Text("Weekdays") }
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                            tempSchedule.keys.forEach { day ->
                                tempSchedule[day] = tempSchedule[day]!!.copy(isEnabled = weekend.contains(day))
                            }
                        },
                        label = { Text("Weekend") }
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            tempSchedule.keys.forEach { day ->
                                tempSchedule[day] = tempSchedule[day]!!.copy(isEnabled = true)
                            }
                        },
                        label = { Text("All Days") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DayOfWeek.entries.forEach { day ->
                    val isEnabled = tempSchedule[day]?.isEnabled == true
                    val dayLabel = day.getDisplayName(java.time.format.TextStyle.SHORT, LocalLocale.current.platformLocale)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable {
                                tempSchedule[day] = tempSchedule[day]!!.copy(isEnabled = !isEnabled)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        border = if (isEnabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Advanced Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize individual days",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = customizeIndividualDays,
                    onCheckedChange = { customizeIndividualDays = it }
                )
            }

            if (customizeIndividualDays) {
                Spacer(modifier = Modifier.height(16.dp))
                tempSchedule.toSortedMap().forEach { (day, schedule) ->
                    if (schedule.isEnabled) {
                        IndividualDayRow(
                            day = day,
                            schedule = schedule,
                            onStartTimeClick = { 
                                dayForTimePicker = day
                                showStartTimePicker = true 
                            },
                            onEndTimeClick = { 
                                dayForTimePicker = day
                                showEndTimePicker = true 
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogResetButton(
                    onClick = {
                        globalStartTime = 8 to 0
                        globalEndTime = 17 to 0
                        customizeIndividualDays = false
                        DayOfWeek.entries.forEach { day ->
                            tempSchedule[day] = DaySchedule(true, 8, 0, 17, 0)
                        }
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                DialogDeclineButton(onClick = onDismiss)
                DialogAcceptButton(
                    onClick = {
                        viewModel.updateScheduleSettings(
                            ScheduleSettings(
                                target = scheduleSettings.target,
                                dailySchedules = tempSchedule.toMap()
                            )
                        )
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TimeSelectionCard(
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = String.format(LocalLocale.current.platformLocale, "%02d:%02d", hour, minute),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun IndividualDayRow(
    day: DayOfWeek,
    schedule: DaySchedule,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(dayToResId(day)),
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        ElevatedAssistChip(
            onClick = onStartTimeClick,
            label = { Text(String.format(LocalLocale.current.platformLocale, "%02d:%02d", schedule.startHour, schedule.startMinute)) },
            modifier = Modifier.weight(1f)
        )
        Text("–")
        ElevatedAssistChip(
            onClick = onEndTimeClick,
            label = { Text(String.format(LocalLocale.current.platformLocale, "%02d:%02d", schedule.endHour, schedule.endMinute)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun dayToResId(day: DayOfWeek): Int {
    return when (day) {
        DayOfWeek.MONDAY -> R.string.day_monday
        DayOfWeek.TUESDAY -> R.string.day_tuesday
        DayOfWeek.WEDNESDAY -> R.string.day_wednesday
        DayOfWeek.THURSDAY -> R.string.day_thursday
        DayOfWeek.FRIDAY -> R.string.day_friday
        DayOfWeek.SATURDAY -> R.string.day_saturday
        DayOfWeek.SUNDAY -> R.string.day_sunday
    }
}

@Composable
fun PermissionWarningBanner(onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permission_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.permission_banner_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB00020),
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.permission_banner_action))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionBottomSheet(
    onDismiss: () -> Unit,
    viewModel: TripsViewModel
) {
    val context = LocalContext.current
    val permissionStatus by viewModel.permissionsStatus.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissions(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
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
                text = stringResource(R.string.permission_status_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            permissionStatus.forEach { status ->
                PermissionRow(
                    status = status,
                    onEnable = {
                        when (val req = status.requirement) {
                            PermissionRequirement.BatteryOptimization -> {
                                openAppSettings(context)
                            }
                            else -> {
                                if (req.permission != null) {
                                    permissionLauncher.launch(req.permission)
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_done))
            }
        }
    }
}

@Composable
fun PermissionRow(
    status: PermissionStatus,
    onEnable: () -> Unit
) {
    val requirement = status.requirement
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(requirement.titleRes),
                fontWeight = FontWeight.SemiBold,
                color = if (status.isGranted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                        else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = stringResource(requirement.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (status.isGranted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = requirement.icon,
                contentDescription = null,
                tint = if (status.isGranted) Color(0xFF4CAF50) else Color(0xFFB00020),
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            if (status.isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.permission_granted_cd),
                    tint = Color(0xFF4CAF50).copy(alpha = 0.6f)
                )
            } else {
                TextButton(onClick = onEnable) {
                    Text(stringResource(R.string.permission_enable), color = Color(0xFFB00020))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
