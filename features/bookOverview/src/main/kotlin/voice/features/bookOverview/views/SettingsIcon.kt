package voice.features.bookOverview.views

import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.core.ui.AttentionBadge
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun SettingsIcon(
  showFolderAccessBadge: Boolean,
  onSettingsClick: () -> Unit,
) {
  IconButton(onSettingsClick) {
    BadgedBox(
      badge = {
        if (showFolderAccessBadge) {
          AttentionBadge(stringResource(StringsR.string.library_folders_no_access_badge))
        }
      },
    ) {
      Icon(
        imageVector = VoiceIcons.Settings,
        contentDescription = stringResource(StringsR.string.settings_action_open),
      )
    }
  }
}
