package ch.opum.tricktrack

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import ch.opum.tricktrack.data.UserPreferencesRepository
import ch.opum.tricktrack.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        if (device == null) {
            AppLogger.log("BluetoothReceiver", "No device found in intent")
            return
        }

        val userPreferences = UserPreferencesRepository(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            // The service will now decide if this device is relevant.
            // The receiver's only job is to report the event.
            val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.name
                } else {
                    "Unknown Device"
                }
            } else {
                device.name
            }

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    AppLogger.log("BluetoothReceiver", "Device $deviceName connected. Notifying LocationService.")
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        this.action = LocationService.ACTION_BLUETOOTH_CONNECTED
                    }
                    context.startService(serviceIntent)
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    AppLogger.log("BluetoothReceiver", "Device $deviceName disconnected. Notifying LocationService.")
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        this.action = LocationService.ACTION_BLUETOOTH_DISCONNECTED
                    }
                    context.startService(serviceIntent)
                }
            }
        }
    }
}