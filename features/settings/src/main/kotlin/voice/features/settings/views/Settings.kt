@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.AuroraBackground
import voice.core.ui.ConfettiState
import voice.core.ui.EntranceState
import voice.core.ui.VoiceTheme
import voice.core.ui.entrance
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.plus
import voice.core.ui.rememberAnimationClock
import voice.core.ui.rememberConfettiState
import voice.core.ui.rememberEntranceState
import voice.features.settings.SettingsListener
import voice.features.settings.SettingsViewEffect
import voice.features.settings.SettingsViewModel
import voice.features.settings.SettingsViewState
import voice.features.settings.views.sleeptimer.AutoSleepTimerSection
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

/**
 * The settings, as colorful islands floating over the drifting aurora. They float in one after
 * another when the screen opens.
 */
@Composable
private fun Settings(
  viewState: SettingsViewState?,
  listener: SettingsListener,
  confetti: ConfettiState = rememberConfettiState(),
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val clock = rememberAnimationClock(running = true)
  var entered by rememberSaveable { mutableStateOf(false) }
  // the sections float in once the settings are read
  val entrance = if (viewState != null) rememberEntranceState(animate = !entered) else null
  LaunchedEffect(entrance) { if (entrance != null) entered = true }
  Box(Modifier.fillMaxSize()) {
    AuroraBackground(
      clock = { clock.value },
      showStars = false,
      modifier = Modifier.fillMaxSize(),
    )
    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState)
      },
      topBar = {
        SettingsTopBar(
          scrollBehavior = scrollBehavior,
          onClose = listener::close,
        )
      },
    ) { contentPadding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (viewState != null && entrance != null) {
          sections(viewState, listener, confetti, entrance)
        }
      }
    }
  }
}

private fun LazyListScope.sections(
  viewState: SettingsViewState,
  listener: SettingsListener,
  confetti: ConfettiState,
  entrance: EntranceState,
) {
  var index = 0
  fun section(
    key: String,
    content: @Composable (Modifier) -> Unit,
  ) {
    // the entrance is over by the time later sections are scrolled to, so they share the last delay
    val entranceIndex = minOf(index++, 6)
    item(key = key) {
      content(
        Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .entrance(entrance, entranceIndex),
      )
    }
  }
  section("library") { modifier ->
    LibrarySection(
      modifier = modifier,
      folderNames = viewState.folderNames,
      useGrid = viewState.useGrid,
      onFoldersClick = listener::openFolderPicker,
      onUseGridChange = listener::setUseGrid,
    )
  }
  if (viewState.showSupportDevelopment) {
    section("support") { modifier ->
      SupportCard(
        modifier = modifier,
        badge = viewState.supporterBadge,
        supporterSince = viewState.supporterSince,
        onClick = listener::openSupportVoice,
      )
    }
  }
  section("appearance") { modifier ->
    AppearanceSection(
      modifier = modifier,
      themeMode = viewState.themeMode,
      themeColorScheme = viewState.themeColorScheme,
      dynamicColorAvailable = viewState.dynamicColorAvailable,
      onThemeModeSelect = listener::setThemeMode,
      onThemeColorSchemeSelect = listener::setThemeColorScheme,
    )
  }
  section("listening") { modifier ->
    ListeningSection(
      modifier = modifier,
      seekTimeInSeconds = viewState.seekTimeInSeconds,
      autoRewindInSeconds = viewState.autoRewindInSeconds,
      onSeekTimeChange = listener::seekAmountChanged,
      onSeekTimeStep = listener::seekAmountStepped,
      onAutoRewindChange = listener::autoRewindAmountChanged,
      onAutoRewindStep = listener::autoRewindAmountStepped,
    )
  }
  section("sleepTimer") { modifier ->
    AutoSleepTimerSection(
      modifier = modifier,
      viewState = viewState.autoSleepTimer,
      themeColorScheme = viewState.themeColorScheme,
      onEnabledChange = listener::setAutoSleepTimer,
      onStartChange = listener::setAutoSleepTimerStart,
      onEndChange = listener::setAutoSleepTimerEnd,
    )
  }
  section("help") { modifier ->
    HelpSection(
      modifier = modifier,
      showAnalytics = viewState.showAnalyticSetting,
      analyticsEnabled = viewState.analyticsEnabled,
      onFaqClick = listener::openFaq,
      onGetHelpClick = listener::getSupport,
      onReportClick = listener::openBugReport,
      onSuggestClick = listener::suggestIdea,
      onAnalyticsToggle = listener::toggleAnalytics,
    )
  }
  section("about") { modifier ->
    AboutFooter(
      modifier = modifier,
      appVersion = viewState.appVersion,
      confetti = confetti,
      onClick = listener::onAppVersionClick,
    )
  }
  if (viewState.showDeveloperMenu) {
    section("developerMenu") { modifier ->
      DeveloperMenuItem(
        modifier = modifier,
        onClick = listener::openDeveloperMenu,
      )
    }
  }
}

@Composable
private fun SettingsTopBar(
  scrollBehavior: TopAppBarScrollBehavior,
  onClose: () -> Unit,
) {
  LargeFlexibleTopAppBar(
    title = {
      Text(stringResource(StringsR.string.settings_action_open))
    },
    subtitle = {
      Text(stringResource(StringsR.string.settings_subtitle))
    },
    navigationIcon = {
      val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
      IconButton(onClick = onClose) {
        Icon(
          modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1F else 1F },
          imageVector = VoiceIcons.ArrowBack,
          contentDescription = stringResource(StringsR.string.common_action_close),
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = Color.Transparent,
      scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    scrollBehavior = scrollBehavior,
  )
}

@ContributesTo(AppScope::class)
interface SettingsGraph {
  val settingsViewModel: SettingsViewModel
}

@BindingContainer
@ContributesTo(AppScope::class)
object SettingsProvider {

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
  val confetti = rememberConfettiState()
  val viewState = viewModel.viewState()
  val currentDeveloperMenuUnlockedMessage = rememberUpdatedState("Developer Menu unlocked")
  LaunchedEffect(viewModel) {
    viewModel.viewEffects.collect { viewEffect ->
      when (viewEffect) {
        SettingsViewEffect.DeveloperMenuUnlocked -> {
          confetti.burst()
          snackbarHostState.showSnackbar(currentDeveloperMenuUnlockedMessage.value)
        }
      }
    }
  }
  Settings(viewState, viewModel, confetti, snackbarHostState)
}
