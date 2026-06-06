package voice.features.bookOverview.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR

@Composable
internal fun AddBookHint() {
  ExplanationTooltip {
    Text(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      text = stringResource(StringsR.string.library_empty_add_first_book),
    )
  }
}
