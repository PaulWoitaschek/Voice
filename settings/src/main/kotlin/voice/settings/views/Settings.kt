package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.settings.R
import voice.settings.SettingsListener
import voice.settings.SettingsViewModel
import voice.settings.SettingsViewState

@Composable
@Preview
fun SettingsPreview() {
  val viewState = SettingsViewState(
    useDarkTheme = false,
    showDarkThemePref = true,
    seekTimeInSeconds = 42,
    autoRewindInSeconds = 12,
    dialog = null,
    appVersion = "1.2.3",
    useGrid = true,
  )
  VoiceTheme {
    Settings(
      viewState,
      object : SettingsListener {
        override fun close() {}
        override fun toggleDarkTheme() {}
        override fun seekAmountChanged(seconds: Int) {}
        override fun onSeekAmountRowClicked() {}
        override fun autoRewindAmountChanged(seconds: Int) {}
        override fun onAutoRewindRowClicked() {}
        override fun dismissDialog() {}
        override fun openTranslations() {}
        override fun getSupport() {}
        override fun suggestIdea() {}
        override fun openBugReport() {}
        override fun toggleGrid() {}
      },
    )
  }
}

@Composable
private fun Settings(viewState: SettingsViewState, listener: SettingsListener) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
          Text(stringResource(R.string.action_settings))
        },
        navigationIcon = {
          IconButton(
            onClick = {
              listener.close()
            },
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(R.string.close),
            )
          }
        },
      )
    },
  ) { contentPadding ->
    Box(Modifier.padding(contentPadding)) {
      Column(Modifier.padding(vertical = 8.dp)) {
        if (viewState.showDarkThemePref) {
          DarkThemeRow(viewState.useDarkTheme, listener::toggleDarkTheme)
        }
        ListItem(
          modifier = Modifier.clickable { listener.toggleGrid() },
          leadingContent = {
            val imageVector = if (viewState.useGrid) {
              Icons.Outlined.GridView
            } else {
              Icons.Outlined.ViewList
            }
            Icon(imageVector, stringResource(R.string.pref_use_grid))
          },
          headlineText = { Text(stringResource(R.string.pref_use_grid)) },
          trailingContent = {
            Switch(
              checked = viewState.useGrid,
              onCheckedChange = {
                listener.toggleGrid()
              },
            )
          },
        )
        SeekTimeRow(viewState.seekTimeInSeconds) {
          listener.onSeekAmountRowClicked()
        }
        AutoRewindRow(viewState.autoRewindInSeconds) {
          listener.onAutoRewindRowClicked()
        }
        ListItem(
          modifier = Modifier.clickable { listener.suggestIdea() },
          leadingContent = { Icon(Icons.Outlined.Lightbulb, stringResource(R.string.pref_suggest_idea)) },
          headlineText = { Text(stringResource(R.string.pref_suggest_idea)) },
        )
        ListItem(
          modifier = Modifier.clickable { listener.getSupport() },
          leadingContent = { Icon(Icons.Outlined.HelpOutline, stringResource(R.string.pref_get_support)) },
          headlineText = { Text(stringResource(R.string.pref_get_support)) },
        )
        ListItem(
          modifier = Modifier.clickable { listener.openBugReport() },
          leadingContent = { Icon(Icons.Outlined.BugReport, stringResource(R.string.pref_report_issue)) },
          headlineText = { Text(stringResource(R.string.pref_report_issue)) },
        )
        ListItem(
          modifier = Modifier.clickable { listener.openTranslations() },
          leadingContent = { Icon(Icons.Outlined.Language, stringResource(R.string.pref_help_translating)) },
          headlineText = { Text(stringResource(R.string.pref_help_translating)) },
        )
        AppVersion(appVersion = viewState.appVersion)
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
  val viewModel = rememberScoped { rootComponentAs<SettingsComponent>().settingsViewModel }
  val viewState = viewModel.viewState()
  Settings(viewState, viewModel)
}

@Composable
private fun Dialog(
  viewState: SettingsViewState,
  listener: SettingsListener,
) {
  val dialog = viewState.dialog ?: return
  when (dialog) {
    SettingsViewState.Dialog.AutoRewindAmount -> {
      AutoRewindAmountDialog(
        currentSeconds = viewState.autoRewindInSeconds,
        onSecondsConfirmed = listener::autoRewindAmountChanged,
        onDismiss = listener::dismissDialog,
      )
    }
    SettingsViewState.Dialog.SeekTime -> {
      SeekAmountDialog(
        currentSeconds = viewState.seekTimeInSeconds,
        onSecondsConfirmed = listener::seekAmountChanged,
        onDismiss = listener::dismissDialog,
      )
    }
  }
}
