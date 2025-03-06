package voice.settings.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.strings.R as StringsR

@Composable
internal fun AutoSleepTimerRow(
  autoSleepTimer: Boolean,
  toggleAutoSleepTimer: (Boolean) -> Unit,
) {
  ListItem(
    leadingContent = {
      Icon(
        imageVector = Icons.Outlined.Bedtime,
        contentDescription = stringResource(StringsR.string.auto_sleep_timer),
      )
    },
    headlineContent = {
      Text(text = stringResource(id = StringsR.string.auto_sleep_timer))
    },
    trailingContent = {
      Switch(
        checked = autoSleepTimer,
        onCheckedChange = toggleAutoSleepTimer,
      )
    },
  )
}
