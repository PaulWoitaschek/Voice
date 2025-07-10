package voice.sleepTimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import voice.strings.R as StringsR

@Composable
fun SleepTimerDialog(
  viewState: SleepTimerViewState,
  onDismiss: () -> Unit,
  onIncrementSleepTime: () -> Unit,
  onDecrementSleepTime: () -> Unit,
  onAcceptSleepTime: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCustomInputDialog by remember { mutableStateOf(false) }

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
                contentDescription = stringResource(id = StringsR.string.sleep_timer_button_decrement),
              )
            }
            IconButton(onClick = onIncrementSleepTime) {
              Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(id = StringsR.string.sleep_timer_button_increment),
              )
            }
          }
        },
      )
      ListItem(
        modifier = Modifier.clickable {
          showCustomInputDialog = true
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(StringsR.string.sleep_timer_custom_input),
          )
        },
        headlineContent = {
          Text(text = stringResource(StringsR.string.sleep_timer_custom_input))
        },
      )
      Spacer(modifier = Modifier.size(32.dp))
    }
  }

  if (showCustomInputDialog) {
    CustomTimeInputDialog(
      onDismiss = { showCustomInputDialog = false },
      onConfirm = { minutes ->
        onAcceptSleepTime(minutes)
        showCustomInputDialog = false
      },
    )
  }
}

@Composable
private fun CustomTimeInputDialog(
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit,
) {
  var timeInput by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = stringResource(StringsR.string.sleep_timer_custom_dialog_title)) },
    text = {
      Column {
        OutlinedTextField(
          value = timeInput,
          onValueChange = { newValue ->
            // Only allow digits
            if (newValue.all { it.isDigit() }) {
              timeInput = newValue
            }
          },
          label = { Text(stringResource(StringsR.string.sleep_timer_custom_dialog_label)) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .focusRequester(focusRequester),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              val minutes = timeInput.toIntOrNull()
              if (minutes != null && minutes > 0) {
                onConfirm(minutes)
              }
            },
          ),
        )
      }
    },
    confirmButton = {
      val minutes = timeInput.toIntOrNull()
      Button(
        onClick = {
          if (minutes != null && minutes > 0) {
            onConfirm(minutes)
          }
        },
        enabled = minutes != null && minutes > 0,
      ) {
        Text(stringResource(StringsR.string.dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(StringsR.string.dialog_cancel))
      }
    },
  )

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
}

@Composable
@ReadOnlyComposable
private fun minutes(minutes: Int): String {
  return pluralStringResource(StringsR.plurals.minutes, minutes, minutes)
}
