package voice.sleepTimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
          Text(text = stringResource(id = StringsR.string.auto_sleep_timer))
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
          modifier = Modifier.padding(horizontal = 15.dp),
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          headlineContent = {
            Text(stringResource(id = StringsR.string.auto_sleep_timer_start))
          },
          trailingContent = {
            if (shouldShowStartTimePicker.value) {
              val initialTime = LocalTime.parse(viewState.autoSleepTimeStart)
              TimePickerDialog(
                initialTime.hour,
                initialTime.minute,
                { timePickerState: TimePickerState ->
                  onSetAutoSleepTimerStart(timePickerState.hour, timePickerState.minute)
                  shouldShowStartTimePicker.value = false
                },
                { shouldShowStartTimePicker.value = false },
              )
            }
            TextButton(
              onClick = {
                shouldShowStartTimePicker.value = true
              },
            ) {
              Text(viewState.autoSleepTimeStart)
            }
          },
        )
        val shouldShowEndTimePicker = remember { mutableStateOf(false) }
        ListItem(
          modifier = Modifier.padding(horizontal = 15.dp),
          colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          headlineContent = {
            Text(stringResource(id = StringsR.string.auto_sleep_timer_end))
          },
          trailingContent = {
            if (shouldShowEndTimePicker.value) {
              val initialTime = LocalTime.parse(viewState.autoSleepTimeEnd)
              TimePickerDialog(
                initialTime.hour,
                initialTime.minute,
                { timePickerState: TimePickerState ->
                  onSetAutoSleepTimerEnd(timePickerState.hour, timePickerState.minute)
                  shouldShowEndTimePicker.value = false
                },
                { shouldShowEndTimePicker.value = false },
              )
            }
            TextButton(
              onClick = {
                shouldShowEndTimePicker.value = true
              },
            ) {
              Text(viewState.autoSleepTimeEnd)
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
