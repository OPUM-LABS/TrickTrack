package ch.opum.tricktrack.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ch.opum.tricktrack.R

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val privacyText = remember {
        try {
            context.assets.open("PRIVACY.md").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "Unable to load Privacy Policy."
        }
    }

    val lines = remember(privacyText) {
        privacyText.lines()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.85f),
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.about_privacy_policy),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var prevWasBlank = false
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) {
                        if (!prevWasBlank) {
                            Spacer(modifier = Modifier.height(4.dp))
                            prevWasBlank = true
                        }
                        continue
                    }
                    prevWasBlank = false

                    when {
                        trimmed.startsWith("# ") -> {
                            Text(
                                text = trimmed.removePrefix("# ").trim(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        trimmed.startsWith("### ") -> {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = trimmed.removePrefix("### ").trim(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        trimmed == "---" -> {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        }
                        trimmed.startsWith("* ") -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = "• ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatMarkdownSpans(trimmed.removePrefix("* ").trim()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = formatMarkdownSpans(trimmed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_close))
            }
        }
    )
}

@Composable
private fun formatMarkdownSpans(text: String): AnnotatedString {
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    return buildAnnotatedString {
        val regex = Regex("""(\*\*([^*]+)\*\*|`([^`]+)`)""")
        var lastIndex = 0
        for (match in regex.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }
            val boldText = match.groups[2]?.value
            val codeText = match.groups[3]?.value
            if (boldText != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldText)
                }
            } else if (codeText != null) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                    append(codeText)
                }
            }
            lastIndex = end
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
