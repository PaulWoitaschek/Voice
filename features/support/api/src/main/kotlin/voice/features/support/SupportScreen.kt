package voice.features.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
      when (viewState.backendState) {
        is SupportBackendState.Free -> {
          FreeSupportContent(
            supporterBadgeVisible = viewState.backendState.supporterBadgeVisible,
            listener = listener,
          )
        }
        SupportBackendState.PlayUnavailable -> {
          Text(
            text = stringResource(StringsR.string.support_description_maintenance),
            style = MaterialTheme.typography.bodyLarge,
          )
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
  if (supporterBadgeVisible) {
    SupportedContent(listener)
  } else {
    DonationContent(listener)
  }
}

@Composable
private fun DonationContent(listener: SupportListener) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = stringResource(StringsR.string.support_description_maintenance),
      style = MaterialTheme.typography.bodyLarge,
    )

    OutlinedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
    ) {
      Text(
        modifier = Modifier.padding(16.dp),
        text = stringResource(StringsR.string.support_description_no_benefits),
        style = MaterialTheme.typography.bodyMedium,
      )
    }

    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = listener::openSupport,
    ) {
      Icon(
        imageVector = Icons.Outlined.FavoriteBorder,
        contentDescription = null,
      )
      Text(stringResource(StringsR.string.support_action_donate_kofi))
    }

    Text(
      text = stringResource(StringsR.string.support_already_donated),
      style = MaterialTheme.typography.titleSmall,
    )

    OutlinedButton(
      modifier = Modifier.fillMaxWidth(),
      onClick = {
        listener.setSupporterBadgeVisible(true)
      },
    ) {
      Text(stringResource(StringsR.string.support_action_show_badge))
    }

    Text(
      text = stringResource(StringsR.string.support_description_local_badge),
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun SupportedContent(listener: SupportListener) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    OutlinedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(StringsR.string.support_badge_label),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = stringResource(StringsR.string.support_thank_you),
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = stringResource(StringsR.string.support_description_local_badge),
          style = MaterialTheme.typography.bodySmall,
        )
        SupporterBadge()
      }
    }

    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = listener::openSupport,
    ) {
      Icon(
        imageVector = Icons.Outlined.FavoriteBorder,
        contentDescription = null,
      )
      Text(stringResource(StringsR.string.support_action_open_kofi))
    }

    ListItem(
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          listener.setSupporterBadgeVisible(false)
        },
      headlineContent = {
        Text(stringResource(StringsR.string.support_badge_setting))
      },
      supportingContent = {
        Text(stringResource(StringsR.string.support_badge_setting_summary))
      },
      trailingContent = {
        Switch(
          checked = true,
          onCheckedChange = listener::setSupporterBadgeVisible,
        )
      },
    )
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
