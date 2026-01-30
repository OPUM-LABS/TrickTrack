package ch.opum.tricktrack.data

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
            try {
                // Use a valid, short route for testing to ensure an "Ok" response code
                val url = baseUrl.trim().removeSuffix("/") + "/52.52,13.40;52.53,13.41"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext false
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        return@withContext false
                    }
                    try {
                        val jsonObject = JSONObject(responseBody)
                        jsonObject.has("code") && jsonObject.getString("code") == "Ok"
                    } catch (e: JSONException) {
                        false
                    }
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun validatePhoton(baseUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = baseUrl.trim().removeSuffix("/") + "?q=berlin&limit=1"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext false
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        return@withContext false
                    }
                    try {
                        val jsonObject = JSONObject(responseBody)
                        jsonObject.has("type") && jsonObject.getString("type") == "FeatureCollection"
                    } catch (e: JSONException) {
                        false
                    }
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
