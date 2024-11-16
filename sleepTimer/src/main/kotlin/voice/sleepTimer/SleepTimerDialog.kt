package voice.sleepTimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import voice.strings.R as StringsR

@Composable
fun SleepTimerDialog(
  viewState: SleepTimerViewState,
  onDismiss: () -> Unit,
  onIncrementSleepTime: () -> Unit,
  onDecrementSleepTime: () -> Unit,
  onAcceptSleepTime: (Int) -> Unit,
  onCheckAutoSleepTimer: (Boolean) -> Unit,
  onSetAutoSleepTimerStart: (Int, Int) -> Unit,
  onSetAutoSleepTimerEnd: (Int, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  ModalBottomSheet(
    modifier = modifier,
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column {
      Text(
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .fillMaxWidth(),
        text = stringResource(id = StringsR.string.sleep_timer_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.size(16.dp))
      listOf(5, 15, 30, 60).forEach { time ->
        ListItem(
          modifier = Modifier.clickable {
            onAcceptSleepTime(time)
          },
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          headlineContent = {
            Text(text = minutes(minutes = time))
          },
        )
      }
      ListItem(
        modifier = Modifier.clickable {
          onAcceptSleepTime(viewState.customSleepTime)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
          Text(text = minutes(minutes = viewState.customSleepTime))
        },
        trailingContent = {
          Row {
            IconButton(onClick = onDecrementSleepTime) {
              Icon(
                imageVector = Icons.Outlined.Remove,
                stringResource(id = StringsR.string.sleep_timer_button_decrement),
              )
            }
            IconButton(onClick = onIncrementSleepTime) {
              Icon(
                imageVector = Icons.Outlined.Add,
                stringResource(id = StringsR.string.sleep_timer_button_increment),
              )
            }
          }
        },
      )
      ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
          Text(text = stringResource(id = StringsR.string.sleep_timer_auto_start))
        },
        trailingContent = {
          Switch(
            checked = viewState.autoSleepTimer,
            onCheckedChange = onCheckAutoSleepTimer,
          )
        },
      )
      if (viewState.autoSleepTimer) {
        val shouldShowStartTimePicker = remember { mutableStateOf(false) }
        ListItem(
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          headlineContent = {
            Text(text = "Start Timer after ${viewState.autoSleepTimeStart}")
          },
          trailingContent = {
            if (shouldShowStartTimePicker.value) {
              TimePickerDialog(
                viewState.autoSleepTimeStart,
                { timePickerState: TimePickerState ->
                  onSetAutoSleepTimerStart(timePickerState.hour, timePickerState.minute)
                  shouldShowStartTimePicker.value = false
                },
                { shouldShowStartTimePicker.value = false },
              )
            }
            IconButton(
              onClick = {
                shouldShowStartTimePicker.value = true
              },
            ) {
              Icon(
                imageVector = Icons.Outlined.AccessTime,
                "Set Auto Sleep Timer Start Time",
              )
            }
          },
        )
        val shouldShowEndTimePicker = remember { mutableStateOf(false) }
        ListItem(
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          headlineContent = {
            Text(text = "Start timer before ${viewState.autoSleepTimeEnd}")
          },
          trailingContent = {
            if (shouldShowEndTimePicker.value) {
              TimePickerDialog(
                viewState.autoSleepTimeEnd,
                { timePickerState: TimePickerState ->
                  onSetAutoSleepTimerEnd(timePickerState.hour, timePickerState.minute)
                  shouldShowEndTimePicker.value = false
                },
                { shouldShowEndTimePicker.value = false },
              )
            }
            IconButton(
              onClick = {
                shouldShowEndTimePicker.value = true
              },
            ) {
              Icon(
                imageVector = Icons.Outlined.AccessTime,
                "Set Auto Sleep Timer End Time",
              )
            }
          },
        )
      }
      Spacer(modifier = Modifier.size(32.dp))
    }
  }
}

@Composable
@ReadOnlyComposable
private fun minutes(minutes: Int): String {
  return pluralStringResource(StringsR.plurals.minutes, minutes, minutes)
}

@Composable
fun TimePickerDialog(
  initialTime: String,
  onConfirm: (TimePickerState) -> Unit,
  onDismiss: () -> Unit,
) {
  val initialLocalTime = LocalTime.parse(initialTime)
  val timePickerState = rememberTimePickerState(
    initialLocalTime.hour,
    initialLocalTime.minute,
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
