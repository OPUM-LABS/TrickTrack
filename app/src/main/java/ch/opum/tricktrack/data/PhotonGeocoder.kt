package ch.opum.tricktrack.data

import android.content.Context
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One result from the Photon API, reduced to the fields TrickTrack needs. */
data class PhotonPlace(
    val name: String?,
    val street: String?,
    val housenumber: String?,
    val postcode: String?,
    val city: String?,
    val type: String?,
    val lat: Double,
    val lon: Double
)

/**
 * Geocoding through the user's Photon server (OpenStreetMap data, self-hostable).
 * Replaces Android's Geocoder, which sends coordinates and addresses to Google on most devices.
 *
 * Network or parsing failures throw; callers decide on the fallback.
 */
class PhotonGeocoder(context: Context) {

    private val prefs = AppPreferences(context)

    // Same budget as the Android Geocoder calls it replaces (3s), plus a little for the connection.
    private val client = AppHttpClient.client.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .build()

    /** Places near the coordinates, nearest first. */
    fun reverse(lat: Double, lon: Double): List<PhotonPlace> {
        val url = reverseBaseUrl().newBuilder()
            .addQueryParameter("lat", lat.toString())
            .addQueryParameter("lon", lon.toString())
            .addQueryParameter("limit", "10")
            .addQueryParameter("radius", "0.1") // km
            .build()
        return fetch(url)
    }

    /**
     * Places matching a free-text query. With a bias point, results near it rank higher and are
     * limited to a box of ±[boxDegrees] around it (the same box the Android Geocoder call used).
     */
    fun search(query: String, limit: Int, biasLat: Double? = null, biasLon: Double? = null, boxDegrees: Double = 2.0): List<PhotonPlace> {
        val builder = searchBaseUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("limit", limit.toString())
        if (biasLat != null && biasLon != null) {
            val minLat = (biasLat - boxDegrees).coerceAtLeast(-90.0)
            val minLon = (biasLon - boxDegrees).coerceAtLeast(-180.0)
            val maxLat = (biasLat + boxDegrees).coerceAtMost(90.0)
            val maxLon = (biasLon + boxDegrees).coerceAtMost(180.0)
            builder.addQueryParameter("lat", biasLat.toString())
            builder.addQueryParameter("lon", biasLon.toString())
            builder.addQueryParameter("bbox", "$minLon,$minLat,$maxLon,$maxLat")
        }
        return fetch(builder.build())
    }

    private fun fetch(url: HttpUrl): List<PhotonPlace> {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Photon request failed with code ${response.code}")
            }
            return parse(response.body.string())
        }
    }

    // The configured URL is the search endpoint, e.g. https://photon.komoot.io/api
    private fun searchBaseUrl(): HttpUrl =
        prefs.getPhotonUrl().trim().removeSuffix("/").toHttpUrl()

    // Photon serves reverse lookups next to the search endpoint: .../api -> .../reverse
    private fun reverseBaseUrl(): HttpUrl {
        val base = prefs.getPhotonUrl().trim().removeSuffix("/")
        val reverse = if (base.endsWith("/api")) base.removeSuffix("/api") + "/reverse" else "$base/reverse"
        return reverse.toHttpUrl()
    }

    companion object {
        fun parse(json: String): List<PhotonPlace> {
            val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
            val places = mutableListOf<PhotonPlace>()
            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val props = feature.optJSONObject("properties") ?: continue
                val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue
                fun str(key: String) = props.optString(key, "").takeIf { it.isNotBlank() }
                places += PhotonPlace(
                    name = str("name"),
                    street = str("street"),
                    housenumber = str("housenumber"),
                    postcode = str("postcode"),
                    city = str("city") ?: str("town") ?: str("village"),
                    type = str("type"),
                    lat = coords.getDouble(1),
                    lon = coords.getDouble(0)
                )
            }
            return places
        }
    }
}

/** Pure address logic, kept free of Android and JSON classes so it can be unit-tested. */
object PhotonAddressFormatter {

    /** A house number further away than this is likely the neighbouring building, so it's not used. */
    const val MAX_HOUSENUMBER_DISTANCE_M = 50.0

    private val placeTypes = setOf("city", "town", "village", "locality", "district")

    /**
     * Picks the result that best describes the point: the nearest one with a house number within
     * [MAX_HOUSENUMBER_DISTANCE_M], otherwise the nearest one with a street, otherwise the nearest.
     */
    fun pickBestReverse(places: List<PhotonPlace>, lat: Double, lon: Double): PhotonPlace? {
        if (places.isEmpty()) return null
        val byDistance = places.sortedBy { distanceMeters(lat, lon, it.lat, it.lon) }
        return byDistance.firstOrNull {
            streetOf(it) != null && it.housenumber != null &&
                distanceMeters(lat, lon, it.lat, it.lon) <= MAX_HOUSENUMBER_DISTANCE_M
        } ?: byDistance.firstOrNull { streetOf(it) != null }
        ?: byDistance.first()
    }

    /**
     * "Street Number, Postcode City" — the same format the Android Geocoder version produced.
     * Returns null when the place has neither a street nor a city.
     */
    fun format(place: PhotonPlace?): String? {
        if (place == null) return null
        val street = streetOf(place) ?: ""
        val number = if (street.isNotEmpty()) place.housenumber ?: "" else ""
        val city = place.city ?: place.name.takeIf { place.type in placeTypes } ?: ""
        val postcode = place.postcode ?: ""

        val streetPart = if (street.isNotEmpty() && number.isNotEmpty()) "$street $number" else street
        val cityPart = if (postcode.isNotEmpty() && city.isNotEmpty()) "$postcode $city" else city

        return when {
            streetPart.isNotEmpty() && cityPart.isNotEmpty() -> "$streetPart, $cityPart"
            streetPart.isNotEmpty() -> streetPart
            cityPart.isNotEmpty() -> cityPart
            else -> null
        }
    }

    // Street results carry the street name in "name" rather than "street".
    private fun streetOf(place: PhotonPlace): String? =
        place.street ?: place.name.takeIf { place.type == "street" }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
