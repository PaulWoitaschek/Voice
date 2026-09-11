package voice.features.bookOverview.editSeries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditBookSeriesDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  viewState: EditBookSeriesState,
  onUpdateSeries: (String) -> Unit,
  onUpdatePart: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = stringResource(StringsR.string.book_edit_series_title))
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
      ) {
        Text(stringResource(id = StringsR.string.common_dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(id = StringsR.string.common_dialog_cancel))
      }
    },
    text = {
      Column {
        if (viewState.suggestedSeries.isNotEmpty()) {
          ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
          ) {
            TextField(
              value = viewState.currentSeries,
              onValueChange = {
                  expanded = true
                  onUpdateSeries(it)
              },
              label = {
                Text(stringResource(StringsR.string.book_edit_series_label))
              },
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
              },
              colors = ExposedDropdownMenuDefaults.textFieldColors(),
              modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
            ) {
              val filteredOptions = viewState.suggestedSeries.filter {
                  it.contains(viewState.currentSeries, ignoreCase = true)
              }
              filteredOptions.forEach { selectionOption ->
                DropdownMenuItem(
                  text = { Text(selectionOption) },
                  onClick = {
                    onUpdateSeries(selectionOption)
                    expanded = false
                  }
                )
              }
            }
          }
        } else {
          TextField(
            value = viewState.currentSeries,
            onValueChange = onUpdateSeries,
            label = {
              Text(stringResource(StringsR.string.book_edit_series_label))
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
          value = viewState.currentPart,
          onValueChange = onUpdatePart,
          label = {
            Text("Part (Optional)")
          },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
  )
}
