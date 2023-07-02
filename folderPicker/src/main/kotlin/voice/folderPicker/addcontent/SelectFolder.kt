package voice.folderPicker.addcontent

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.common.navigation.Destination
import voice.folderPicker.R
import voice.folderPicker.folderPicker.FileTypeSelection
import voice.strings.R as StringsR

@Composable
internal fun SelectFolder(
  onBack: () -> Unit,
  onAdd: (FileTypeSelection, Uri) -> Unit,
  mode: Destination.AddContent.Mode,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      SelectFolderAppBar(onBack)
    },
    content = { contentPadding ->
      Column(Modifier.padding(contentPadding)) {
        if (LocalConfiguration.current.screenHeightDp > 600) {
          Column(Modifier.weight(1F)) {
            Image(
              modifier = Modifier
                .heightIn(max = 400.dp)
                .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.CenterHorizontally),
              painter = painterResource(id = R.drawable.folder_type_artwork),
              contentDescription = null,
            )
          }
        }

        Column(Modifier.weight(2F)) {
          Spacer(modifier = Modifier.size(16.dp))

          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(
              when (mode) {
                Destination.AddContent.Mode.Default -> StringsR.string.select_folder_title_default
                Destination.AddContent.Mode.Onboarding -> StringsR.string.select_folder_title_onboarding
              },
            ),
            style = MaterialTheme.typography.displayMedium,
          )
          Spacer(modifier = Modifier.size(4.dp))
          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(StringsR.string.select_folder_subtitle),
            style = MaterialTheme.typography.bodyLarge,
          )
          Spacer(modifier = Modifier.size(24.dp))
          SelectFolderButtonRow(onAdd)
        }
      }
    },
  )
}

@Composable
@Preview
private fun SelectFolderPreview() {
  SelectFolder(
    onBack = {},
    onAdd = { _, _ -> },
    mode = Destination.AddContent.Mode.Default,
  )
}
