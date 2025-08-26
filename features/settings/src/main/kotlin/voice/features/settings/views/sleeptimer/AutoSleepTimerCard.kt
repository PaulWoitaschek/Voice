package voice.features.settings.views.sleeptimer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.common.compose.VoiceTheme
import voice.features.settings.SettingsListener
import voice.features.settings.SettingsViewState
import voice.core.strings.R as StringsR

@Composable
internal fun AutoSleepTimerCard(
  viewState: SettingsViewState.AutoSleepTimerViewState,
  listener: SettingsListener,
) {
  OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
      AutoSleepTimerRow(
        autoSleepTimer = viewState.enabled,
        start = viewState.startTime,
        end = viewState.endTime,
        toggleAutoSleepTimer = listener::setAutoSleepTimer,
      )
      Row(Modifier.padding(start = 36.dp)) {
        AutoSleepTimerSetting(
          time = viewState.startTime,
          label = stringResource(StringsR.string.auto_sleep_timer_start),
          enabled = viewState.enabled,
          setAutoSleepTime = listener::setAutoSleepTimerStart,
        )
        AutoSleepTimerSetting(
          time = viewState.endTime,
          label = stringResource(StringsR.string.auto_sleep_timer_end),
          enabled = viewState.enabled,
          setAutoSleepTime = listener::setAutoSleepTimerEnd,
        )
      }
    }
  }
}

@Composable
@Preview
private fun AutoSleepTimerCardPreview() {
  VoiceTheme {
    AutoSleepTimerCard(SettingsViewState.AutoSleepTimerViewState.preview(), SettingsListener.noop())
  }
}
