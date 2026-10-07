@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import voice.features.bookOverview.overview.BookOverviewCategory

@Composable
internal fun Header(
  category: BookOverviewCategory,
  count: Int,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.semantics(mergeDescendants = true) { heading() },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(id = category.nameRes),
      style = MaterialTheme.typography.titleLargeEmphasized,
    )
    Spacer(Modifier.width(10.dp))
    Text(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        .padding(horizontal = 10.dp, vertical = 2.dp),
      text = count.toString(),
      style = MaterialTheme.typography.labelLargeEmphasized,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
  }
}
