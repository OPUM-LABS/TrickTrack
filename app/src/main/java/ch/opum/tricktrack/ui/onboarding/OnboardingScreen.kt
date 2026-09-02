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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ch.opum.tricktrack.R

enum class OnboardingStep {
    WELCOME,
    LOCATION,
    BACKGROUND_LOCATION,
    BLUETOOTH,
    NOTIFICATIONS,
    BATTERY,
    FINISH
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val context = LocalContext.current

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LinearProgressIndicator(
                    progress = { (currentStep.ordinal.toFloat() + 1) / OnboardingStep.entries.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                )

                AnimatedContent(
                    targetState = currentStep,
                    label = "OnboardingStepAnimation",
                    modifier = Modifier.weight(1f)
                ) { step ->
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep()
                        OnboardingStep.LOCATION -> LocationStep()
                        OnboardingStep.BACKGROUND_LOCATION -> BackgroundLocationStep()
                        OnboardingStep.BLUETOOTH -> BluetoothStep()
                        OnboardingStep.NOTIFICATIONS -> NotificationsStep()
                        OnboardingStep.BATTERY -> BatteryStep()
                        OnboardingStep.FINISH -> FinishStep()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StepActions(
                    step = currentStep,
                    onNext = {
                        if (currentStep == OnboardingStep.FINISH) {
                            onFinish()
                        } else {
                            currentStep = OnboardingStep.entries[currentStep.ordinal + 1]
                        }
                    },
                    context = context
                )
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    StepContent(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_desc),
        iconRes = R.drawable.tricktrack_logo
    )
}

@Composable
fun LocationStep() {
    val context = LocalContext.current
    val isGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    StepContent(
        title = stringResource(R.string.onboarding_precise_location_title),
        description = stringResource(R.string.permission_precise_location_desc),
        icon = Icons.Default.LocationOn,
        isGranted = isGranted
    )
}

@Composable
fun BackgroundLocationStep() {
    val context = LocalContext.current
    val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    StepContent(
        title = stringResource(R.string.onboarding_background_location_title),
        description = stringResource(R.string.onboarding_background_location_instruction),
        icon = Icons.Default.MyLocation,
        isGranted = isGranted
    )
}

@Composable
fun BluetoothStep() {
    val context = LocalContext.current
    val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    StepContent(
        title = stringResource(R.string.permission_bluetooth),
        description = stringResource(R.string.permission_bluetooth_desc),
        icon = Icons.Default.Bluetooth,
        isGranted = isGranted
    )
}

@Composable
fun NotificationsStep() {
    val context = LocalContext.current
    val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    StepContent(
        title = stringResource(R.string.onboarding_notifications_title),
        description = stringResource(R.string.onboarding_notifications_desc),
        icon = Icons.Default.Notifications,
        isGranted = isGranted
    )
}

@Composable
fun BatteryStep() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    // Track permission state reactively
    var isGranted by remember { 
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) 
    }

    // Re-check when app comes back to foreground
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isGranted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    StepContent(
        title = stringResource(R.string.onboarding_battery_title),
        description = stringResource(R.string.onboarding_battery_desc),
        icon = Icons.Default.BatteryAlert,
        isGranted = isGranted
    )
}

@Composable
fun FinishStep() {
    StepContent(
        title = stringResource(R.string.button_done),
        description = stringResource(R.string.onboarding_finish),
        icon = Icons.Default.CheckCircle
    )
}

@Composable
fun StepContent(
    title: String,
    description: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    isGranted: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_permission_granted),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StepActions(
    step: OnboardingStep,
    onNext: () -> Unit,
    context: Context
) {
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) onNext()
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onNext()
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onNext()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onNext()
    }

    when (step) {
        OnboardingStep.WELCOME -> {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
        OnboardingStep.LOCATION -> {
            Button(
                onClick = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_grant_permission))
            }
            TextButton(onClick = onNext) {
                Text(stringResource(R.string.button_cancel))
            }
        }
        OnboardingStep.BACKGROUND_LOCATION -> {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        onNext()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_grant_permission))
            }
            TextButton(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }) {
                Text(stringResource(R.string.onboarding_open_settings))
            }
        }
        OnboardingStep.BLUETOOTH -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Button(
                    onClick = {
                        bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_grant_permission))
                }
            } else {
                onNext()
            }
            TextButton(onClick = onNext) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
        OnboardingStep.NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(
                    onClick = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_grant_permission))
                }
            } else {
                onNext()
            }
            TextButton(onClick = onNext) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
        OnboardingStep.BATTERY -> {
            Button(
                onClick = {
                    try {
                        @SuppressLint("BatteryLife")
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Fallback to the general battery optimization settings if direct request fails
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_grant_permission))
            }
            TextButton(onClick = onNext) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
        OnboardingStep.FINISH -> {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_finish))
            }
        }
    }
}
