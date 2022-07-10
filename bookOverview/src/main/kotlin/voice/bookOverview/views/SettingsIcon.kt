package voice.bookOverview.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.bookOverview.R

@Composable
internal fun SettingsIcon(onSettingsClick: () -> Unit) {
  IconButton(onSettingsClick) {
    Icon(
      imageVector = Icons.Outlined.Settings,
      contentDescription = stringResource(R.string.action_settings)
    )
  }
}
