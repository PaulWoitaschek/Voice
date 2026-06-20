package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun SeekTimeRow(
  seekTimeInSeconds: Int,
  openSeekTimeDialog: () -> Unit,
) {
  ListItem(
    modifier = Modifier
      .clickable {
        openSeekTimeDialog()
      }
      .fillMaxWidth(),
    leadingContent = {
      Icon(
        imageVector = VoiceIcons.Timelapse,
        contentDescription = stringResource(StringsR.string.settings_playback_seek_time_title),
      )
    },
    headlineContent = {
      Text(text = stringResource(StringsR.string.settings_playback_seek_time_title))
    },
    supportingContent = {
      Text(
        text = LocalResources.current.getQuantityString(
          StringsR.plurals.duration_seconds,
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
  onSecondsConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  TimeSettingDialog(
    title = stringResource(StringsR.string.settings_playback_seek_time_title),
    currentSeconds = currentSeconds,
    minSeconds = 3,
    maxSeconds = 60,
    textPluralRes = StringsR.plurals.duration_seconds,
    onSecondsConfirm = onSecondsConfirm,
    onDismiss = onDismiss,
  )
}
