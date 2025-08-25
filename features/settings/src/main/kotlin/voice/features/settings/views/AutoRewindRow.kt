package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import voice.core.strings.R as StringsR

@Composable
internal fun AutoRewindRow(
  autoRewindInSeconds: Int,
  openAutoRewindDialog: () -> Unit,
) {
  ListItem(
    modifier = Modifier
      .clickable {
        openAutoRewindDialog()
      }
      .fillMaxWidth(),
    leadingContent = {
      Icon(
        imageVector = Icons.Outlined.FastRewind,
        contentDescription = stringResource(StringsR.string.pref_auto_rewind_title),
      )
    },
    headlineContent = {
      Text(text = stringResource(StringsR.string.pref_auto_rewind_title))
    },
    supportingContent = {
      Text(
        text = LocalResources.current.getQuantityString(
          StringsR.plurals.seconds,
          autoRewindInSeconds,
          autoRewindInSeconds,
        ),
      )
    },
  )
}

@Composable
internal fun AutoRewindAmountDialog(
  currentSeconds: Int,
  onSecondsConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  TimeSettingDialog(
    title = stringResource(StringsR.string.pref_auto_rewind_title),
    currentSeconds = currentSeconds,
    minSeconds = 0,
    maxSeconds = 20,
    textPluralRes = StringsR.plurals.seconds,
    onSecondsConfirm = onSecondsConfirm,
    onDismiss = onDismiss,
  )
}
