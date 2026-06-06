package voice.features.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.VoiceTheme
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@Composable
@Preview
private fun SupportPreview() {
  VoiceTheme {
    Support(
      viewState = SupportViewState.preview(),
      listener = SupportListener.noop(),
    )
  }
}

@Composable
private fun Support(
  viewState: SupportViewState,
  listener: SupportListener,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(stringResource(StringsR.string.support_title))
        },
        navigationIcon = {
          IconButton(
            onClick = listener::close,
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(StringsR.string.support_description_maintenance),
        style = MaterialTheme.typography.bodyLarge,
      )
      Text(
        text = stringResource(StringsR.string.support_description_no_benefits),
        style = MaterialTheme.typography.bodyMedium,
      )
      when (viewState.backendState) {
        is SupportBackendState.Free -> {
          FreeSupportContent(
            supporterBadgeVisible = viewState.backendState.supporterBadgeVisible,
            listener = listener,
          )
        }
        SupportBackendState.PlayUnavailable -> {
          Text(
            text = stringResource(StringsR.string.support_play_unavailable_message),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
  }
}

@Composable
private fun FreeSupportContent(
  supporterBadgeVisible: Boolean,
  listener: SupportListener,
) {
  Column {
    if (supporterBadgeVisible) {
      Text(
        text = stringResource(StringsR.string.support_thank_you),
        style = MaterialTheme.typography.bodyMedium,
      )
      SupporterBadge()
    }

    Button(
      onClick = listener::openSupport,
    ) {
      Icon(
        imageVector = Icons.Outlined.FavoriteBorder,
        contentDescription = null,
      )
      Text(stringResource(StringsR.string.support_action_open_kofi))
    }

    if (supporterBadgeVisible) {
      TextButton(
        onClick = {
          listener.setSupporterBadgeVisible(false)
        },
      ) {
        Text(stringResource(StringsR.string.support_action_hide_badge))
      }
    } else {
      OutlinedButton(
        onClick = {
          listener.setSupporterBadgeVisible(true)
        },
      ) {
        Icon(
          imageVector = Icons.Outlined.CheckCircle,
          contentDescription = null,
        )
        Text(stringResource(StringsR.string.support_action_i_supported_kofi))
      }
      Text(
        text = stringResource(StringsR.string.support_description_local_badge),
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun SupporterBadge() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AssistChip(
      onClick = {},
      label = {
        Text(stringResource(StringsR.string.support_badge_label))
      },
    )
  }
}

@ContributesTo(AppScope::class)
interface SupportGraph {
  val supportViewModel: SupportViewModel
}

@ContributesTo(AppScope::class)
interface SupportProvider {

  @Provides
  @IntoSet
  fun supportNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.SupportVoice> { key ->
    NavEntry(key) {
      Support()
    }
  }
}

@Composable
fun Support() {
  val viewModel = retain<SupportViewModel> { rootGraphAs<SupportGraph>().supportViewModel }
  Support(viewModel.viewState(), viewModel)
}
