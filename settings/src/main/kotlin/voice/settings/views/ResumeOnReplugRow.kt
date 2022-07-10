package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ListItem
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
  toggle: () -> Unit
) {
  ListItem(
    modifier = Modifier
      .clickable {
        toggle()
      }
      .fillMaxWidth(),
    singleLineSecondaryText = false,
    text = {
      Text(
        text = stringResource(R.string.pref_resume_on_replug),
        style = MaterialTheme.typography.bodyLarge
      )
    },
    secondaryText = {
      Text(
        text = stringResource(R.string.pref_resume_on_replug_hint),
        style = MaterialTheme.typography.bodyMedium
      )
    },
    trailing = {
      Switch(
        checked = resumeOnReplug,
        onCheckedChange = {
          toggle()
        }
      )
    }
  )
}
