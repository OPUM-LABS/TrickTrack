package ch.opum.tricktrack.data.repository

import android.content.Context
import android.util.Log
import ch.opum.tricktrack.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.round

class DistanceRepository(context: Context) {

    private val client = OkHttpClient()
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
        Log.d("DistanceRepository", "Requesting URL: $url")
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseData = response.body.string()
                Log.d("DistanceRepository", "Response: $responseData")
                val jsonObject = JSONObject(responseData)
                val distanceInMeters = jsonObject.getJSONArray("routes").getJSONObject(0).getDouble("distance")
                val distanceInKm = distanceInMeters / 1000.0
                round(distanceInKm * 10) / 10
            } else {
                Log.e("DistanceRepository", "API call failed with code: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("DistanceRepository", "API call failed with exception", e)
            null
        }
    }

    suspend fun getOsrmRouteGeometry(startLat: Double, startLon: Double, endLat: Double, endLon: Double): List<GeoPoint>? = withContext(Dispatchers.IO) {
        val cacheKey = "%.5f,%.5f;%.5f,%.5f".format(startLat, startLon, endLat, endLon)
        routeCache[cacheKey]?.let {
            Log.d("DistanceRepository", "Returning cached in-memory OSRM route geometry for $cacheKey")
            return@withContext it
        }

        val baseUrl = prefs.getOsrmUrl().trim().removeSuffix("/")
        val url = "$baseUrl/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson&continue_straight=false&approaches=unrestricted;unrestricted"
        Log.d("DistanceRepository", "Requesting Route Geometry URL: $url")
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
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
            null
        } catch (e: Exception) {
            Log.e("DistanceRepository", "OSRM route geometry call failed", e)
            null
        }
    }
}
