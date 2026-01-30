package ch.opum.tricktrack.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width // Added import for Modifier.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.AppPreferences
import ch.opum.tricktrack.data.ServerValidator
import kotlinx.coroutines.launch

@Composable
fun ServerSettingsDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    val appPreferences = remember { AppPreferences(context) }
    val serverValidator = remember { ServerValidator() }
    val coroutineScope = rememberCoroutineScope()

    var osrmUrlInput by remember { mutableStateOf(appPreferences.getOsrmUrl()) }
    var photonUrlInput by remember { mutableStateOf(appPreferences.getPhotonUrl()) }

    // State for OSRM test button visual feedback
    var osrmTestIcon by remember { mutableStateOf<ImageVector>(Icons.Default.Refresh) }
    var osrmTestColor by remember { mutableStateOf(Color.Unspecified) } // Default color
    var isOsrmValid by remember { mutableStateOf(false) }

    // State for Photon test button visual feedback
    var photonTestIcon by remember { mutableStateOf<ImageVector>(Icons.Default.Refresh) }
    var photonTestColor by remember { mutableStateOf(Color.Unspecified) } // Default color
    var isPhotonValid by remember { mutableStateOf(false) }

    // Get error color from MaterialTheme outside the coroutine scope
    val errorColor = MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.api_settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // OSRM URL Section
                OutlinedTextField(
                    value = osrmUrlInput,
                    onValueChange = {
                        osrmUrlInput = it
                        // Reset validation status if URL changes
                        isOsrmValid = false
                        osrmTestIcon = Icons.Default.Refresh
                        osrmTestColor = Color.Unspecified
                    },
                    label = { Text(stringResource(R.string.osrm_server_url)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        osrmUrlInput = AppPreferences.DEFAULT_OSRM_URL
                        isOsrmValid = false
                        osrmTestIcon = Icons.Default.Refresh
                        osrmTestColor = Color.Unspecified
                        Toast.makeText(context, R.string.reset_to_defaults_message_osrm, Toast.LENGTH_SHORT).show()
                    }) {
                        Text(stringResource(R.string.reset_to_defaults))
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                osrmTestIcon = Icons.Default.Refresh // Show refresh while testing
                                osrmTestColor = Color.Unspecified
                                val valid = serverValidator.validateOsrm(osrmUrlInput)
                                isOsrmValid = valid
                                osrmTestIcon = if (valid) Icons.Default.CheckCircle else Icons.Default.Cancel
                                osrmTestColor = if (valid) Color.Green else errorColor // Use the captured errorColor
                                Toast.makeText(
                                    context,
                                    if (valid) R.string.server_valid else R.string.osrm_connection_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = osrmTestColor)
                    ) {
                        Icon(imageVector = osrmTestIcon, contentDescription = stringResource(R.string.test_server))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.test_server))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Photon URL Section
                OutlinedTextField(
                    value = photonUrlInput,
                    onValueChange = {
                        photonUrlInput = it
                        // Reset validation status if URL changes
                        isPhotonValid = false
                        photonTestIcon = Icons.Default.Refresh
                        photonTestColor = Color.Unspecified
                    },
                    label = { Text(stringResource(R.string.photon_server_url)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        photonUrlInput = AppPreferences.DEFAULT_PHOTON_URL
                        isPhotonValid = false
                        photonTestIcon = Icons.Default.Refresh
                        photonTestColor = Color.Unspecified
                        Toast.makeText(context, R.string.reset_to_defaults_message_photon, Toast.LENGTH_SHORT).show()
                    }) {
                        Text(stringResource(R.string.reset_to_defaults))
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                photonTestIcon = Icons.Default.Refresh // Show refresh while testing
                                photonTestColor = Color.Unspecified
                                val valid = serverValidator.validatePhoton(photonUrlInput)
                                isPhotonValid = valid
                                photonTestIcon = if (valid) Icons.Default.CheckCircle else Icons.Default.Cancel
                                photonTestColor = if (valid) Color.Green else errorColor // Use the captured errorColor
                                Toast.makeText(
                                    context,
                                    if (valid) R.string.server_valid else R.string.photon_connection_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = photonTestColor)
                    ) {
                        Icon(imageVector = photonTestIcon, contentDescription = stringResource(R.string.test_server))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.test_server))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    appPreferences.setOsrmUrl(osrmUrlInput)
                    appPreferences.setPhotonUrl(photonUrlInput)
                    Toast.makeText(context, R.string.servers_saved_successfully, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                enabled = isOsrmValid && isPhotonValid
            ) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
