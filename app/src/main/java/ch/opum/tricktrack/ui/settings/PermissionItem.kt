package ch.opum.tricktrack.ui.settings

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    object BackgroundLocation : PermissionRequirement(
        "background_location",
        R.string.permission_background_location,
        R.string.permission_background_location_desc,
        Icons.Default.MyLocation,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        else null,
    )

    object Bluetooth : PermissionRequirement(
        "bluetooth",
        R.string.permission_bluetooth,
        R.string.permission_bluetooth_desc,
        Icons.Default.Bluetooth,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_CONNECT
        else null,
    )

    object Notifications : PermissionRequirement(
        "notifications",
        R.string.permission_notifications,
        R.string.permission_notifications_desc,
        Icons.Default.Notifications,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.POST_NOTIFICATIONS
        else null,
    )

    object BatteryOptimization : PermissionRequirement(
        "battery_optimization",
        R.string.permission_battery_optimization_title,
        R.string.permission_battery_optimization_desc,
        Icons.Default.BatteryAlert,
        null,
    )
}

data class PermissionStatus(
    val requirement: PermissionRequirement,
    val isGranted: Boolean,
)

sealed class PermissionHealthState {
    data object AllGranted : PermissionHealthState()
    data object Missing : PermissionHealthState()
}
