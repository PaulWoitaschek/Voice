package voice.features.settings.developer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun StringFeatureFlagRow(
  viewState: DeveloperSettingsViewState.FeatureFlagViewState.StringFlag,
  clearOverride: () -> Unit,
  setOverride: (String) -> Unit,
) {
  var showDialog by remember { mutableStateOf(false) }
  ListItem(
    modifier = Modifier.clickable {
      showDialog = true
    },
    headlineContent = {
      Text(viewState.key)
    },
    supportingContent = {
      Column {
        Text(viewState.description)
        Text(viewState.value)
      }
    },
    trailingContent = {
      Row {
        if (viewState.isOverridden) {
          TextButton(
            onClick = {
              clearOverride()
            },
          ) {
            Text("Reset")
          }
        }
        TextButton(
          onClick = {
            showDialog = true
          },
        ) {
          Text("Edit")
        }
      }
    },
  )
  if (showDialog) {
    EditStringFeatureFlagDialog(
      key = viewState.key,
      initialValue = viewState.value,
      onDismiss = {
        showDialog = false
      },
      onConfirm = { value ->
        setOverride(value)
        showDialog = false
      },
    )
  }
}
