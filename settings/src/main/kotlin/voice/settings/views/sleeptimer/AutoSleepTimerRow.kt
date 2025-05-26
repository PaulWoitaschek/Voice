package voice.settings.views.sleeptimer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceTheme
import java.time.LocalTime
import voice.strings.R as StringsR

@Composable
internal fun AutoSleepTimerRow(
  autoSleepTimer: Boolean,
  start: LocalTime,
  end: LocalTime,
  toggleAutoSleepTimer: (Boolean) -> Unit,
) {
  Row(Modifier.padding(top = 8.dp)) {
    Icon(
      modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
      imageVector = Icons.Outlined.Bedtime,
      contentDescription = stringResource(StringsR.string.auto_sleep_timer_subtitle),
    )
    Column(
      Modifier
        .weight(1f)
        .padding(start = 8.dp, end = 8.dp),
    ) {
      Text(
        text = stringResource(id = StringsR.string.auto_sleep_timer_title),
        style = MaterialTheme.typography.bodyLarge,
      )
      val localTimeFormatter = rememberLocalTimeFormatter()
      Text(
        text = stringResource(
          id = StringsR.string.auto_sleep_timer_subtitle,
          localTimeFormatter.format(start),
          localTimeFormatter.format(end),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = autoSleepTimer,
      onCheckedChange = toggleAutoSleepTimer,
    )
  }
}

@Composable
@Preview
private fun AutoSleepTimerRowPreview() {
  VoiceTheme {
    AutoSleepTimerRow(
      autoSleepTimer = true,
      start = LocalTime.of(22, 0),
      end = LocalTime.of(6, 0),
      toggleAutoSleepTimer = {},
    )
  }
}
