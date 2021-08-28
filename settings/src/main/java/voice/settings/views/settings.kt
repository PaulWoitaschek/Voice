package voice.settings.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.settings.R
import voice.settings.SettingsViewModel
import voice.settings.SettingsViewState

@Composable
internal fun Settings(viewModel: SettingsViewModel) {
  val viewState by viewModel.viewState().collectAsState(SettingsViewState.Empty)
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(stringResource(R.string.action_settings))
        },
        actions = {
          IconButton(
            onClick = {
              viewModel.onLikeClicked()
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
              viewModel.close()
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
        DarkThemeRow(viewState.useDarkTheme, viewModel::toggleDarkTheme)
      }
      ResumeOnReplugRow(viewState.resumeOnReplug, viewModel::toggleResumeOnReplug)
      SeekTimeRow(viewState.seekTimeInSeconds) {
        viewModel.onSeekAmountRowClicked()
      }
      AutoRewindRow(viewState.autoRewindInSeconds) {
        viewModel.onAutoRewindRowClicked()
      }
      Dialog(viewState, viewModel)
    }
  }
}

@Composable
private fun Dialog(
  viewState: SettingsViewState,
  viewModel: SettingsViewModel
) {
  val dialog = viewState.dialog ?: return
  when (dialog) {
    SettingsViewState.Dialog.Contribute -> {
      ContributeDialog(
        suggestionsClicked = { viewModel.openSupport() },
        translationsClicked = { viewModel.openTranslations() },
        onDismiss = { viewModel.dismissDialog() }
      )
    }
    SettingsViewState.Dialog.AutoRewindAmount -> {
      AutoRewindAmountDialog(
        currentSeconds = viewState.autoRewindInSeconds,
        onSecondsConfirmed = viewModel::autoRewindAmountChanged,
        onDismiss = viewModel::dismissDialog
      )
    }
    SettingsViewState.Dialog.SeekTime -> {
      SeekAmountDialog(
        currentSeconds = viewState.seekTimeInSeconds,
        onSecondsConfirmed = viewModel::seekAmountChanged,
        onDismiss = viewModel::dismissDialog
      )
    }
  }
}
