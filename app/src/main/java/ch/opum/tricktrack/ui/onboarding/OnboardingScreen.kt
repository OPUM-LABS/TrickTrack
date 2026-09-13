package ch.opum.tricktrack.ui.onboarding

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.CarBrandHelper
import ch.opum.tricktrack.ui.ClearableTextField
import ch.opum.tricktrack.ui.TripsViewModel
import ch.opum.tricktrack.ui.clearFocusOnTap
import ch.opum.tricktrack.ui.place.FavouritesViewModel

enum class OnboardingStep {
    WELCOME,                // Step 0: Welcome, Logo, App Description & 3-Step Overview
    PERMISSIONS,            // Step 1: Permissions Checklist + Privacy Guarantee
    ADD_FAVOURITES,         // Step 2: Driver, Company & Vehicle
    TRACKING_PREFERENCE     // Step 3: Tracking Mode Preference
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    tripsViewModel: TripsViewModel,
    favouritesViewModel: FavouritesViewModel
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val context = LocalContext.current

    val onNext: () -> Unit = {
        when (currentStep) {
            OnboardingStep.WELCOME -> currentStep = OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> currentStep = OnboardingStep.ADD_FAVOURITES
            OnboardingStep.ADD_FAVOURITES -> currentStep = OnboardingStep.TRACKING_PREFERENCE
            OnboardingStep.TRACKING_PREFERENCE -> onFinish()
        }
    }

