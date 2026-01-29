package ch.opum.tricktrack.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("tricktrack_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_OSRM_URL = "https://router.project-osrm.org/route/v1/driving" // Changed to HTTPS
        const val DEFAULT_PHOTON_URL = "https://photon.komoot.io/api"
    }

    fun getOsrmUrl(): String {
        return sharedPreferences.getString("osrm_url", DEFAULT_OSRM_URL) ?: DEFAULT_OSRM_URL
    }

    fun setOsrmUrl(url: String) {
        sharedPreferences.edit().putString("osrm_url", url).apply()
    }

    fun getPhotonUrl(): String {
        return sharedPreferences.getString("photon_url", DEFAULT_PHOTON_URL) ?: DEFAULT_PHOTON_URL
    }

    fun setPhotonUrl(url: String) {
        sharedPreferences.edit().putString("photon_url", url).apply()
    }
}
