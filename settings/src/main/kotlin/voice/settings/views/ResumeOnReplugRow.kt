package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.settings.R

@Composable
internal fun ResumeOnReplugRow(
  resumeOnReplug: Boolean,
  toggle: () -> Unit,
) {
  ListItem(
    modifier = Modifier
      .clickable {
        toggle()
      }
      .fillMaxWidth(),
    leadingContent = {
      Icon(
        imageVector = Icons.Outlined.Headset,
        contentDescription = stringResource(R.string.pref_resume_on_replug),
      )
    },
    headlineText = {
      Text(
        text = stringResource(R.string.pref_resume_on_replug),
        style = MaterialTheme.typography.bodyLarge,
      )
    },
    supportingText = {
      Text(
        text = stringResource(R.string.pref_resume_on_replug_hint),
        style = MaterialTheme.typography.bodyMedium,
      )
    },
    trailingContent = {
      Switch(
        checked = resumeOnReplug,
        onCheckedChange = {
          toggle()
        },
      )
    },
  )
}
