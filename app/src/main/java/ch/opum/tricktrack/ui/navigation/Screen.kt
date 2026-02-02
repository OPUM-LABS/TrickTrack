package ch.opum.tricktrack.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import ch.opum.tricktrack.R

sealed class Screen(val route: String, @param:StringRes val title: Int, val icon: ImageVector) {
    object Review : Screen("review", R.string.screen_title_review, Icons.AutoMirrored.Filled.FactCheck)
    object TripList : Screen("trip_list", R.string.screen_title_trips, Icons.Default.Route)
    object PlacesList : Screen("places_list", R.string.screen_title_favourites, Icons.Default.Star)
    object Settings : Screen("settings", R.string.screen_title_settings, Icons.Default.Settings)
}