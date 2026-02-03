package voice.features.settings.developer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.rememberScoped
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
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(StringsR.string.close),
            )
          }
        },
      )
    },
  ) { contentPadding ->
    LazyColumn(contentPadding = contentPadding) {
      val fcmToken = viewState.fcmToken
      if (fcmToken != null) {
        item {
          ListItem(
            headlineContent = {
              Text("FCM Token")
            },
            supportingContent = {
              Text(fcmToken)
            },
          )
        }
      }
    }
  }
}

@ContributesTo(AppScope::class)
interface DeveloperSettingsGraph {
  val developerSettingsViewModel: DeveloperSettingsViewModel
}

@ContributesTo(AppScope::class)
interface DeveloperSettingsProvider {

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
  val viewModel = rememberScoped { rootGraphAs<DeveloperSettingsGraph>().developerSettingsViewModel }
  val viewState = viewModel.viewState()
  DeveloperSettings(viewState, viewModel)
}
