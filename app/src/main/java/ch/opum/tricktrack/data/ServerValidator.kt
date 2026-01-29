package ch.opum.tricktrack.data

import android.util.Log // Import Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ServerValidator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun validateOsrm(baseUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            val TAG = "ServerValidator"
            try {
                val url = baseUrl.trim().removeSuffix("/") + "/0,0;0,0" // Test route
                Log.d(TAG, "Validating OSRM URL: $url")
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "OSRM Response Code: ${response.code}, isSuccessful: ${response.isSuccessful}")

                    // OSRM returns 400 Bad Request for invalid routes like 0,0;0,0
                    // We consider this a successful validation if the body is still OSRM-like JSON
                    if (response.code != 200 && response.code != 400) {
                        Log.d(TAG, "OSRM validation failed: HTTP status not 200 OK or 400 Bad Request.")
                        return@withContext false
                    }
                    
                    val responseBody = response.body?.string()
                    Log.d(TAG, "OSRM Response Body: $responseBody")
                    if (responseBody.isNullOrEmpty()) {
                        Log.d(TAG, "OSRM validation failed: Empty response body.")
                        return@withContext false
                    }
                    // Check if the response is a valid JSON object and contains an "code" field
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val hasCodeField = jsonObject.has("code")
                        Log.d(TAG, "OSRM JSON has 'code' field: $hasCodeField")
                        hasCodeField
                    } catch (e: JSONException) {
                        Log.e(TAG, "OSRM validation failed: Response is not a valid JSON object.", e)
                        false
                    }
                }
            } catch (e: Exception) {
                // Any network or parsing exception means validation failed
                Log.e(TAG, "OSRM validation failed with exception: ${e.message}", e)
                false
            }
        }
    }

    suspend fun validatePhoton(baseUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            val TAG = "ServerValidator"
            try {
                val url = baseUrl.trim().removeSuffix("/") + "?q=berlin"
                Log.d(TAG, "Validating Photon URL: $url")
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Photon Response Code: ${response.code}, isSuccessful: ${response.isSuccessful}")
                    response.isSuccessful && response.code == 200
                }
            } catch (e: Exception) {
                Log.e(TAG, "Photon validation failed with exception: ${e.message}", e)
                false
            }
        }
    }
}
