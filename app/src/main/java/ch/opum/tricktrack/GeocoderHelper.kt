package ch.opum.tricktrack

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import ch.opum.tricktrack.data.place.SavedPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GeocoderHelper(private val context: Context) { // Changed to a class and added context to constructor

    suspend fun getAddressFromLocation(
        lat: Double?,
        lng: Double?
    ): String = withContext(Dispatchers.IO) {
        if (lat == null || lng == null) return@withContext "Unknown Address"

        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { continuation ->
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)
            }
            formatAddress(addresses?.firstOrNull())
        } catch (e: Exception) {
            // Log the exception or handle it as needed
            e.printStackTrace()
            "Address not found"
        }
    }

    suspend fun getAddressFromName(locationName: String): String = withContext(Dispatchers.IO) {
        if (locationName.isBlank()) return@withContext "Unknown Address"

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { continuation ->
                    geocoder.getFromLocationName(locationName, 1) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(locationName, 1)
            }
            formatAddress(addresses?.firstOrNull())
        } catch (e: Exception) {
            e.printStackTrace()
            locationName // Fallback to the original name if geocoding fails
        }
    }

    private fun formatAddress(address: Address?): String {
        if (address == null) {
            return "Unknown Address"
        }
        // Format: "Street Number, ZipCode City"
        val street = address.thoroughfare ?: ""
        val number = address.subThoroughfare ?: ""
        val postalCode = address.postalCode ?: ""
        val city = address.locality ?: ""

        val streetPart =
            if (street.isNotEmpty() && number.isNotEmpty()) "$street $number" else street
        val cityPart =
            if (postalCode.isNotEmpty() && city.isNotEmpty()) "$postalCode $city" else city

        return when {
            streetPart.isNotEmpty() && cityPart.isNotEmpty() -> "$streetPart, $cityPart"
            streetPart.isNotEmpty() -> streetPart
            cityPart.isNotEmpty() -> cityPart
            else -> "Address not found"
        }
    }

    fun getSmartAddress(
        originalAddress: String,
        lat: Double?,
        lng: Double?,
        favorites: List<SavedPlace>,
        isEnabled: Boolean,
        radius: Int
    ): String {
        if (!isEnabled || lat == null || lng == null) {
            return originalAddress
        }

        val currentLocation = Location("").apply {
            latitude = lat
            longitude = lng
        }

        val matchingFavorite = favorites.find { favorite ->
            val favoriteLocation = Location("").apply {
                latitude = favorite.latitude
                longitude = favorite.longitude
            }
            currentLocation.distanceTo(favoriteLocation) < radius // Use the configurable radius
        }

        return if (matchingFavorite != null) {
            "${matchingFavorite.name}, ${matchingFavorite.address}"
        } else {
            originalAddress
        }
    }
}
