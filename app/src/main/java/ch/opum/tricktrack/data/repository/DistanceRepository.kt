package ch.opum.tricktrack.data.repository

import android.content.Context
import android.util.Log
import ch.opum.tricktrack.data.AppPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.round

class DistanceRepository(context: Context) {

    private val client = OkHttpClient()
    private val prefs = AppPreferences(context)

    suspend fun getDrivingDistance(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Double? {
        val baseUrl = prefs.getOsrmUrl().trim().removeSuffix("/")
        val url = "$baseUrl/$startLon,$startLat;$endLon,$endLat?overview=false"
        Log.d("DistanceRepository", "Requesting URL: $url")
        val request = Request.Builder().url(url).build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseData = response.body?.string()
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
}
