package voice.bookOverview.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.strings.R as StringsR

@Composable
internal fun PermissionBugCard(onPermissionBugCardClick: () -> Unit) {
  Card(
    Modifier
      .padding(horizontal = 8.dp)
      .fillMaxWidth(),
  ) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
      Text(text = stringResource(id = StringsR.string.storage_bug_title), style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.size(4.dp))
      Text(text = stringResource(id = StringsR.string.storage_bug_subtitle))
      Spacer(modifier = Modifier.size(16.dp))
      Button(
        onClick = onPermissionBugCardClick,
      ) {
        Text(text = stringResource(id = StringsR.string.storage_bug_button))
      }
    }
  }
}

@Composable
@Preview
private fun PermissionBugCardPreview() {
  PermissionBugCard(onPermissionBugCardClick = {})
}
