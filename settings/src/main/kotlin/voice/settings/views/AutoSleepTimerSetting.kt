import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import voice.settings.views.TimePickerDialog
import java.time.LocalTime

@Composable
internal fun AutoSleepTimerSetting(
  autoSleepTimer: Boolean,
  time: String,
  label: String,
  setAutoSleepTime: (Int, Int) -> Unit,
) {
  val shouldShowTimePicker = remember { mutableStateOf(false) }
  CompositionLocalProvider(
    LocalContentColor provides
      if (autoSleepTimer) MaterialTheme.colorScheme.onSurface
      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
  ) {
    Text(text = label)
  }
  if (shouldShowTimePicker.value) {
    val initialTime = LocalTime.parse(time)
    TimePickerDialog(
      initialTime.hour,
      initialTime.minute,
      { timePickerState: TimePickerState ->
        setAutoSleepTime(timePickerState.hour, timePickerState.minute)
        shouldShowTimePicker.value = false
      },
      { shouldShowTimePicker.value = false },
    )
  }
  TextButton(
    onClick = {
      shouldShowTimePicker.value = true
    },
    enabled = autoSleepTimer,
  ) {
    Text(text = time)
  }
}
