package voice.features.settings.developer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun BooleanFeatureFlagRow(
  viewState: DeveloperSettingsViewState.FeatureFlagViewState.BooleanFlag,
  clearOverride: () -> Unit,
  setOverride: (Boolean) -> Unit,
) {
  ListItem(
    modifier = Modifier.clickable {
      setOverride(!viewState.value)
    },
    headlineContent = {
      Text(viewState.key)
    },
    supportingContent = {
      Text(if (viewState.isOverridden) "Override active" else "Using remote config")
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
        Switch(
          checked = viewState.value,
          onCheckedChange = {
            setOverride(it)
          },
        )
      }
    },
  )
}
