package voice.bookOverview.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.bookOverview.R

@Composable
internal fun AddBookHint() {
  ExplanationTooltip {
    Text(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      text = stringResource(R.string.voice_intro_first_book),
    )
  }
}
