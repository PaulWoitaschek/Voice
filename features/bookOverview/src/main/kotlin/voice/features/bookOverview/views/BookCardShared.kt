package voice.features.bookOverview.views

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import voice.core.data.BookId

@Composable
internal fun BookCard(
  bookId: BookId,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  ElevatedCard(
    shape = MaterialTheme.shapes.extraLarge,
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = { onBookClick(bookId) },
        onLongClick = { onBookLongClick(bookId) },
      ),
  ) {
    content()
  }
}

@Composable
internal fun BookRemainingProgressRow(
  remainingTime: String,
  progress: Float,
  modifier: Modifier = Modifier,
  remainingTimeMaxLines: Int = Int.MAX_VALUE,
  progressMaxLines: Int = Int.MAX_VALUE,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = remainingTime,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = remainingTimeMaxLines,
    )
    if (progress > 0f) {
      Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = progressMaxLines,
      )
    }
  }
}

@Composable
internal fun BookProgressIndicator(
  progress: Float,
  modifier: Modifier = Modifier,
  color: Color? = null,
  trackColor: Color? = null,
) {
  if (progress > 0.05f) {
    if (color != null && trackColor != null) {
      LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
      )
    } else {
      LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
      )
    }
  }
}
