package ch.opum.tricktrack

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import ch.opum.tricktrack.data.place.SavedPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

class GeocoderHelper(private val context: Context) { // Changed to a class and added context to constructor

    suspend fun getAddressFromLocation(
        lat: Double?,
        lng: Double?
    ): String = withContext(Dispatchers.IO) {
        if (lat == null || lng == null) return@withContext "Unknown Address"

        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
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
                suspendCancellableCoroutine { continuation ->
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

    suspend fun getCoordinatesFromAddress(
        address: String,
        biasLat: Double? = null,
        biasLon: Double? = null
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext null

        val candidates = mutableListOf(address)
        if (address.contains(",")) {
            val stripped = address.substringAfter(",").trim()
            if (stripped.isNotBlank() && stripped != address) {
                candidates.add(stripped)
            }
            val lastPart = address.substringAfterLast(",").trim()
            if (lastPart.isNotBlank() && lastPart != stripped && lastPart != address) {
                candidates.add(lastPart)
            }
        }

        val geocoder = Geocoder(context, Locale.getDefault())

        for (candidate in candidates) {
            try {
                val addresses = if (biasLat != null && biasLon != null && (abs(biasLat) > 0.001 || abs(biasLon) > 0.001)) {
                    val lowerLeftLat = (biasLat - 2.0).coerceAtLeast(-90.0)
                    val lowerLeftLon = (biasLon - 2.0).coerceAtLeast(-180.0)
                    val upperRightLat = (biasLat + 2.0).coerceAtMost(90.0)
                    val upperRightLon = (biasLon + 2.0).coerceAtMost(180.0)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocationName(candidate, 5, lowerLeftLat, lowerLeftLon, upperRightLat, upperRightLon) { list ->
                                continuation.resume(list)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(candidate, 5, lowerLeftLat, lowerLeftLon, upperRightLat, upperRightLon)
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocationName(candidate, 5) { list ->
                                continuation.resume(list)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(candidate, 5)
                    }
                }

                if (!addresses.isNullOrEmpty()) {
                    for (loc in addresses) {
                        if (abs(loc.latitude) > 0.001 || abs(loc.longitude) > 0.001) {
                            if (biasLat != null && biasLon != null) {
                                val results = FloatArray(1)
                                Location.distanceBetween(biasLat, biasLon, loc.latitude, loc.longitude, results)
                                if (results[0] > 300_000f) {
                                    continue // Skip implausible result in another continent/country
                                }
                            }
                            return@withContext Pair(loc.latitude, loc.longitude)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
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
