package voice.features.settings.developer

import android.content.ClipData
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.ui.CenteredContent
import voice.core.ui.icons.VoiceIcons
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@Composable
private fun DeveloperSettings(
  viewState: DeveloperSettingsViewState,
  viewModel: DeveloperSettingsViewModel,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
          Text("Developer Menu")
        },
        navigationIcon = {
          IconButton(onClick = viewModel::close) {
            Icon(
              imageVector = VoiceIcons.Close,
              contentDescription = stringResource(StringsR.string.common_action_close),
            )
          }
        },
      )
    },
  ) { contentPadding ->
    val layoutDirection = LocalLayoutDirection.current
    CenteredContent(
      modifier = Modifier.padding(
        start = contentPadding.calculateStartPadding(layoutDirection),
        end = contentPadding.calculateEndPadding(layoutDirection),
      ),
    ) { horizontalPadding ->
      // the rows bring their own 16dp
      val listPadding = horizontalPadding - 16.dp
      LazyColumn(
        contentPadding = PaddingValues(
          start = listPadding,
          top = contentPadding.calculateTopPadding(),
          end = listPadding,
          bottom = contentPadding.calculateBottomPadding(),
        ),
      ) {
        item {
          ListItem(
            trailingContent = {
              TextButton(
                onClick = {
                  viewModel.refreshRemoteConfig()
                },
                content = {
                  Text("Refresh")
                },
              )
            },
          ) {
            Text("Refresh FCM")
          }
        }

        item {
          ListItem(
            trailingContent = {
              TextButton(
                onClick = viewModel::showOnboarding,
                content = {
                  Text("Show")
                },
              )
            },
          ) {
            Text("Onboarding")
          }
        }

        val fcmToken = viewState.fcmToken
        if (fcmToken != null) {
          item {
            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()
            ListItem(
              trailingContent = {
                TextButton(
                  onClick = {
                    scope.launch {
                      clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("FCM Token", fcmToken)),
                      )
                    }
                  },
                  content = {
                    Text("Copy to Clipboard")
                  },
                )
              },
            ) {
              Text("FCM Token")
            }
          }
        }

        items(viewState.featureFlags, key = { it.key }) { viewState ->
          when (viewState) {
            is DeveloperSettingsViewState.FeatureFlagViewState.BooleanFlag -> {
              BooleanFeatureFlagRow(
                viewState = viewState,
                clearOverride = {
                  viewModel.clearOverride(viewState.key)
                },
                setOverride = { viewModel.setBooleanOverride(viewState.key, it) },
              )
            }
            is DeveloperSettingsViewState.FeatureFlagViewState.StringFlag -> {
              StringFeatureFlagRow(
                viewState = viewState,
                clearOverride = {
                  viewModel.clearOverride(viewState.key)
                },
                setOverride = { viewModel.setStringOverride(viewState.key, it) },
              )
            }
            is DeveloperSettingsViewState.FeatureFlagViewState.EnumFlag -> {
              EnumFeatureFlagRow(
                viewState = viewState,
                clearOverride = {
                  viewModel.clearOverride(viewState.key)
                },
                setOverride = { viewModel.setEnumOverride(viewState.key, it) },
              )
            }
          }
        }
      }
    }
  }
}

@ContributesTo(AppScope::class)
interface DeveloperSettingsGraph {
  val developerSettingsViewModel: DeveloperSettingsViewModel
}

@BindingContainer
@ContributesTo(AppScope::class)
object DeveloperSettingsProvider {

  @Provides
  @IntoSet
  fun navEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.DeveloperSettings> { key ->
    NavEntry(key) {
      DeveloperSettings()
    }
  }
}

@Composable
fun DeveloperSettings() {
  val viewModel = retain<DeveloperSettingsViewModel> { rootGraphAs<DeveloperSettingsGraph>().developerSettingsViewModel }
  val viewState = viewModel.viewState()
  DeveloperSettings(viewState, viewModel)
}
