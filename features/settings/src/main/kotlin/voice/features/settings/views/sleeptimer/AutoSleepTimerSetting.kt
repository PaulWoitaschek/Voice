package voice.features.settings.views.sleeptimer

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import voice.features.settings.views.TimePickerDialog
import java.time.LocalTime

@Composable
internal fun AutoSleepTimerSetting(
  time: LocalTime,
  label: String,
  enabled: Boolean,
  setAutoSleepTime: (LocalTime) -> Unit,
) {
  var shouldShowTimePicker by remember { mutableStateOf(false) }
  TextButton(
    enabled = enabled,
    onClick = {
      shouldShowTimePicker = true
    },
  ) {
    Text(label)
  }
  if (shouldShowTimePicker) {
    TimePickerDialog(
      initialHour = time.hour,
      initialMinute = time.minute,
      onConfirm = { timePickerState ->
        setAutoSleepTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
        shouldShowTimePicker = false
      },
      onDismiss = {
        shouldShowTimePicker = false
      },
    )
  }
}
