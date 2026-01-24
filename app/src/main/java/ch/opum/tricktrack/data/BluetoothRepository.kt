package ch.opum.tricktrack.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import ch.opum.tricktrack.logging.AppLogger

class BluetoothRepository(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    fun isDeviceConnected(deviceAddress: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            AppLogger.log("BluetoothRepository", "BLUETOOTH_CONNECT permission not granted.")
            return false
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            AppLogger.log("BluetoothRepository", "Bluetooth adapter is not available or not enabled.")
            return false
        }

        val bondedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val device = bondedDevices?.find { it.address == deviceAddress }

        if (device == null) {
            AppLogger.log("BluetoothRepository", "Device with address $deviceAddress not found in bonded devices.")
            return false
        }

        return try {
            val isConnected = device.javaClass.getMethod("isConnected").invoke(device) as Boolean
            AppLogger.log("BluetoothRepository", "Device ${device.name} ($deviceAddress) isConnected: $isConnected")
            isConnected
        } catch (e: Exception) {
            AppLogger.log("BluetoothRepository", "Error checking Bluetooth connection status: ${e.message}")
            false
        }
    }
}