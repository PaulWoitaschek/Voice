package voice.bookOverview.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.bookOverview.R

@Composable
internal fun MigrateHint(onClick: () -> Unit) {
  ExplanationTooltip {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
      Text(stringResource(R.string.migration_hint_title), style = MaterialTheme.typography.headlineSmall)
      Spacer(modifier = Modifier.size(8.dp))
      Text(stringResource(R.string.migration_hint_content))
      Spacer(modifier = Modifier.size(16.dp))
      Button(
        modifier = Modifier.align(Alignment.End),
        onClick = onClick,
      ) {
        Text(stringResource(R.string.migration_hint_confirm))
      }
    }
  }
}
