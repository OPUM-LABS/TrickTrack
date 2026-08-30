package ch.opum.tricktrack.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import ch.opum.tricktrack.R

sealed class PermissionRequirement(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val permission: String? = null,
) {
    object PreciseLocation : PermissionRequirement(
        "precise_location",
        R.string.permission_precise_location,
        R.string.permission_precise_location_desc,
        Icons.Default.LocationOn,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    object BackgroundLocation : PermissionRequirement(
        "background_location",
        R.string.permission_background_location,
        R.string.permission_background_location_desc,
        Icons.Default.LocationOn,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        else null
    )

    object Bluetooth : PermissionRequirement(
        "bluetooth",
        R.string.permission_bluetooth,
        R.string.permission_bluetooth_desc,
        Icons.Default.Bluetooth,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            android.Manifest.permission.BLUETOOTH_CONNECT
        else null
    )

    object Notifications : PermissionRequirement(
        "notifications",
        R.string.permission_notifications,
        R.string.permission_notifications_desc,
        Icons.Default.Notifications,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            android.Manifest.permission.POST_NOTIFICATIONS
        else null
    )

    object BatteryOptimization : PermissionRequirement(
        "battery_optimization",
        R.string.permission_battery_optimization_title,
        R.string.permission_battery_optimization_desc,
        Icons.Default.BatteryAlert,
        null
    )
}

data class PermissionStatus(
    val requirement: PermissionRequirement,
    val isGranted: Boolean
)

sealed class PermissionHealthState {
    object AllGranted : PermissionHealthState()
    data class Missing(@Suppress("unused") val missing: List<PermissionStatus>) : PermissionHealthState()
}
