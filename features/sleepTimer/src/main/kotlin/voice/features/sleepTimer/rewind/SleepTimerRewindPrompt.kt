package voice.features.sleepTimer.rewind

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import voice.core.common.rootGraphAs
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import voice.core.strings.R as StringsR

/**
 * Offers to rewind after the sleep timer stopped playback. Shown app wide, so it is the first
 * thing the user sees when opening the app after falling asleep.
 */
@Composable
fun SleepTimerRewindPrompt() {
  val viewModel = remember { rootGraphAs<SleepTimerRewindGraph>().sleepTimerRewindViewModel }
  val viewState = viewModel.viewState() ?: return
  SleepTimerRewindDialog(
    viewState = viewState,
    onRewind = viewModel::onRewind,
    onDismiss = viewModel::onDismiss,
  )
}

@Composable
private fun SleepTimerRewindDialog(
  viewState: SleepTimerRewindViewState,
  onRewind: (RewindOption) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    // an accidental tap next to the dialog should not throw the offer away
    properties = DialogProperties(dismissOnClickOutside = false),
    onDismissRequest = onDismiss,
    icon = {
      Icon(imageVector = VoiceIcons.Bedtime, contentDescription = null)
    },
    title = {
      Text(stringResource(StringsR.string.sleep_timer_rewind_title))
    },
    text = {
      Column {
        Text(
          modifier = Modifier.padding(bottom = 8.dp),
          text = stringResource(StringsR.string.sleep_timer_rewind_message),
        )
        viewState.options.forEach { option ->
          ListItem(
            modifier = Modifier.clickable { onRewind(option) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
              Icon(imageVector = VoiceIcons.History, contentDescription = null)
            },
            supportingContent = if (option.isTimerStart) {
              { Text(stringResource(StringsR.string.sleep_timer_rewind_timer_start)) }
            } else {
              null
            },
          ) {
            Text(rewindAmountLabel(option.amount))
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(StringsR.string.sleep_timer_rewind_dismiss))
      }
    },
  )
}

@Composable
private fun rewindAmountLabel(amount: Duration): String {
  val minutes = amount.inWholeMinutes.toInt()
  return if (minutes > 0) {
    pluralStringResource(StringsR.plurals.duration_minutes, minutes, minutes)
  } else {
    val seconds = amount.inWholeSeconds.toInt()
    pluralStringResource(StringsR.plurals.duration_seconds, seconds, seconds)
  }
}

@ContributesTo(AppScope::class)
interface SleepTimerRewindGraph {
  val sleepTimerRewindViewModel: SleepTimerRewindViewModel
}

@Preview
@Composable
private fun SleepTimerRewindDialogPreview() {
  VoiceTheme {
    SleepTimerRewindDialog(
      viewState = SleepTimerRewindViewState(
        options = listOf(30, 25, 20, 15, 10, 5).map {
          RewindOption(amount = it.minutes, positionInBookMs = 0, isTimerStart = it == 30)
        },
      ),
      onRewind = {},
      onDismiss = {},
    )
  }
}
