package ch.opum.tricktrack.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

enum class TrackingMode {
    AUTO,
    BLUETOOTH,
    BOTH
}

class PermissionHelper(
    private val activity: ComponentActivity,
    private val showDialog: (title: String, message: String, onPositive: () -> Unit) -> Unit
) {

    private var onSuccessCallback: (() -> Unit)? = null
    private lateinit var currentTrackingMode: TrackingMode
    private var isRequestingLocation: Boolean = false

    private val requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val successful = if (isRequestingLocation) {
                permissions.entries.any { it.value }
            } else {
                permissions.entries.all { it.value }
            }

            if (successful && onSuccessCallback != null) {
                checkAndRequest(currentTrackingMode, onSuccessCallback!!)
            }
        }

    fun checkAndRequest(trackingMode: TrackingMode, onSuccess: () -> Unit) {
        this.currentTrackingMode = trackingMode
        this.onSuccessCallback = onSuccess

        if (needsBluetoothPermission(trackingMode) && !hasBluetoothPermissions()) {
            showDialog(
                "Bluetooth Permission",
                "This feature requires bluetooth permission to connect to devices. Please grant the permission."
            ) {
                isRequestingLocation = false
                requestBluetoothPermissions()
            }
            return
        }

        if (!hasForegroundLocationPermission()) {
            showDialog(
                "Location Permission",
                "This feature requires location permission to track your trips. Please grant the permission."
            ) {
                isRequestingLocation = true
                requestForegroundLocation()
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission()) {
            showDialog(
                "Background Location",
                "To enable automatic tracking, please grant 'Allow all the time' location permission in the settings."
            ) {
                openAppSettings()
            }
            return
        }

        onSuccess()
    }

    private fun hasForegroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun needsBluetoothPermission(trackingMode: TrackingMode): Boolean {
        return trackingMode == TrackingMode.BLUETOOTH || trackingMode == TrackingMode.BOTH
    }

    private fun requestForegroundLocation() {
        requestMultiplePermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }
}
