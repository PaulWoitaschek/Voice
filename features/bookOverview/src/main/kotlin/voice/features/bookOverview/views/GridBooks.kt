@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.features.bookOverview.overview.BookOverviewItemViewState
import kotlin.math.roundToInt

@Composable
internal fun GridBook(
  book: BookOverviewItemViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  modifier: Modifier = Modifier,
  finished: Boolean = false,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .combinedClickable(
        onClick = { onBookClick(book.id) },
        onLongClick = { onBookLongClick(book.id) },
      )
      .padding(6.dp),
  ) {
    BookCover(
      bookId = book.id,
      cover = book.cover,
      finished = finished,
      unavailable = book.unavailable,
      download = book.download,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1F),
    )
    // right under the cover, so the bars in a row line up however long the titles are
    if (!finished && book.progress > 0F) {
      Spacer(Modifier.height(8.dp))
      LinearProgressIndicator(
        progress = { book.progress },
        modifier = Modifier.fillMaxWidth(),
        gapSize = 0.dp,
        drawStopIndicator = {},
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = book.name,
      style = MaterialTheme.typography.titleSmallEmphasized.bookText(),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (book.author != null) {
      Text(
        text = book.author,
        style = MaterialTheme.typography.labelMedium.bookText(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

internal object BookGridCells : GridCells {
  override fun Density.calculateCrossAxisCellSizes(
    availableSize: Int,
    spacing: Int,
  ): List<Int> {
    val columns = (availableSize / 137.dp.toPx()).roundToInt().coerceAtLeast(2)
    return with(GridCells.Fixed(columns)) { calculateCrossAxisCellSizes(availableSize, spacing) }
  }
}

@Composable
@Preview(widthDp = 200)
private fun GridBookPreviewWithProgress() {
  VoiceTheme {
    Surface {
      GridBook(BookOverviewPreviewParameterProvider().book().copy(progress = 0.66f), {}, {})
    }
  }
}

@Composable
@Preview(widthDp = 200)
private fun GridBookPreviewFinished() {
  VoiceTheme {
    Surface {
      GridBook(BookOverviewPreviewParameterProvider().book().copy(progress = 1f), {}, {}, finished = true)
    }
  }
}
