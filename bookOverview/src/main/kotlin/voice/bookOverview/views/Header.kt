package voice.bookOverview.views

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.bookOverview.overview.BookOverviewCategory

@Composable
internal fun Header(
  modifier: Modifier = Modifier,
  category: BookOverviewCategory
) {
  Text(
    modifier = modifier,
    text = stringResource(id = category.nameRes),
    style = MaterialTheme.typography.headlineSmall
  )
}
