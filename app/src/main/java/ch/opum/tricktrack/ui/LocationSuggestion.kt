package ch.opum.tricktrack.ui

data class LocationSuggestion(
    val title: String,
    val subtitle: String,
    val fullAddress: String,
    val isFavorite: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val postalCode: String? = null
)
