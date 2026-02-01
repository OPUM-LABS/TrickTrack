package ch.opum.tricktrack.ui.place

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.ui.ClearableTextField // Import ClearableTextField
import ch.opum.tricktrack.ui.LocationSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlaceDialog(
    place: SavedPlace?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double) -> Unit,
    placesViewModel: PlacesViewModel
) {
    var name by remember(place) { mutableStateOf(place?.name ?: "") }
    var addressText by remember(place) { mutableStateOf(place?.address ?: "") }
    var selectedLatitude by remember(place) { mutableStateOf(place?.latitude) }
    var selectedLongitude by remember(place) { mutableStateOf(place?.longitude) }

    val addressSuggestions by placesViewModel.addressSuggestions.collectAsState()
    val nameSuggestions by placesViewModel.nameSuggestions.collectAsState()

    var nameTextFieldSize by remember { mutableStateOf(Size.Zero) }
    var addressTextFieldSize by remember { mutableStateOf(Size.Zero) }

    fun clearAllSuggestions() {
        placesViewModel.clearAddressSuggestions()
        placesViewModel.clearNameSuggestions()
    }

    fun handleSuggestionClick(suggestion: LocationSuggestion) {
        name = suggestion.title
        addressText = suggestion.subtitle

        selectedLatitude = suggestion.latitude
        selectedLongitude = suggestion.longitude
        clearAllSuggestions()
    }

    AlertDialog(
        onDismissRequest = {
            clearAllSuggestions()
            onDismiss()
        },
        title = { Text(if (place == null) stringResource(R.string.add_place_title) else stringResource(R.string.edit_place_title)) },
        text = {
            Column {
                // --- Name Field with Autocomplete ---
                Box(modifier = Modifier.fillMaxWidth()) {
                    ClearableTextField( // Using ClearableTextField
                        value = name,
                        onValueChange = {
                            name = it
                            placesViewModel.searchName(it) // Pass String directly
                        },
                        label = { Text(stringResource(R.string.place_name_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nameTextFieldSize = it.size.toSize() }
                    )
                    SuggestionDropdown(
                        expanded = nameSuggestions.isNotEmpty(),
                        onDismissRequest = { placesViewModel.clearNameSuggestions() },
                        suggestions = nameSuggestions,
                        onSuggestionClick = ::handleSuggestionClick,
                        textFieldSize = nameTextFieldSize
                    )
                }

                // --- Address Field with Autocomplete ---
                Box(modifier = Modifier.fillMaxWidth()) {
                    ClearableTextField( // Using ClearableTextField
                        value = addressText,
                        onValueChange = {
                            addressText = it
                            placesViewModel.searchAddress(it) // Pass String directly
                        },
                        label = { Text(stringResource(R.string.place_address_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { addressTextFieldSize = it.size.toSize() }
                    )
                    SuggestionDropdown(
                        expanded = addressSuggestions.isNotEmpty(),
                        onDismissRequest = { placesViewModel.clearAddressSuggestions() },
                        suggestions = addressSuggestions,
                        onSuggestionClick = ::handleSuggestionClick,
                        textFieldSize = addressTextFieldSize
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedLatitude?.let { lat ->
                        selectedLongitude?.let { lon ->
                            onSave(name, addressText, lat, lon) // Pass String directly
                        }
                    }
                    onDismiss()
                },
                enabled = name.isNotBlank() && addressText.isNotBlank() && selectedLatitude != null
            ) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                clearAllSuggestions()
                onDismiss()
            }) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@Composable
private fun SuggestionDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    suggestions: List<LocationSuggestion>,
    onSuggestionClick: (LocationSuggestion) -> Unit,
    textFieldSize: Size
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = false),
        offset = DpOffset(x = 0.dp, y = 4.dp),
        modifier = Modifier
            .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
            .requiredSizeIn(maxHeight = 200.dp)
    ) {
        suggestions.forEach { suggestion ->
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (suggestion.isFavorite) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = stringResource(R.string.place_favorite_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Column {
                            Text(
                                suggestion.title,
                                fontWeight = if (suggestion.isFavorite) FontWeight.Bold else FontWeight.Normal
                            )
                            if (suggestion.subtitle.isNotEmpty()) {
                                Text(
                                    suggestion.subtitle,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}
