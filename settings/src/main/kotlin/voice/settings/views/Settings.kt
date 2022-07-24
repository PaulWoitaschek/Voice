package voice.settings.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.rootComponentAs
import voice.settings.R
import voice.settings.SettingsListener
import voice.settings.SettingsViewModel
import voice.settings.SettingsViewState

@Composable
@Preview
private fun SettingsPreview() {
  val viewState = SettingsViewState(
    useDarkTheme = false,
    showDarkThemePref = true,
    resumeOnReplug = true,
    seekTimeInSeconds = 42,
    autoRewindInSeconds = 12,
    dialog = null
  )
  VoiceTheme {
    Settings(
      viewState,
      object : SettingsListener {
        override fun close() {}
        override fun toggleResumeOnReplug() {}
        override fun toggleDarkTheme() {}
        override fun seekAmountChanged(seconds: Int) {}
        override fun onSeekAmountRowClicked() {}
        override fun autoRewindAmountChanged(seconds: Int) {}
        override fun onAutoRewindRowClicked() {}
        override fun onLikeClicked() {}
        override fun dismissDialog() {}
        override fun openSupport() {}
        override fun openTranslations() {}
      }
    )
  }
}

@Composable
private fun Settings(viewState: SettingsViewState, listener: SettingsListener) {
  Scaffold(
    topBar = {
      SmallTopAppBar(
        title = {
          Text(stringResource(R.string.action_settings))
        },
        actions = {
          IconButton(
            onClick = {
              listener.onLikeClicked()
            },
            content = {
              Icon(
                imageVector = Icons.Outlined.Favorite,
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
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(R.string.close)
            )
          }
        }
      )
    }
  ) { contentPadding ->
    Box(Modifier.padding(contentPadding)) {
      Column(Modifier.padding(vertical = 8.dp)) {
        if (viewState.showDarkThemePref) {
          DarkThemeRow(viewState.useDarkTheme, listener::toggleDarkTheme)
        }
        ResumeOnReplugRow(viewState.resumeOnReplug, listener::toggleResumeOnReplug)
        SeekTimeRow(viewState.seekTimeInSeconds) {
          listener.onSeekAmountRowClicked()
        }
        AutoRewindRow(viewState.autoRewindInSeconds) {
          listener.onAutoRewindRowClicked()
        }
        Dialog(viewState, listener)
      }
    }
  }
}

@ContributesTo(AppScope::class)
interface SettingsComponent {
  val settingsViewModel: SettingsViewModel
}

@Composable
fun Settings() {
  val viewModel = viewModel { rootComponentAs<SettingsComponent>().settingsViewModel }
  val viewState = remember(viewModel) { viewModel.viewState() }
    .collectAsState(SettingsViewState.Empty).value
  Settings(viewState, viewModel)
}

@Composable
private fun Dialog(
  viewState: SettingsViewState,
  listener: SettingsListener
) {
  val dialog = viewState.dialog ?: return
  when (dialog) {
    SettingsViewState.Dialog.Contribute -> {
      ContributeDialog(
        suggestionsClicked = { listener.openSupport() },
        translationsClicked = { listener.openTranslations() },
        onDismiss = { listener.dismissDialog() }
      )
    }
    SettingsViewState.Dialog.AutoRewindAmount -> {
      AutoRewindAmountDialog(
        currentSeconds = viewState.autoRewindInSeconds,
        onSecondsConfirmed = listener::autoRewindAmountChanged,
        onDismiss = listener::dismissDialog
      )
    }
    SettingsViewState.Dialog.SeekTime -> {
      SeekAmountDialog(
        currentSeconds = viewState.seekTimeInSeconds,
        onSecondsConfirmed = listener::seekAmountChanged,
        onDismiss = listener::dismissDialog
      )
    }
  }
}
