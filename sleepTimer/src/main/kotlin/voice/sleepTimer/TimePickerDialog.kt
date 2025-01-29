package voice.sleepTimer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.strings.R as StringsR

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = TimePickerState(
        initialHour,
        initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(id = StringsR.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState)
                },
            ) {
                Text(stringResource(id = StringsR.string.dialog_confirm))
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
            )
        },
    )
}
