package ch.opum.tricktrack.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.AppPreferences
import ch.opum.tricktrack.data.ServerValidator
import ch.opum.tricktrack.ui.DialogAcceptButton
import ch.opum.tricktrack.ui.DialogDeclineButton
import ch.opum.tricktrack.ui.DialogResetButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    val appPreferences = remember { AppPreferences(context) }
    val serverValidator = remember { ServerValidator() }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var osrmUrlInput by remember { mutableStateOf(appPreferences.getOsrmUrl()) }
    var photonUrlInput by remember { mutableStateOf(appPreferences.getPhotonUrl()) }

    var osrmTestIcon by remember { mutableStateOf<ImageVector>(Icons.Default.Refresh) }
    var osrmTestColor by remember { mutableStateOf(Color.Unspecified) }
    var isOsrmValid by remember { mutableStateOf(false) }

    var photonTestIcon by remember { mutableStateOf<ImageVector>(Icons.Default.Refresh) }
    var photonTestColor by remember { mutableStateOf(Color.Unspecified) }
    var isPhotonValid by remember { mutableStateOf(false) }

    val errorColor = MaterialTheme.colorScheme.error
    val successColor = Color(0xFF2E7D32) // Darker green for accessibility

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.api_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // OSRM Section
            ServerConfigCard(
                title = "OSRM Server",
                url = osrmUrlInput,
                onUrlChange = {
                    osrmUrlInput = it
                    isOsrmValid = false
                    osrmTestIcon = Icons.Default.Refresh
                    osrmTestColor = Color.Unspecified
                },
                onReset = {
                    osrmUrlInput = AppPreferences.DEFAULT_OSRM_URL
                    isOsrmValid = false
                    osrmTestIcon = Icons.Default.Refresh
                    osrmTestColor = Color.Unspecified
                },
                onTest = {
                    coroutineScope.launch {
                        osrmTestIcon = Icons.Default.Refresh
                        osrmTestColor = Color.Unspecified
                        val valid = serverValidator.validateOsrm(osrmUrlInput)
                        isOsrmValid = valid
                        osrmTestIcon = if (valid) Icons.Default.CheckCircle else Icons.Default.Cancel
                        osrmTestColor = if (valid) successColor else errorColor
                    }
                },
                testIcon = osrmTestIcon,
                testColor = osrmTestColor,
                label = stringResource(R.string.osrm_server_url)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Photon Section
            ServerConfigCard(
                title = "Photon Geocoder",
                url = photonUrlInput,
                onUrlChange = {
                    photonUrlInput = it
                    isPhotonValid = false
                    photonTestIcon = Icons.Default.Refresh
                    photonTestColor = Color.Unspecified
                },
                onReset = {
                    photonUrlInput = AppPreferences.DEFAULT_PHOTON_URL
                    isPhotonValid = false
                    photonTestIcon = Icons.Default.Refresh
                    photonTestColor = Color.Unspecified
                },
                onTest = {
                    coroutineScope.launch {
                        photonTestIcon = Icons.Default.Refresh
                        photonTestColor = Color.Unspecified
                        val valid = serverValidator.validatePhoton(photonUrlInput)
                        isPhotonValid = valid
                        photonTestIcon = if (valid) Icons.Default.CheckCircle else Icons.Default.Cancel
                        photonTestColor = if (valid) successColor else errorColor
                    }
                },
                testIcon = photonTestIcon,
                testColor = photonTestColor,
                label = stringResource(R.string.photon_server_url)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogResetButton(
                    onClick = {
                        osrmUrlInput = AppPreferences.DEFAULT_OSRM_URL
                        photonUrlInput = AppPreferences.DEFAULT_PHOTON_URL
                        isOsrmValid = false
                        isPhotonValid = false
                        osrmTestIcon = Icons.Default.Refresh
                        photonTestIcon = Icons.Default.Refresh
                        osrmTestColor = Color.Unspecified
                        photonTestColor = Color.Unspecified
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                DialogDeclineButton(onClick = onDismiss)
                DialogAcceptButton(
                    onClick = {
                        appPreferences.setOsrmUrl(osrmUrlInput)
                        appPreferences.setPhotonUrl(photonUrlInput)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    enabled = isOsrmValid && isPhotonValid
                )
            }
        }
    }
}

@Composable
fun ServerConfigCard(
    title: String,
    url: String,
    label: String,
    onUrlChange: (String) -> Unit,
    onReset: () -> Unit,
    onTest: () -> Unit,
    testIcon: ImageVector,
    testColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelMedium)
                }
                
                val containerColor = if (testColor == Color.Unspecified) 
                    MaterialTheme.colorScheme.secondaryContainer 
                    else testColor
                val contentColor = if (testColor == Color.Unspecified) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                    else Color.White

                Button(
                    onClick = onTest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = testIcon, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp), 
                        tint = contentColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.test_server), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
