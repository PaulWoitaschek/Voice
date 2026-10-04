package voice.features.settings.medianotification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.notification.NotificationAction
import voice.core.ui.icons.VoiceIcons
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@Composable
private fun MediaNotificationSettings(
  viewState: MediaNotificationSettingsViewState,
  viewModel: MediaNotificationSettingsViewModel,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
          Text(stringResource(StringsR.string.settings_media_notification_title))
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
    LazyColumn(contentPadding = contentPadding) {
      item {
        NotificationActionRow(1, viewState.slot1) { viewModel.editSlot(1) }
      }
      item {
        NotificationActionRow(2, viewState.slot2) { viewModel.editSlot(2) }
      }
      item {
        NotificationActionRow(3, viewState.slot3) { viewModel.editSlot(3) }
      }
    }
  }

  val editingSlot = viewState.editingSlot
  if (editingSlot != null) {
    val selected = when (editingSlot) {
      1 -> viewState.slot1
      2 -> viewState.slot2
      else -> viewState.slot3
    }
    NotificationActionDialog(
      slot = editingSlot,
      selectedAction = selected,
      onActionSelect = { action -> viewModel.selectAction(editingSlot, action) },
      onDismiss = viewModel::dismissDialog,
    )
  }
}

@Composable
private fun NotificationActionRow(
  slot: Int,
  action: NotificationAction,
  onClick: () -> Unit,
) {
  ListItem(
    modifier = Modifier
      .clickable { onClick() }
      .fillMaxWidth(),
    headlineContent = {
      Text(stringResource(StringsR.string.settings_media_notification_button_label, slot))
    },
    supportingContent = {
      Text(action.label())
    },
  )
}

@Composable
private fun NotificationActionDialog(
  slot: Int,
  selectedAction: NotificationAction,
  onActionSelect: (NotificationAction) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(StringsR.string.settings_media_notification_button_label, slot))
    },
    text = {
      Column {
        NotificationAction.entries.forEach { action ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = action == selectedAction,
                onClick = { onActionSelect(action) },
                role = Role.RadioButton,
              )
              .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = action == selectedAction,
              onClick = null,
            )
            Spacer(Modifier.width(16.dp))
            Text(action.label())
          }
        }
      }
    },
    confirmButton = {},
  )
}

@Composable
private fun NotificationAction.label(): String = when (this) {
  NotificationAction.REWIND -> stringResource(StringsR.string.playback_action_rewind)
  NotificationAction.FAST_FORWARD -> stringResource(StringsR.string.playback_action_fast_forward)
  NotificationAction.NEXT_CHAPTER -> stringResource(StringsR.string.playback_chapter_next)
  NotificationAction.PREVIOUS_CHAPTER -> stringResource(StringsR.string.playback_chapter_previous)
  NotificationAction.SLEEP_TIMER -> stringResource(StringsR.string.sleep_timer_action_open)
  NotificationAction.SKIP_SILENCE -> stringResource(StringsR.string.playback_option_skip_silence)
  NotificationAction.BOOKMARK -> stringResource(StringsR.string.bookmark_title)
}

@ContributesTo(AppScope::class)
interface MediaNotificationSettingsGraph {
  val mediaNotificationSettingsViewModel: MediaNotificationSettingsViewModel
}

@ContributesTo(AppScope::class)
interface MediaNotificationSettingsProvider {

  @Provides
  @IntoSet
  fun mediaNotificationSettingsNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.MediaNotificationSettings> { key ->
    NavEntry(key) {
      MediaNotificationSettings()
    }
  }
}

@Composable
fun MediaNotificationSettings() {
  val viewModel = retain<MediaNotificationSettingsViewModel> {
    rootGraphAs<MediaNotificationSettingsGraph>().mediaNotificationSettingsViewModel
  }
  val viewState = viewModel.viewState()
  MediaNotificationSettings(viewState, viewModel)
}
