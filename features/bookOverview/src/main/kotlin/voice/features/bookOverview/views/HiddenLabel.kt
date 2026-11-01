package voice.features.bookOverview.views

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR

@Composable
internal fun HiddenLabel(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
      text = stringResource(StringsR.string.library_book_hidden),
      style = MaterialTheme.typography.labelMedium,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
    )
  }
}
