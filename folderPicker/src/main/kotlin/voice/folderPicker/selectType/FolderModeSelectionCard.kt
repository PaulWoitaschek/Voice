package voice.folderPicker.selectType

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceTheme
import voice.data.folders.FolderType
import voice.folderPicker.FolderTypeIcon
import voice.folderPicker.R

@Composable
internal fun FolderModeSelectionCard(
  onFolderModeSelected: (FolderMode) -> Unit,
  selectedFolderMode: FolderMode,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
  ) {
    Column(
      modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FolderMode.values().forEach { folderMode ->
        val selectFolder = { onFolderModeSelected(folderMode) }
        FolderModeColumn(selectFolder = selectFolder, selectedFolderMode = selectedFolderMode, folderMode = folderMode)
      }
    }
  }
}

@Composable
private fun FolderModeColumn(
  selectedFolderMode: FolderMode,
  folderMode: FolderMode,
  selectFolder: () -> Unit,
) {
  Row(
    modifier = Modifier
      .clickable(onClick = selectFolder)
      .padding(end = 24.dp, start = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(
      selected = selectedFolderMode == folderMode,
      onClick = selectFolder,
    )
    Spacer(Modifier.size(16.dp))
    Text(
      text = stringResource(id = folderMode.title()),
      modifier = Modifier.weight(1F),
    )
    FolderTypeIcon(
      folderType = when (folderMode) {
        FolderMode.Audiobooks -> FolderType.Root
        FolderMode.SingleBook -> FolderType.SingleFolder
      },
    )
  }
}

@StringRes
private fun FolderMode.title(): Int {
  return when (this) {
    FolderMode.Audiobooks -> R.string.folder_mode_root
    FolderMode.SingleBook -> R.string.folder_mode_single
  }
}

@Composable
@Preview
private fun FolderModeSelectionCardPreview() {
  VoiceTheme {
    FolderModeSelectionCard(
      onFolderModeSelected = {},
      selectedFolderMode = FolderMode.Audiobooks,
    )
  }
}
