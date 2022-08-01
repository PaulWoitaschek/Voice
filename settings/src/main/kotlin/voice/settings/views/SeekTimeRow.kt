package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import voice.settings.R

@Composable
internal fun SeekTimeRow(seekTimeInSeconds: Int, openSeekTimeDialog: () -> Unit) {
  ListItem(
    modifier = Modifier
      .clickable {
        openSeekTimeDialog()
      }
      .fillMaxWidth(),
    text = {
      Text(text = stringResource(R.string.pref_seek_time))
    },
    secondaryText = {
      Text(
        text = LocalContext.current.resources.getQuantityString(
          R.plurals.seconds,
          seekTimeInSeconds,
          seekTimeInSeconds,
        ),
      )
    },
  )
}

@Composable
internal fun SeekAmountDialog(
  currentSeconds: Int,
  onSecondsConfirmed: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  TimeSettingDialog(
    title = stringResource(R.string.pref_seek_time),
    currentSeconds = currentSeconds,
    minSeconds = 3,
    maxSeconds = 60,
    textPluralRes = R.plurals.seconds,
    onSecondsConfirmed = onSecondsConfirmed,
    onDismiss = onDismiss,
  )
}
