package ch.opum.tricktrack.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    var osrmUrl by remember { mutableStateOf(appPreferences.getOsrmUrl()) }
    var photonUrl by remember { mutableStateOf(appPreferences.getPhotonUrl()) }
    var testStatus by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.server_settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = osrmUrl,
                    onValueChange = { osrmUrl = it },
                    label = { Text(stringResource(R.string.osrm_server_url)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = photonUrl,
                    onValueChange = { photonUrl = it },
                    label = { Text(stringResource(R.string.photon_server_url)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                coroutineScope.launch {
                    testStatus = context.getString(R.string.testing_servers)
                    val osrmValid = serverValidator.validateOsrm(osrmUrl)
                    val photonValid = serverValidator.validatePhoton(photonUrl)

                    if (osrmValid && photonValid) {
                        appPreferences.setOsrmUrl(osrmUrl)
                        appPreferences.setPhotonUrl(photonUrl)
                        testStatus = context.getString(R.string.servers_valid_and_saved)
                        Toast.makeText(context, R.string.servers_valid_and_saved, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        testStatus = ""
                        if (!osrmValid) {
                            testStatus += context.getString(R.string.osrm_connection_failed) + "\n"
                        }
                        if (!photonValid) {
                            testStatus += context.getString(R.string.photon_connection_failed) + "\n"
                        }
                        Toast.makeText(context, testStatus.trim(), Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                Text(stringResource(R.string.test_and_save))
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = {
                    osrmUrl = AppPreferences.DEFAULT_OSRM_URL
                    photonUrl = AppPreferences.DEFAULT_PHOTON_URL
                    appPreferences.setOsrmUrl(osrmUrl)
                    appPreferences.setPhotonUrl(photonUrl)
                    testStatus = context.getString(R.string.reset_to_defaults_message)
                    Toast.makeText(context, R.string.reset_to_defaults_message, Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.reset_to_defaults))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
