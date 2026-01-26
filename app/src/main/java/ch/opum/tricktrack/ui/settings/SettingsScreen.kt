package ch.opum.tricktrack.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.opum.tricktrack.R
import ch.opum.tricktrack.TripApplication
import ch.opum.tricktrack.data.DaySchedule
import ch.opum.tricktrack.data.ScheduleSettings
import ch.opum.tricktrack.data.ScheduleTarget
import ch.opum.tricktrack.hasBackgroundLocationPermission
import ch.opum.tricktrack.ui.ClearableTextField
import ch.opum.tricktrack.ui.TripsViewModel
import ch.opum.tricktrack.ui.troubleshooting.TroubleshootingViewModel
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("ShowToast")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TripsViewModel,
    troubleshootingViewModel: TroubleshootingViewModel,
    showAboutDialog: Boolean,
    onDismissAboutDialog: () -> Unit,
    showLogsDialog: Boolean,
    onDismissLogsDialog: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as TripApplication
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application, application.repository, application.userPreferencesRepository)
    )

    val isAutoTrackingEnabled by viewModel.isAutoTrackingEnabled.collectAsState()
    val isBluetoothTriggerEnabled by viewModel.isBluetoothTriggerEnabled.collectAsState()
    val selectedBluetoothDevices by viewModel.selectedBluetoothDevices.collectAsState()
    val defaultIsBusiness by viewModel.defaultIsBusiness.collectAsState()
    val permissionsStatus by viewModel.permissionsStatus.collectAsState()
    val isAllPermissionsGranted by viewModel.isAllPermissionsGranted.collectAsState()
    val expenseTrackingEnabled by viewModel.expenseTrackingEnabled.collectAsState()
    val expenseRatePerKm by viewModel.expenseRatePerKm.collectAsState()
    val expenseCurrency by viewModel.expenseCurrency.collectAsState()
    val isSmartLocationEnabled by viewModel.isSmartLocationEnabled.collectAsState()
    val smartLocationRadius by viewModel.smartLocationRadius.collectAsState()
    val isScheduleEnabled by viewModel.isScheduleEnabled.collectAsState()
    val isAutomaticSwitchEnabled by viewModel.isAutomaticSwitchEnabled.collectAsState()
    val isBluetoothSwitchEnabled by viewModel.isBluetoothSwitchEnabled.collectAsState()
    val isBluetoothDeviceSelectionEnabled by viewModel.isBluetoothDeviceSelectionEnabled.collectAsState()
    val stillnessTimer by viewModel.stillnessTimer.collectAsState()
    val minSpeed by viewModel.minSpeed.collectAsState()
    var pairedDevices by remember { mutableStateOf<Set<BluetoothDevice>>(emptySet()) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setBluetoothTriggerEnabled(true)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.settings_bluetooth_permission_denied_toast),
                Toast.LENGTH_SHORT
            ).show()
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
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text(stringResource(R.string.settings_bluetooth_select_devices_dialog_title)) },
            text = {
                LazyColumn {
                    items(pairedDevices.toList()) { device ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = device.name ?: stringResource(R.string.unknown_device),
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = selectedBluetoothDevices.contains(device.address),
                                onCheckedChange = {
                                    viewModel.toggleBluetoothDevice(device.address)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDeviceDialog = false }) {
                    Text(stringResource(R.string.button_done))
                }
            }
        )
    }

    if (showPermissionDialog) {
        PermissionDetailDialog(
            onDismiss = { showPermissionDialog = false },
            permissions = permissionsStatus,
            context = context
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

        val context = LocalContext.current
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val versionName = packageInfo?.versionName ?: "1.0.0"

        AlertDialog(
            onDismissRequest = onDismissAboutDialog,
            icon = { Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.about_version, versionName), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = osmAnnotatedString,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = copyrightAnnotatedString,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.about_made_with_love), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissAboutDialog) {
                    Text(stringResource(R.string.button_close))
                }
            }
        )
    }

    if (showScheduleDialog) {
        ScheduleSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showScheduleDialog = false }
        )
    }

    if (showExportDialog) {
        ExportSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            stringResource(R.string.settings_diagnostics_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPermissionDialog = true },
            shape = RoundedCornerShape(12.dp)
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
                        tint = Color.Green
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.settings_permissions_action_needed_cd),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.settings_permissions_action_needed_text), color = Color.Red)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            stringResource(R.string.settings_tracking_settings_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_automatic_tracking_title))
                        Text(
                            stringResource(R.string.settings_automatic_tracking_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isAutomaticSwitchEnabled) MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isAutomaticSwitchEnabled) {
                            Text(
                                stringResource(R.string.settings_controlled_by_schedule),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isAutoTrackingEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.onToggleAutoTracking(
                                checked = enabled,
                                hasBackgroundLocationPermission = context.hasBackgroundLocationPermission()
                            )
                        },
                        enabled = isAutomaticSwitchEnabled
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_bluetooth_trigger_title))
                        Text(
                            stringResource(R.string.settings_bluetooth_trigger_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isBluetoothSwitchEnabled) MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isBluetoothSwitchEnabled) {
                            Text(
                                stringResource(R.string.settings_controlled_by_schedule),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isBluetoothTriggerEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    viewModel.setBluetoothTriggerEnabled(true)
                                }
                            } else {
                                viewModel.setBluetoothTriggerEnabled(false)
                            }
                        },
                        enabled = isBluetoothSwitchEnabled
                    )
                }

                if (isBluetoothDeviceSelectionEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeviceDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val text = if (selectedBluetoothDevices.isEmpty()) {
                            stringResource(R.string.settings_bluetooth_select_device)
                        } else {
                            stringResource(R.string.settings_bluetooth_change_device)
                        }
                        Text(text)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_enable_schedule_title))
                        Text(
                            stringResource(R.string.settings_enable_schedule_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isScheduleEnabled,
                        onCheckedChange = {
                            viewModel.setScheduleEnabled(it)
                        }
                    )
                }
                if (isScheduleEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showScheduleDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_schedule_title))
                            Text(
                                stringResource(R.string.settings_schedule_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = stringResource(R.string.settings_schedule_edit_cd)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            stringResource(R.string.settings_trip_defaults_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

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
                    Text(stringResource(R.string.settings_calculate_expenses_title), modifier = Modifier.weight(1f))
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
                        Text(stringResource(R.string.settings_smart_location_snapping_title))
                        Text(
                            stringResource(R.string.settings_smart_location_snapping_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showExportDialog = true },
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_export_fields_title))
                    Text(
                        stringResource(R.string.settings_export_fields_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = stringResource(R.string.settings_export_fields_configure_cd))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            stringResource(R.string.settings_advanced_settings_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            stringResource(R.string.settings_backup_restore_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json"),
                    onResult = { uri ->
                        if (uri != null) {
                            settingsViewModel.exportBackup(uri)
                        }
                    }
                )

                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri ->
                        if (uri != null) {
                            settingsViewModel.importBackup(uri)
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
                            contentDescription = stringResource(R.string.settings_export_button)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_export_button))
                    }

                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(R.string.settings_import_button)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_import_button))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                BackupSettingsSection(viewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun ExportSettingsDialog(
    viewModel: TripsViewModel,
    onDismiss: () -> Unit
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
            "EXPENSES" to R.string.export_column_expenses
        )
    }

    // Temporary state for the checkboxes within the dialog
    var tempSelectedColumns by remember { mutableStateOf(exportColumns) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_export_fields_title)) },
        text = {
            Column {
                allColumns.forEach { (key, stringResId) ->
                    val isEnabled = when (key) {
                        "DATE" -> false // Always disabled
                        "EXPENSES" -> expenseTrackingEnabled
                        else -> true
                    }
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
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.setExportColumns(tempSelectedColumns)
                onDismiss()
            }) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsDialog(
    viewModel: TripsViewModel,
    onDismiss: () -> Unit
) {
    var selectedDayForStartTime by remember { mutableStateOf<DayOfWeek?>(null) }
    var selectedDayForEndTime by remember { mutableStateOf<DayOfWeek?>(null) }
    var showAllDaysStartTimePicker by remember { mutableStateOf(false) }
    var showAllDaysEndTimePicker by remember { mutableStateOf(false) }

    val scheduleSettings by viewModel.scheduleSettings.collectAsState()

    // Temporary state for the schedule, held in a mutable map
    val tempSchedule = remember { mutableStateMapOf<DayOfWeek, DaySchedule>() }
    var allDaysStartTime by remember { mutableStateOf(0 to 0) }
    var allDaysEndTime by remember { mutableStateOf(23 to 59) }
    var selectedTarget by remember { mutableStateOf(scheduleSettings.target) }

    // Initialize the temporary state from the collected scheduleSettings
    LaunchedEffect(scheduleSettings) {
        if (scheduleSettings.dailySchedules.isNotEmpty()) {
            tempSchedule.clear()
            tempSchedule.putAll(scheduleSettings.dailySchedules)
            selectedTarget = scheduleSettings.target
        }
    }

    if (showAllDaysStartTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = allDaysStartTime.first, initialMinute = allDaysStartTime.second)
        AlertDialog(
            onDismissRequest = { showAllDaysStartTimePicker = false },
            title = { Text(stringResource(R.string.schedule_select_start_time, stringResource(R.string.settings_all_days))) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(onClick = {
                    allDaysStartTime = timePickerState.hour to timePickerState.minute
                    tempSchedule.keys.forEach { day ->
                        tempSchedule[day] = tempSchedule[day]!!.copy(startHour = timePickerState.hour, startMinute = timePickerState.minute)
                    }
                    showAllDaysStartTimePicker = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllDaysStartTimePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    if (showAllDaysEndTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = allDaysEndTime.first, initialMinute = allDaysEndTime.second)
        AlertDialog(
            onDismissRequest = { showAllDaysEndTimePicker = false },
            title = { Text(stringResource(R.string.schedule_select_end_time, stringResource(R.string.settings_all_days))) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(onClick = {
                    allDaysEndTime = timePickerState.hour to timePickerState.minute
                    tempSchedule.keys.forEach { day ->
                        tempSchedule[day] = tempSchedule[day]!!.copy(endHour = timePickerState.hour, endMinute = timePickerState.minute)
                    }
                    showAllDaysEndTimePicker = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllDaysEndTimePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    if (selectedDayForStartTime != null) {
        val day = selectedDayForStartTime!!
        val schedule = tempSchedule[day]
        if (schedule != null) {
            val timePickerState = rememberTimePickerState(initialHour = schedule.startHour, initialMinute = schedule.startMinute)
            AlertDialog(
                onDismissRequest = { selectedDayForStartTime = null },
                title = { Text(stringResource(R.string.schedule_select_start_time, day.getDisplayName(TextStyle.FULL, Locale.getDefault()))) },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    Button(onClick = {
                        tempSchedule[day] = schedule.copy(startHour = timePickerState.hour, startMinute = timePickerState.minute)
                        selectedDayForStartTime = null
                    }) {
                        Text(stringResource(R.string.button_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedDayForStartTime = null }) {
                        Text(stringResource(R.string.button_cancel))
                    }
                }
            )
        }
    }

    if (selectedDayForEndTime != null) {
        val day = selectedDayForEndTime!!
        val schedule = tempSchedule[day]
        if (schedule != null) {
            val timePickerState = rememberTimePickerState(initialHour = schedule.endHour, initialMinute = schedule.endMinute)
            AlertDialog(
                onDismissRequest = { selectedDayForEndTime = null },
                title = { Text(stringResource(R.string.schedule_select_end_time, day.getDisplayName(TextStyle.FULL, Locale.getDefault()))) },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    Button(onClick = {
                        tempSchedule[day] = schedule.copy(endHour = timePickerState.hour, endMinute = timePickerState.minute)
                        selectedDayForEndTime = null
                    }) {
                        Text(stringResource(R.string.button_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedDayForEndTime = null }) {
                        Text(stringResource(R.string.button_cancel))
                    }
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_schedule_title)) },
        text = {
            if (tempSchedule.isEmpty()) {
                // Show a loading indicator or an empty state while the schedule is being loaded
                Text(stringResource(R.string.schedule_loading))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val targets = listOf(ScheduleTarget.AUTOMATIC, ScheduleTarget.BLUETOOTH, ScheduleTarget.BOTH)
                    val targetLabels = listOf(
                        stringResource(R.string.schedule_target_automatic),
                        stringResource(R.string.schedule_target_bluetooth),
                        stringResource(R.string.schedule_target_both)
                    )
                    val targetIcons: (ScheduleTarget) -> ImageVector = { target ->
                        when (target) {
                            ScheduleTarget.AUTOMATIC -> Icons.Default.DirectionsCar
                            ScheduleTarget.BLUETOOTH -> Icons.Default.Bluetooth
                            ScheduleTarget.BOTH -> Icons.Default.Layers
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        targets.forEachIndexed { index, target ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = targets.size),
                                onClick = { selectedTarget = target },
                                selected = selectedTarget == target,
                                icon = {
                                    Icon(
                                        imageVector = targetIcons(target),
                                        contentDescription = targetLabels[index],
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                }
                            ) {
                                Text(targetLabels[index])
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(modifier = Modifier.width(80.dp)) // Align with day buttons
                        Text(stringResource(R.string.schedule_from), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(stringResource(R.string.schedule_to), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.size(48.dp)) // Spacer for reset button
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_all_days),
                            modifier = Modifier.width(80.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        OutlinedButton(
                            onClick = { showAllDaysStartTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", allDaysStartTime.first, allDaysStartTime.second))
                        }
                        OutlinedButton(
                            onClick = { showAllDaysEndTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", allDaysEndTime.first, allDaysEndTime.second))
                        }
                        Spacer(modifier = Modifier.size(48.dp)) // Spacer for reset button
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DayOfWeek.values().forEach { day ->
                        val schedule = tempSchedule[day]
                        if (schedule != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { tempSchedule[day] = schedule.copy(isEnabled = !schedule.isEnabled) },
                                    modifier = Modifier.width(80.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (schedule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        contentColor = if (schedule.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                                }

                                OutlinedButton(
                                    onClick = { selectedDayForStartTime = day },
                                    modifier = Modifier.weight(1f),
                                    enabled = schedule.isEnabled
                                ) {
                                    Text(String.format(Locale.getDefault(), "%02d:%02d", schedule.startHour, schedule.startMinute))
                                }
                                OutlinedButton(
                                    onClick = { selectedDayForEndTime = day },
                                    modifier = Modifier.weight(1f),
                                    enabled = schedule.isEnabled
                                ) {
                                    Text(String.format(Locale.getDefault(), "%02d:%02d", schedule.endHour, schedule.endMinute))
                                }
                                IconButton(onClick = {
                                    tempSchedule[day] = schedule.copy(
                                        startHour = 0,
                                        startMinute = 0,
                                        endHour = 23,
                                        endMinute = 59
                                    )
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.schedule_reset_cd))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    allDaysStartTime = 0 to 0
                    allDaysEndTime = 23 to 59
                    DayOfWeek.values().forEach { day ->
                        tempSchedule[day]?.let {
                            tempSchedule[day] = it.copy(
                                isEnabled = true,
                                startHour = 0,
                                startMinute = 0,
                                endHour = 23,
                                endMinute = 59
                            )
                        }
                    }
                }) {
                    Text(stringResource(R.string.schedule_reset_times))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.button_cancel))
                }
                Button(onClick = {
                    val newSettings = ScheduleSettings(
                        target = selectedTarget,
                        dailySchedules = tempSchedule.toMap()
                    )
                    viewModel.updateScheduleSettings(newSettings)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.button_apply))
                }
            }
        }
    )
}