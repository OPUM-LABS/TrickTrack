package ch.opum.tricktrack.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import ch.opum.tricktrack.logging.AppLogger

import android.bluetooth.BluetoothProfile
import java.util.Collections

class BluetoothRepository(private val context: Context) {

    companion object {
        private val activeConnectedDevices = Collections.synchronizedSet(mutableSetOf<String>())

        fun registerConnectedDevice(address: String) {
            activeConnectedDevices.add(address.uppercase())
        }

        fun unregisterConnectedDevice(address: String) {
            activeConnectedDevices.remove(address.uppercase())
        }

        fun isDeviceRegisteredConnected(address: String): Boolean {
            return activeConnectedDevices.contains(address.uppercase())
        }
    }

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
        val device = bondedDevices?.find { it.address.equals(deviceAddress, ignoreCase = true) }

        if (device == null) {
            AppLogger.log("BluetoothRepository", "Device with address $deviceAddress not found in bonded devices.")
            return false
        }

        // 1. Check in-memory ACL connection state from system broadcasts
        if (isDeviceRegisteredConnected(deviceAddress)) {
            AppLogger.log("BluetoothRepository", "Device ${device.name ?: deviceAddress} is registered as connected via ACL broadcast.")
            return true
        }

        // 2. Check standard BluetoothProfile connection state
        try {
            val gattState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)
            if (gattState == BluetoothProfile.STATE_CONNECTED) {
                AppLogger.log("BluetoothRepository", "Device ${device.name ?: deviceAddress} GATT state is connected.")
                return true
            }
        } catch (e: Exception) {
            AppLogger.log("BluetoothRepository", "GATT connection check failed: ${e.message}")
        }

        // 3. Fallback to reflection on hidden device.isConnected()
        return try {
            val isConnected = device.javaClass.getMethod("isConnected").invoke(device) as? Boolean ?: false
            AppLogger.log("BluetoothRepository", "Device ${device.name} ($deviceAddress) isConnected (reflection): $isConnected")
            isConnected
        } catch (e: Exception) {
            AppLogger.log("BluetoothRepository", "Reflection check failed: ${e.message}")
            false
        }
    }
}