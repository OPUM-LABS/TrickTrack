package ch.opum.tricktrack.data.repository

import android.content.Context
import android.util.Log
import ch.opum.tricktrack.data.AppHttpClient
import ch.opum.tricktrack.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.round

class DistanceRepository(context: Context) {

    private val client = AppHttpClient.client
    private val prefs = AppPreferences(context)

    companion object {
        private val routeCache = ConcurrentHashMap<String, List<GeoPoint>>()

        fun clearCache() {
            routeCache.clear()
        }
    }

    suspend fun getDrivingDistance(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Double? = withContext(Dispatchers.IO) {
        val baseUrl = prefs.getOsrmUrl().trim().removeSuffix("/")
        val url = "$baseUrl/$startLon,$startLat;$endLon,$endLat?overview=false&continue_straight=false&approaches=unrestricted;unrestricted"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(response.body.string())
                    val distanceInMeters = jsonObject.getJSONArray("routes").getJSONObject(0).getDouble("distance")
                    val distanceInKm = distanceInMeters / 1000.0
                    round(distanceInKm * 10) / 10
                } else {
                    Log.e("DistanceRepository", "OSRM distance call failed with code: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            // Log only the exception type: messages can contain the request URL with coordinates.
            Log.e("DistanceRepository", "OSRM distance call failed: ${e.javaClass.simpleName}")
            null
        }
    }

    suspend fun getOsrmRouteGeometry(startLat: Double, startLon: Double, endLat: Double, endLon: Double): List<GeoPoint>? = withContext(Dispatchers.IO) {
        val cacheKey = "%.5f,%.5f;%.5f,%.5f".format(startLat, startLon, endLat, endLon)
        routeCache[cacheKey]?.let {
            return@withContext it
        }

        val baseUrl = prefs.getOsrmUrl().trim().removeSuffix("/")
        val url = "$baseUrl/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson&continue_straight=false&approaches=unrestricted;unrestricted"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseData = response.body.string()
                    val jsonObject = JSONObject(responseData)
                    val routes = jsonObject.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).optJSONObject("geometry")
                        val coordinates = geometry?.optJSONArray("coordinates")
                        if (coordinates != null) {
                            val geoPoints = mutableListOf<GeoPoint>()
                            for (i in 0 until coordinates.length()) {
                                val coord = coordinates.getJSONArray(i)
                                val lon = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                geoPoints.add(GeoPoint(lat, lon))
                            }
                            if (geoPoints.isNotEmpty()) {
                                routeCache[cacheKey] = geoPoints
                            }
                            return@withContext geoPoints
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("DistanceRepository", "OSRM route geometry call failed: ${e.javaClass.simpleName}")
            null
        }
    }
}
