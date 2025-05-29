package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
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
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.settings.SettingsListener
import voice.settings.SettingsViewModel
import voice.settings.SettingsViewState
import voice.settings.views.sleeptimer.AutoSleepTimerCard
import voice.strings.R as StringsR

@Composable
@Preview
private fun SettingsPreview() {
  VoiceTheme {
    Settings(
      SettingsViewState.preview(),
      SettingsListener.noop(),
    )
  }
}

@Composable
private fun Settings(
  viewState: SettingsViewState,
  listener: SettingsListener,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
          Text(stringResource(StringsR.string.action_settings))
        },
        navigationIcon = {
          IconButton(
            onClick = {
              listener.close()
            },
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(StringsR.string.close),
            )
          }
        },
      )
    },
  ) { contentPadding ->
    LazyColumn(contentPadding = contentPadding) {
      if (viewState.showDarkThemePref) {
        item {
          DarkThemeRow(viewState.useDarkTheme, listener::toggleDarkTheme)
        }
      }
      item {
        ListItem(
          modifier = Modifier.clickable { listener.toggleGrid() },
          leadingContent = {
            val imageVector = if (viewState.useGrid) {
              Icons.Outlined.GridView
            } else {
              Icons.AutoMirrored.Outlined.ViewList
            }
            Icon(imageVector, stringResource(StringsR.string.pref_use_grid))
          },
          headlineContent = { Text(stringResource(StringsR.string.pref_use_grid)) },
          trailingContent = {
            Switch(
              checked = viewState.useGrid,
              onCheckedChange = {
                listener.toggleGrid()
              },
            )
          },
        )
      }

      item {
        SeekTimeRow(viewState.seekTimeInSeconds) {
          listener.onSeekAmountRowClick()
        }
      }

      item {
        AutoRewindRow(viewState.autoRewindInSeconds) {
          listener.onAutoRewindRowClick()
        }
      }

      item {
        AutoSleepTimerCard(viewState.autoSleepTimer, listener)
      }

      item {
        ListItem(
          modifier = Modifier.clickable { listener.suggestIdea() },
          leadingContent = { Icon(Icons.Outlined.Lightbulb, stringResource(StringsR.string.pref_suggest_idea)) },
          headlineContent = { Text(stringResource(StringsR.string.pref_suggest_idea)) },
        )
      }

      item {
        ListItem(
          modifier = Modifier.clickable { listener.getSupport() },
          leadingContent = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, stringResource(StringsR.string.pref_get_support)) },
          headlineContent = { Text(stringResource(StringsR.string.pref_get_support)) },
        )
      }

      item {
        ListItem(
          modifier = Modifier.clickable { listener.openBugReport() },
          leadingContent = { Icon(Icons.Outlined.BugReport, stringResource(StringsR.string.pref_report_issue)) },
          headlineContent = { Text(stringResource(StringsR.string.pref_report_issue)) },
        )
      }
      item {
        ListItem(
          modifier = Modifier.clickable { listener.openTranslations() },
          leadingContent = { Icon(Icons.Outlined.Language, stringResource(StringsR.string.pref_help_translating)) },
          headlineContent = { Text(stringResource(StringsR.string.pref_help_translating)) },
        )
      }
      item {
        AppVersion(appVersion = viewState.appVersion)
      }
    }
    Dialog(viewState, listener)
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
        onSecondsConfirm = listener::autoRewindAmountChang,
        onDismiss = listener::dismissDialog,
      )
    }
    SettingsViewState.Dialog.SeekTime -> {
      SeekAmountDialog(
        currentSeconds = viewState.seekTimeInSeconds,
        onSecondsConfirm = listener::seekAmountChanged,
        onDismiss = listener::dismissDialog,
      )
    }
  }
}