    val onBack: () -> Unit = {
        when (currentStep) {
            OnboardingStep.WELCOME -> { /* First step */ }
            OnboardingStep.PERMISSIONS -> currentStep = OnboardingStep.WELCOME
            OnboardingStep.ADD_FAVOURITES -> currentStep = OnboardingStep.PERMISSIONS
            OnboardingStep.TRACKING_PREFERENCE -> currentStep = OnboardingStep.ADD_FAVOURITES
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar at the top
            LinearProgressIndicator(
                progress = { (currentStep.ordinal.toFloat()) / (OnboardingStep.entries.size - 1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            // Skip Button placed cleanly directly UNDER the progress bar on ALL steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = currentStep,
                label = "OnboardingStepAnimation",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onStartSetup = onNext
                    )
                    OnboardingStep.PERMISSIONS -> PermissionsStep(
                        onNext = onNext,
                        onBack = onBack,
                        context = context
                    )
                    OnboardingStep.ADD_FAVOURITES -> AddFavouritesStep(
                        onNext = onNext,
                        onBack = onBack,
                        favouritesViewModel = favouritesViewModel
                    )
                    OnboardingStep.TRACKING_PREFERENCE -> TrackingPreferenceStep(
                        onFinish = onFinish,
                        onBack = onBack,
                        tripsViewModel = tripsViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(
    onStartSetup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                painter = painterResource(id = R.drawable.tricktrack_logo),
                contentDescription = null,
                modifier = Modifier.size(110.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            val welcomeDesc = stringResource(R.string.onboarding_welcome_desc)
            val descParts = welcomeDesc.split("\n", limit = 2)
            if (descParts.isNotEmpty()) {
                Text(
                    text = descParts[0],
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (descParts.size > 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = descParts[1],
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3-Step Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_welcome_steps_overview),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step_1_summary),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Text(
                        text = stringResource(R.string.onboarding_step_2_summary),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Text(
                        text = stringResource(R.string.onboarding_step_3_summary),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Button(
            onClick = onStartSetup,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(stringResource(R.string.onboarding_start_setup))
        }
    }
}

@Composable
fun PermissionsStep(
    onNext: () -> Unit,
    onBack: () -> Unit,
    context: Context
) {
    val scrollState = rememberScrollState()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    var isLocationGranted by remember { mutableStateOf(false) }
    var isBackgroundGranted by remember { mutableStateOf(false) }
    var isBatteryGranted by remember { mutableStateOf(false) }
    var isNotificationsGranted by remember { mutableStateOf(false) }
    var isBluetoothGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        isLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        isBackgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        isBatteryGranted = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        isNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        isBluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Initial check
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // Re-check when app regains focus / returns from Android System Settings or Permission Dialog
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> checkPermissions() }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> checkPermissions() }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> checkPermissions() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> checkPermissions() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_step_permissions_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_step_permissions_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Guarantee Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.onboarding_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permissions Checklist Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // 1. Precise Location
                    PermissionCheckRow(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.onboarding_precise_location_title),
                        description = stringResource(R.string.permission_precise_location_desc),
                        isGranted = isLocationGranted,
                        isRequired = true,
                        onGrant = {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 2. Background Location
                    PermissionCheckRow(
                        icon = Icons.Default.MyLocation,
                        title = stringResource(R.string.onboarding_background_location_title),
                        description = stringResource(R.string.onboarding_background_location_instruction),
                        isGranted = isBackgroundGranted,
                        isRequired = true,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            } else {
                                checkPermissions()
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 3. Battery Optimization
                    PermissionCheckRow(
                        icon = Icons.Default.BatteryAlert,
                        title = stringResource(R.string.onboarding_battery_title),
                        description = stringResource(R.string.onboarding_battery_desc),
                        isGranted = isBatteryGranted,
                        isRequired = true,
                        onGrant = {
                            try {
                                @SuppressLint("BatteryLife")
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 4. Notifications
                    PermissionCheckRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.onboarding_notifications_title),
                        description = stringResource(R.string.onboarding_notifications_desc),
                        isGranted = isNotificationsGranted,
                        isRequired = true,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                checkPermissions()
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 5. Bluetooth (Optional)
                    PermissionCheckRow(
                        icon = Icons.Default.Bluetooth,
                        title = stringResource(R.string.permission_bluetooth),
                        description = stringResource(R.string.permission_bluetooth_desc),
                        isGranted = isBluetoothGranted,
                        isRequired = false,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                checkPermissions()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val areAllPermissionsGranted = isLocationGranted && isBackgroundGranted && isBatteryGranted && isNotificationsGranted && isBluetoothGranted

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(0.38f)
            ) {
                Text(stringResource(R.string.button_back))
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(0.62f),
                colors = if (areAllPermissionsGranted) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }
}

@Composable
fun PermissionCheckRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean = true,
    onGrant: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "✓ " + stringResource(R.string.onboarding_permission_granted_chip),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isRequired) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_permission_required),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_permission_optional),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (isGranted) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2E7D32), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onGrant,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.onboarding_grant_permission),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .offset(x = 2.dp, y = (-2).dp)
                        .background(
                            if (isRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavouritesStep(
    onNext: () -> Unit,
    onBack: () -> Unit,
    favouritesViewModel: FavouritesViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var driverName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var vehicleBrand by remember { mutableStateOf("Volkswagen") }
    var carModel by remember { mutableStateOf("") }
    var initialOdometer by remember { mutableStateOf("") }
    var brandExpanded by remember { mutableStateOf(false) }

    val carBrands = remember { CarBrandHelper.brands }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_step_favorites_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_step_favorites_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Driver Name
                    ClearableTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        label = { Text(stringResource(R.string.onboarding_driver_label)) },
                        trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        isFilled = true
                    )

                    // Company Name
                    ClearableTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text(stringResource(R.string.onboarding_company_label)) },
                        trailingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        isFilled = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Vehicle License Plate
                    ClearableTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it },
                        label = { Text(stringResource(R.string.favourites_license_plate_label)) },
                        placeholder = { Text("ZH 766767") },
                        trailingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        isFilled = true
                    )

                    // Vehicle Brand Selector
                    ExposedDropdownMenuBox(
                        expanded = brandExpanded,
                        onExpandedChange = { brandExpanded = !brandExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val iconResId = CarBrandHelper.getBrandIconResId(context, vehicleBrand)
                        TextField(
                            value = vehicleBrand,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.favourites_car_brand_label)) },
                            leadingIcon = {
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ExposedDropdownMenuDefaults.textFieldColors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false }
                        ) {
                            carBrands.forEach { brand ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val itemIconResId = CarBrandHelper.getBrandIconResId(context, brand)
                                            if (itemIconResId != 0) {
                                                Icon(
                                                    painter = painterResource(id = itemIconResId),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(brand)
                                        }
                                    },
                                    onClick = {
                                        vehicleBrand = brand
                                        brandExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Car Model Field
                    ClearableTextField(
                        value = carModel,
                        onValueChange = { carModel = it },
                        label = { Text(stringResource(R.string.favourites_car_model_label)) },
                        placeholder = { Text("Golf 8") },
                        trailingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        isFilled = true
                    )

                    // Initial Odometer
                    ClearableTextField(
                        value = initialOdometer,
                        onValueChange = { initialOdometer = it },
                        label = { Text(stringResource(R.string.odometer_label)) },
                        placeholder = { Text("140000") },
                        trailingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isFilled = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(0.38f)
            ) {
                Text(stringResource(R.string.button_back))
            }
            Button(
                onClick = {
                    if (driverName.isNotBlank()) {
                        favouritesViewModel.addDriver(driverName)
                    }
                    if (companyName.isNotBlank()) {
                        favouritesViewModel.addCompany(companyName)
                    }
                    if (licensePlate.isNotBlank()) {
                        val odoDouble = initialOdometer.toDoubleOrNull() ?: 0.0
                        favouritesViewModel.addVehicle(
                            licensePlate = licensePlate,
                            carModel = carModel.ifBlank { null },
                            brand = vehicleBrand,
                            odometer = odoDouble
                        )
                    }
                    onNext()
                },
                modifier = Modifier.weight(0.62f)
            ) {
                Text(stringResource(R.string.onboarding_save_and_continue))
            }
        }
    }
}

@Composable
fun TrackingPreferenceStep(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    tripsViewModel: TripsViewModel
) {
    val context = LocalContext.current

    // Check permissions before allowing toggling auto-tracking or bluetooth
    val isLocationGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val isBluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val isAutoTrackingEnabled by tripsViewModel.isAutoTrackingEnabled.collectAsState()
    val isBluetoothTriggerEnabled by tripsViewModel.isBluetoothTriggerEnabled.collectAsState()
    val defaultIsBusiness by tripsViewModel.defaultIsBusiness.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_step_tracking_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_step_tracking_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto Tracking
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_automatic_tracking_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLocationGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = stringResource(R.string.settings_automatic_tracking_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLocationGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                            if (!isLocationGranted) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_tracking_requires_location),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Switch(
                            checked = isAutoTrackingEnabled && isLocationGranted,
                            enabled = isLocationGranted,
                            onCheckedChange = { tripsViewModel.onToggleAutoTracking(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Bluetooth Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_bluetooth_trigger_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBluetoothGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = stringResource(R.string.settings_bluetooth_trigger_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBluetoothGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                            if (!isBluetoothGranted) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_tracking_requires_bluetooth),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Switch(
                            checked = isBluetoothTriggerEnabled && isBluetoothGranted,
                            enabled = isBluetoothGranted,
                            onCheckedChange = { tripsViewModel.setBluetoothTriggerEnabled(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Default Trip Type
                    Text(stringResource(R.string.settings_default_type_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val tripTypes = listOf(stringResource(R.string.trip_type_business), stringResource(R.string.trip_type_personal))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        tripTypes.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = tripTypes.size),
                                onClick = { tripsViewModel.setDefaultTripType(index == 0) },
                                selected = (index == 0) == defaultIsBusiness
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(0.38f)
            ) {
                Text(stringResource(R.string.button_back))
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(0.62f)
            ) {
                Text(stringResource(R.string.onboarding_finish))
            }
        }
    }
}
