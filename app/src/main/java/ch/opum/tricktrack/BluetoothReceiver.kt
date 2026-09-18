package ch.opum.tricktrack

import android.Manifest
import android.app.UiModeManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import ch.opum.tricktrack.data.BluetoothRepository
import ch.opum.tricktrack.data.UserPreferencesRepository
import ch.opum.tricktrack.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val isCarModeAction = (action == UiModeManager.ACTION_ENTER_CAR_MODE || action == UiModeManager.ACTION_EXIT_CAR_MODE)
        val isAclAction = (action == BluetoothDevice.ACTION_ACL_CONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECTED)

        if (!isCarModeAction && !isAclAction) {
            return
        }

        // If the service is not running, and we receive a disconnect event, no work is needed
        if (!LocationService.isServiceRunning &&
            (action == UiModeManager.ACTION_EXIT_CAR_MODE || action == BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
            AppLogger.log("BluetoothReceiver", "LocationService not running; ignoring disconnect event: $action")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(context)
                val isBtTriggerEnabled = prefs.bluetoothTriggerEnabled.first()

                if (!isBtTriggerEnabled) {
                    AppLogger.log("BluetoothReceiver", "Bluetooth trigger is disabled in settings; ignoring event: $action")
                    return@launch
                }

                val serviceIntent = Intent(context, LocationService::class.java)

                if (isCarModeAction) {
                    val isEntering = (action == UiModeManager.ACTION_ENTER_CAR_MODE)
                    AppLogger.log("BluetoothReceiver", "Android Auto ${if (isEntering) "connected" else "disconnected"}. Notifying LocationService.")
                    serviceIntent.action = if (isEntering) LocationService.ACTION_BLUETOOTH_CONNECTED else LocationService.ACTION_BLUETOOTH_DISCONNECTED
                    serviceIntent.putExtra(LocationService.EXTRA_CAR_MODE_EVENT, isEntering)
                } else {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    if (device == null) {
                        AppLogger.log("BluetoothReceiver", "No device found in intent")
                        return@launch
                    }

                    val address = device.address
                    val isConnected = (action == BluetoothDevice.ACTION_ACL_CONNECTED)

                    if (address != null) {
                        if (isConnected) {
                            BluetoothRepository.registerConnectedDevice(address)
                        } else {
                            BluetoothRepository.unregisterConnectedDevice(address)
                        }
                        serviceIntent.putExtra(
                            if (isConnected) LocationService.EXTRA_CONNECTED_DEVICE_ADDRESS else LocationService.EXTRA_DISCONNECTED_DEVICE_ADDRESS,
                            address
                        )
                    }

                    val selectedDevices = prefs.selectedBluetoothDevices.first()
                    val isSelectedDevice = address != null && selectedDevices.any { it.equals(address, ignoreCase = true) }

                    // Only notify LocationService if this device is one of the user's selected vehicles,
                    // or if the service is already running and this is a disconnect event.
                    if (!isSelectedDevice && !LocationService.isServiceRunning) {
                        AppLogger.log("BluetoothReceiver", "Device ${device.name ?: address} is not a selected vehicle; skipping service start.")
                        return@launch
                    }

                    serviceIntent.action = if (isConnected) LocationService.ACTION_BLUETOOTH_CONNECTED else LocationService.ACTION_BLUETOOTH_DISCONNECTED

                    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            device.name ?: address ?: "Unknown Device"
                        } else {
                            address ?: "Unknown Device"
                        }
                    } else {
                        device.name ?: address ?: "Unknown Device"
                    }

                    AppLogger.log("BluetoothReceiver", "Device $deviceName ${if (isConnected) "connected" else "disconnected"}. Notifying LocationService.")
                }

                try {
                    if (LocationService.isServiceRunning) {
                        context.startService(serviceIntent)
                    } else {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                } catch (e: Exception) {
                    AppLogger.log("BluetoothReceiver", "Failed to start LocationService for action $action: ${e.message}")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}