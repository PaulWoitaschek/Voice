import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import voice.settings.views.TimePickerDialog
import java.time.LocalTime

@Composable
internal fun AutoSleepTimerSetting(
  autoSleepTimer: Boolean,
  time: String,
  label: String,
  setAutoSleepTime: (Int, Int) -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val shouldShowTimePicker = remember { mutableStateOf(false) }
    Text(
      text = label,
      color = LocalContentColor.current.copy(alpha = if (autoSleepTimer) 1.0F else 0.5F),
    )
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
}
