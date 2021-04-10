package voice.settings.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.settings.R
import voice.settings.SettingsViewListener
import voice.settings.SettingsViewState

@Composable
internal fun Settings(viewState: SettingsViewState, listener: SettingsViewListener) {
  val showContributeDialog = remember { mutableStateOf(false) }

  MaterialTheme {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.action_settings))
          },
          actions = {
            IconButton(
              onClick = {
                showContributeDialog.value = true
              },
              content = {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = stringResource(R.string.pref_support_title)
                )
              }
            )
          },
          navigationIcon = {
            IconButton(
              onClick = {
                listener.close()
              }
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close)
              )
            }
          }
        )
      }
    ) {
      Column(Modifier.padding(vertical = 8.dp)) {
        if (viewState.showDarkThemePref) {
          DarkThemeRow(viewState.useDarkTheme, listener::toggleDarkTheme)
        }
        ResumeOnReplugRow(viewState.resumeOnReplug, listener::toggleResumeOnReplug)
        val showSeekTimeDialog = remember { mutableStateOf(false) }
        val showAutoRewindDialog = remember { mutableStateOf(false) }
        SeekTimeRow(viewState.seekTimeInSeconds) {
          showSeekTimeDialog.value = true
        }
        AutoRewindRow(viewState.autoRewindInSeconds) {
          showAutoRewindDialog.value = true
        }
        SeekAmountDialog(showSeekTimeDialog, viewState.seekTimeInSeconds, listener::seekAmountChanged)
        AutoRewindAmountDialog(showAutoRewindDialog, viewState.autoRewindInSeconds, listener::autoRewindAmountChanged)
        ContributeDialog(
          showContributeDialog,
          suggestionsClicked = { listener.openSupport() },
          translationsClicked = { listener.openTranslations() }
        )
      }
    }
  }
}
