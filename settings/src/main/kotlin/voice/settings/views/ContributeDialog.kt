package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ListItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.settings.R

@Composable
internal fun ContributeDialog(
  suggestionsClicked: () -> Unit,
  translationsClicked: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = stringResource(R.string.pref_support_title))
    },
    text = {
      Column {
        ListItem(
          modifier = Modifier
            .clickable {
              translationsClicked()
            }
            .fillMaxWidth(),
          text = {
            Text(text = stringResource(R.string.pref_support_translations))
          },
        )
        ListItem(
          modifier = Modifier
            .clickable {
              suggestionsClicked()
            }
            .fillMaxWidth(),
          text = {
            Text(text = stringResource(R.string.pref_support_contribute))
          },
        )
      }
    },
    confirmButton = {},
  )
}
