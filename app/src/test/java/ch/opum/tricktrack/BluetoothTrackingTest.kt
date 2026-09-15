package ch.opum.tricktrack

import ch.opum.tricktrack.data.BluetoothRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothTrackingTest {

    @Test
    fun testBluetoothRepositoryRegistration() {
        val testAddress = "00:11:22:33:AA:BB"
        
        // Ensure clean state
        BluetoothRepository.unregisterConnectedDevice(testAddress)
        assertFalse(BluetoothRepository.isDeviceRegisteredConnected(testAddress))

        // Register device
        BluetoothRepository.registerConnectedDevice(testAddress)
        assertTrue(BluetoothRepository.isDeviceRegisteredConnected(testAddress))

        // Case-insensitive check (lowercase address)
        assertTrue(BluetoothRepository.isDeviceRegisteredConnected("00:11:22:33:aa:bb"))

        // Unregister
        BluetoothRepository.unregisterConnectedDevice("00:11:22:33:aa:bb")
        assertFalse(BluetoothRepository.isDeviceRegisteredConnected(testAddress))
    }

    @Test
    fun testLocationServiceConstants() {
        assertEquals("ch.opum.tricktrack.EXTRA_CAR_MODE_EVENT", LocationService.EXTRA_CAR_MODE_EVENT)
        assertEquals("ch.opum.tricktrack.EXTRA_CONNECTED_DEVICE_ADDRESS", LocationService.EXTRA_CONNECTED_DEVICE_ADDRESS)
        assertEquals("ch.opum.tricktrack.EXTRA_DISCONNECTED_DEVICE_ADDRESS", LocationService.EXTRA_DISCONNECTED_DEVICE_ADDRESS)
    }
}
