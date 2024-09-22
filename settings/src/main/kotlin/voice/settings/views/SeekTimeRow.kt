package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import voice.strings.R as StringsR

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
        imageVector = Icons.Outlined.Timelapse,
        contentDescription = stringResource(StringsR.string.pref_seek_time),
      )
    },
    headlineContent = {
      Text(text = stringResource(StringsR.string.pref_seek_time))
    },
    supportingContent = {
      Text(
        text = LocalContext.current.resources.getQuantityString(
          StringsR.plurals.seconds,
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
    title = stringResource(StringsR.string.pref_seek_time),
    currentSeconds = currentSeconds,
    minSeconds = 3,
    maxSeconds = 60,
    textPluralRes = StringsR.plurals.seconds,
    onSecondsConfirm = onSecondsConfirm,
    onDismiss = onDismiss,
  )
}
