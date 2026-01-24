package ch.opum.tricktrack.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import ch.opum.tricktrack.R

@Composable
fun ClearableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null, // Added placeholder parameter
    isError: Boolean = false // Added isError parameter
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_text)
                    )
                }
            } else {
                trailingIcon?.invoke()
            }
        },
        placeholder = placeholder, // Pass placeholder to OutlinedTextField
        isError = isError // Pass isError to OutlinedTextField
    )
}

@Preview(showBackground = true)
@Composable
fun ClearableTextFieldPreview() {
    var text by remember { mutableStateOf("Hello") }
    ClearableTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.name)) },
        placeholder = { Text(stringResource(R.string.enter_the_name)) }
    )
}
