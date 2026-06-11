package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.VoiceTheme
import voice.features.settings.SettingsListener
import voice.features.settings.SettingsViewEffect
import voice.features.settings.SettingsViewModel
import voice.features.settings.SettingsViewState
import voice.features.settings.views.sleeptimer.AutoSleepTimerCard
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

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
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    topBar = {
      TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
          Text(stringResource(StringsR.string.settings_action_open))
        },
        navigationIcon = {
          IconButton(
            onClick = {
              listener.close()
            },
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(StringsR.string.common_action_close),
            )
          }
        },
      )
    },
  ) { contentPadding ->
    LazyColumn(contentPadding = contentPadding) {
      if (viewState.showDeveloperMenu && !viewState.kioskMode) {
        item {
          DeveloperMenuItem(
            onClick = listener::openDeveloperMenu,
          )
        }
      }
      if (viewState.showFolderPickerEntry) {
        item {
          ListItem(
            modifier = Modifier.clickable { listener.openFolderPicker() },
            leadingContent = {
              Icon(
                imageVector = Icons.Outlined.Book,
                contentDescription = stringResource(StringsR.string.library_folders_title),
              )
            },
            headlineContent = {
              Text(stringResource(StringsR.string.library_folders_title))
            },
            supportingContent = {
              Text(stringResource(StringsR.string.settings_library_folders_summary))
            },
          )
        }
      }
      if (viewState.showDarkThemePref) {
        item {
          DarkThemeRow(viewState.useDarkTheme, listener::toggleDarkTheme)
        }
      }
      if (viewState.showAnalyticSetting && !viewState.kioskMode) {
        item {
          AnalyticsRow(analyticsEnabled = viewState.analyticsEnabled, toggle = listener::toggleAnalytics)
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
            Icon(imageVector, stringResource(StringsR.string.settings_library_use_grid_title))
          },
          headlineContent = { Text(stringResource(StringsR.string.settings_library_use_grid_title)) },
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
          leadingContent = {
            Icon(
              imageVector = Icons.Outlined.Lightbulb,
              contentDescription = stringResource(StringsR.string.settings_support_suggest_idea_title),
            )
          },
          headlineContent = {
            Text(stringResource(StringsR.string.settings_support_suggest_idea_title))
          },
        )
      }

      item {
        ListItem(
          modifier = Modifier.clickable { listener.getSupport() },
          leadingContent = {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
              contentDescription = stringResource(StringsR.string.settings_support_get_support_title),
            )
          },
          headlineContent = {
            Text(stringResource(StringsR.string.settings_support_get_support_title))
          },
        )
      }

      item {
        ListItem(
          modifier = Modifier.clickable { listener.openBugReport() },
          leadingContent = {
            Icon(
              imageVector = Icons.Outlined.BugReport,
              contentDescription = stringResource(StringsR.string.settings_support_report_issue_title),
            )
          },
          headlineContent = {
            Text(stringResource(StringsR.string.settings_support_report_issue_title))
          },
        )
      }
      item {
        ListItem(
          modifier = Modifier.clickable { listener.openTranslations() },
          leadingContent = {
            Icon(
              imageVector = Icons.Outlined.Language,
              contentDescription = stringResource(StringsR.string.settings_support_help_translating_title),
            )
          },
          headlineContent = {
            Text(stringResource(StringsR.string.settings_support_help_translating_title))
          },
        )
      }
      item {
        ListItem(
          modifier = Modifier.clickable { listener.openFaq() },
          leadingContent = {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
              contentDescription = stringResource(StringsR.string.settings_support_faq_title),
            )
          },
          headlineContent = {
            Text(stringResource(StringsR.string.settings_support_faq_title))
          },
        )
      }
      if (viewState.showSupportDevelopment) {
        item {
          ListItem(
            modifier = Modifier.clickable { listener.openSupportVoice() },
            leadingContent = {
              Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = stringResource(StringsR.string.settings_support_support_voice_title),
                tint = MaterialTheme.colorScheme.primary,
              )
            },
            headlineContent = {
              Text(stringResource(StringsR.string.settings_support_support_voice_title))
            },
            supportingContent = {
              Text(stringResource(StringsR.string.settings_support_support_voice_summary))
            },
          )
        }
      }
      item {
        AppVersion(
          appVersion = viewState.appVersion,
          onClick = listener::onAppVersionClick,
        )
      }
      if (viewState.kioskMode) {
        if (viewState.showAnalyticSetting) {
          item {
            AnalyticsRow(analyticsEnabled = viewState.analyticsEnabled, toggle = listener::toggleAnalytics)
          }
        }
        if (viewState.showDeveloperMenu) {
          item {
            DeveloperMenuItem(
              onClick = listener::openDeveloperMenu,
            )
          }
        }
      }
    }
    Dialog(viewState, listener)
  }
}

@Composable
private fun AnalyticsRow(
  analyticsEnabled: Boolean,
  toggle: () -> Unit,
) {
  ListItem(
    modifier = Modifier.clickable { toggle() },
    leadingContent = {
      Icon(
        imageVector = Icons.Outlined.Analytics,
        contentDescription = null,
      )
    },
    headlineContent = {
      Text(text = stringResource(StringsR.string.settings_analytics_consent_title))
    },
    supportingContent = {
      Text(text = stringResource(StringsR.string.settings_analytics_consent_description))
    },
    trailingContent = {
      Switch(
        checked = analyticsEnabled,
        onCheckedChange = { toggle() },
      )
    },
  )
}

@ContributesTo(AppScope::class)
interface SettingsGraph {
  val settingsViewModel: SettingsViewModel
}

@ContributesTo(AppScope::class)
interface SettingsProvider {

  @Provides
  @IntoSet
  fun settingsNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Settings> { key ->
    NavEntry(key) {
      Settings()
    }
  }
}

@Composable
fun Settings() {
  val viewModel = retain<SettingsViewModel> { rootGraphAs<SettingsGraph>().settingsViewModel }
  val snackbarHostState = remember { SnackbarHostState() }
  val viewState = viewModel.viewState()
  val currentDeveloperMenuUnlockedMessage = rememberUpdatedState("Developer Menu unlocked")
  LaunchedEffect(viewModel) {
    viewModel.viewEffects.collect { viewEffect ->
      when (viewEffect) {
        SettingsViewEffect.DeveloperMenuUnlocked -> {
          snackbarHostState.showSnackbar(currentDeveloperMenuUnlockedMessage.value)
        }
      }
    }
  }
  Settings(viewState, viewModel, snackbarHostState)
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
