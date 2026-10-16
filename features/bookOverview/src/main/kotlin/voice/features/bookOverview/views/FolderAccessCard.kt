package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.ui.VoiceTheme
import voice.core.strings.R as StringsR

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FolderAccessCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier
      .padding(horizontal = 8.dp)
      .fillMaxWidth(),
  ) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
      Text(
        text = stringResource(StringsR.string.library_folders_no_access_card_title),
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(Modifier.size(4.dp))
      Text(text = stringResource(StringsR.string.library_folders_no_access_card_message))
      Spacer(Modifier.size(16.dp))
      Button(onClick = onClick, shapes = ButtonDefaults.shapes()) {
        Text(text = stringResource(StringsR.string.library_folders_no_access_card_action))
      }
    }
  }
}

@Composable
@Preview
private fun FolderAccessCardPreview() {
  VoiceTheme {
    FolderAccessCard(onClick = {})
  }
}
