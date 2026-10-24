package voice.features.settings.developer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun EnumFeatureFlagRow(
  viewState: DeveloperSettingsViewState.FeatureFlagViewState.EnumFlag,
  clearOverride: () -> Unit,
  setOverride: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  ListItem(
    modifier = Modifier.clickable {
      expanded = true
    },
    supportingContent = {
      Column {
        Text(viewState.description)
        Text(if (viewState.isOverridden) "Override active" else "Using remote config")
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
        Box {
          TextButton(
            onClick = {
              expanded = true
            },
          ) {
            Text(viewState.value)
          }
          DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
              expanded = false
            },
          ) {
            viewState.entries.forEach { entry ->
              DropdownMenuItem(
                text = {
                  Text(entry)
                },
                leadingIcon = {
                  RadioButton(
                    selected = entry == viewState.value,
                    onClick = null,
                  )
                },
                onClick = {
                  setOverride(entry)
                  expanded = false
                },
              )
            }
          }
        }
      }
    },
  ) {
    Text(viewState.key)
  }
}
