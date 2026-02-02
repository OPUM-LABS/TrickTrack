package ch.opum.tricktrack.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("tricktrack_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_OSRM_URL = "https://router.project-osrm.org/route/v1/driving" // Changed to HTTPS
        const val DEFAULT_PHOTON_URL = "https://photon.komoot.io/api"
        const val KEY_DEFAULT_DRIVER_ID = "defaultDriverId"
        const val KEY_DEFAULT_COMPANY_ID = "defaultCompanyId"
        const val KEY_DEFAULT_VEHICLE_ID = "defaultVehicleId"
        const val KEY_EXPORT_INCLUDE_DRIVER = "exportIncludeDriver"
        const val KEY_EXPORT_INCLUDE_COMPANY = "exportIncludeCompany"
        const val KEY_EXPORT_INCLUDE_VEHICLE = "exportIncludeVehicle"
    }

    fun getOsrmUrl(): String {
        return sharedPreferences.getString("osrm_url", DEFAULT_OSRM_URL) ?: DEFAULT_OSRM_URL
    }

    fun setOsrmUrl(url: String) {
        sharedPreferences.edit { putString("osrm_url", url) }
    }

    fun getPhotonUrl(): String {
        return sharedPreferences.getString("photon_url", DEFAULT_PHOTON_URL) ?: DEFAULT_PHOTON_URL
    }

    fun setPhotonUrl(url: String) {
        sharedPreferences.edit { putString("photon_url", url) }
    }

}
