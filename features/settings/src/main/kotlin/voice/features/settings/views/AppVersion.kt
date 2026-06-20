package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.ui.icons.Tag
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun AppVersion(
  appVersion: String,
  onClick: () -> Unit,
) {
  ListItem(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        onClick()
      },
    leadingContent = {
      Icon(
        imageVector = VoiceIcons.Tag,
        contentDescription = stringResource(StringsR.string.settings_about_app_version_title),
      )
    },
    headlineContent = {
      Text(
        text = stringResource(StringsR.string.settings_about_app_version_title),
        color = LocalContentColor.current.copy(alpha = 0.5F),
      )
    },
    supportingContent = {
      Text(
        text = appVersion,
        color = LocalContentColor.current.copy(alpha = 0.5F),
      )
    },
  )
}
