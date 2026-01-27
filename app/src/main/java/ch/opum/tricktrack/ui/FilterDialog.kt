package ch.opum.tricktrack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.opum.tricktrack.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentFilterState: FilterState,
    onApplyFilter: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf(currentFilterState.keyword) }
    var selectedType by remember { mutableStateOf<TripType?>(currentFilterState.type.takeIf { it != TripType.ALL }) }
    var startDate by remember { mutableStateOf(currentFilterState.startDate) }
    var endDate by remember { mutableStateOf(currentFilterState.endDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_trips_title)) },
        text = {
            Column {
                ClearableTextField( // Using ClearableTextField
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.filter_keyword_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.filter_trip_type_label), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    val cornerRadius = 50.dp

                    // "All" Button
                    TripTypeButton(
                        text = stringResource(R.string.filter_trip_type_all),
                        isSelected = selectedType == null,
                        onClick = { selectedType = null },
                        shape = RoundedCornerShape(
                            topStart = cornerRadius,
                            bottomStart = cornerRadius
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .then(if (selectedType == null) Modifier.zIndex(1f) else Modifier)
                    )

                    // "Business" Button
                    TripTypeButton(
                        text = stringResource(R.string.trip_type_business),
                        isSelected = selectedType == TripType.BUSINESS,
                        onClick = { selectedType = TripType.BUSINESS },
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
                            .then(if (selectedType == TripType.BUSINESS) Modifier.zIndex(1f) else Modifier)
                    )

                    // "Personal" Button
                    TripTypeButton(
                        text = stringResource(R.string.trip_type_personal),
                        isSelected = selectedType == TripType.PERSONAL,
                        onClick = { selectedType = TripType.PERSONAL },
                        shape = RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius),
                        modifier = Modifier
                            .weight(1f)
                            .then(if (selectedType == TripType.PERSONAL) Modifier.zIndex(1f) else Modifier)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                DateSelectionField(
                    label = stringResource(R.string.filter_start_date_label),
                    selectedDate = startDate,
                    onClick = { showStartDatePicker = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DateSelectionField(
                    label = stringResource(R.string.filter_end_date_label),
                    selectedDate = endDate,
                    onClick = { showEndDatePicker = true }
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    keyword = ""
                    selectedType = null
                    startDate = null
                    endDate = null
                }) {
                    Text(stringResource(R.string.filter_reset_button))
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.button_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val endOfDay = endDate?.let {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = it
                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                            calendar.set(Calendar.MINUTE, 59)
                            calendar.set(Calendar.SECOND, 59)
                            calendar.set(Calendar.MILLISECOND, 999)
                            calendar.timeInMillis
                        }

                        onApplyFilter(
                            FilterState(
                                keyword = keyword,
                                type = selectedType ?: TripType.ALL,
                                startDate = startDate,
                                endDate = endOfDay
                            )
                        )
                    }) {
                        Text(stringResource(R.string.button_apply))
                    }
                }
            }
        }
    )

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DateSelectionField(
    label: String,
    selectedDate: Long?,
    onClick: () -> Unit
) {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateText = selectedDate?.let { formatter.format(Date(it)) } ?: ""

    Box {
        OutlinedTextField(
            value = dateText,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
fun TripTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.primary

    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = shape,
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier
    ) {
        Text(text)
    }
}