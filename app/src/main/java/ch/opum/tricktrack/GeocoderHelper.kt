package ch.opum.tricktrack

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import ch.opum.tricktrack.data.PhotonAddressFormatter
import ch.opum.tricktrack.data.PhotonGeocoder
import ch.opum.tricktrack.data.place.SavedPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class GeocoderHelper(private val context: Context) {

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Address lookups go to the user's Photon server (OpenStreetMap), not Android's Geocoder,
    // which sends coordinates to Google on most devices.
    private val photon by lazy { PhotonGeocoder(context) }

    suspend fun getAddressFromLocation(
        lat: Double?,
        lng: Double?
    ): String = withContext(Dispatchers.IO) {
        val pending = context.getString(R.string.address_pending_offline)
        if (lat == null || lng == null) return@withContext pending
        if (!isNetworkAvailable()) return@withContext pending

        try {
            val best = PhotonAddressFormatter.pickBestReverse(photon.reverse(lat, lng), lat, lng)
            PhotonAddressFormatter.format(best)?.takeIf { it.isNotBlank() } ?: pending
        } catch (_: Exception) {
            pending
        }
    }

    suspend fun getAddressFromName(locationName: String): String = withContext(Dispatchers.IO) {
        if (locationName.isBlank()) return@withContext "Unknown Address"
        if (!isNetworkAvailable()) return@withContext context.getString(R.string.address_pending_offline)

        try {
            val first = photon.search(locationName, limit = 1).firstOrNull()
                ?: return@withContext "Unknown Address"
            PhotonAddressFormatter.format(first) ?: "Address not found"
        } catch (_: Exception) {
            locationName
        }
    }

    suspend fun getCoordinatesFromAddress(
        address: String,
        biasLat: Double? = null,
        biasLon: Double? = null
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (address.isBlank() || !isNetworkAvailable()) return@withContext null

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

        val useBiasBox = biasLat != null && biasLon != null && (abs(biasLat) > 0.001 || abs(biasLon) > 0.001)

        for (candidate in candidates) {
            try {
                val places = if (useBiasBox) {
                    photon.search(candidate, limit = 5, biasLat = biasLat, biasLon = biasLon)
                } else {
                    photon.search(candidate, limit = 5)
                }

                for (loc in places) {
                    if (abs(loc.lat) > 0.001 || abs(loc.lon) > 0.001) {
                        if (biasLat != null && biasLon != null) {
                            val results = FloatArray(1)
                            Location.distanceBetween(biasLat, biasLon, loc.lat, loc.lon, results)
                            if (results[0] > 300_000f) {
                                continue
                            }
                        }
                        return@withContext Pair(loc.lat, loc.lon)
                    }
                }
            } catch (_: Exception) {
            }
        }
        null
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
            currentLocation.distanceTo(favoriteLocation) < radius
        }

        return if (matchingFavorite != null) {
            "${matchingFavorite.name}, ${matchingFavorite.address}"
        } else {
            originalAddress
        }
    }
}
