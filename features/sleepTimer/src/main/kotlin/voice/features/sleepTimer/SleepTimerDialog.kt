package voice.features.sleepTimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import voice.core.strings.R as StringsR

@Composable
fun SleepTimerDialog(
  viewState: SleepTimerViewState,
  onDismiss: () -> Unit,
  onIncrementSleepTime: () -> Unit,
  onDecrementSleepTime: () -> Unit,
  onAcceptSleepTime: (Int) -> Unit,
  onAcceptSleepAtEndOfChapter: () -> Unit,
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
            ContinuousPressIcon(
              onEmit = onDecrementSleepTime,
              icon = Icons.Outlined.Remove,
              contentDescription = stringResource(id = StringsR.string.sleep_timer_button_decrement),
            )
            ContinuousPressIcon(
              onEmit = onIncrementSleepTime,
              icon = Icons.Outlined.Add,
              contentDescription = stringResource(id = StringsR.string.sleep_timer_button_increment),
            )
          }
        },
      )
      ListItem(
        modifier = Modifier.clickable(onClick = onAcceptSleepAtEndOfChapter),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
          Text(text = stringResource(id = StringsR.string.end_of_chapter))
        },
      )
      Spacer(modifier = Modifier.size(32.dp))
    }
  }
}

@Composable
private fun ContinuousPressIcon(
  onEmit: () -> Unit,
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  var isPressed by remember { mutableStateOf(false) }

  LaunchedEffect(isPressed, onEmit) {
    if (isPressed) {
      delay(500)
      while (isPressed) {
        onEmit()
        delay(100)
      }
    }
  }
  val interactionSource = remember { MutableInteractionSource() }
  Icon(
    imageVector = icon,
    contentDescription = contentDescription,
    modifier = modifier
      .size(48.dp)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = onEmit,
        onLongClick = { isPressed = true },
      )
      .padding(12.dp),
  )
  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      if (interaction is PressInteraction.Release ||
        interaction is PressInteraction.Cancel
      ) {
        isPressed = false
      }
    }
  }
}

@Composable
@ReadOnlyComposable
private fun minutes(minutes: Int): String {
  return pluralStringResource(StringsR.plurals.minutes, minutes, minutes)
}
