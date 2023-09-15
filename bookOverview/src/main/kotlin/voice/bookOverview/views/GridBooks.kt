package voice.bookOverview.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableMap
import voice.bookOverview.overview.BookOverviewCategory
import voice.bookOverview.overview.BookOverviewItemViewState
import voice.common.BookId
import voice.common.compose.LongClickableCard
import kotlin.math.roundToInt
import voice.common.R as CommonR

@Composable
internal fun GridBooks(
  books: ImmutableMap<BookOverviewCategory, List<BookOverviewItemViewState>>,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  showPermissionBugCard: Boolean,
  onPermissionBugCardClicked: () -> Unit,
) {
  val cellCount = gridColumnCount()
  LazyVerticalGrid(
    columns = GridCells.Fixed(cellCount),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 4.dp),
  ) {
    if (showPermissionBugCard) {
      item(
        span = { GridItemSpan(maxLineSpan) },
      ) {
        PermissionBugCard(onPermissionBugCardClicked)
      }
    }
    books.forEach { (category, books) ->
      if (books.isEmpty()) return@forEach
      item(
        span = { GridItemSpan(maxLineSpan) },
        key = category,
        contentType = "header",
      ) {
        Header(
          modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
          category = category,
        )
      }
      items(
        items = books,
        key = { it.id },
        contentType = { "item" },
      ) { book ->
        GridBook(
          book = book,
          onBookClick = onBookClick,
          onBookLongClick = onBookLongClick,
        )
      }
    }
  }
}

@Composable
internal fun GridBook(
  book: BookOverviewItemViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
) {
  LongClickableCard(
    onClick = {
      onBookClick(book.id)
    },
    onLongClick = {
      onBookLongClick(book.id)
    },
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      AsyncImage(
        modifier = Modifier
          .aspectRatio(4F / 3F)
          .padding(start = 8.dp, end = 8.dp, top = 8.dp)
          .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop,
        model = book.cover?.file,
        placeholder = painterResource(id = CommonR.drawable.album_art),
        error = painterResource(id = CommonR.drawable.album_art),
        contentDescription = null,
      )
      Text(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
        text = book.name,
        maxLines = 3,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        text = book.remainingTime,
        style = MaterialTheme.typography.bodySmall,
      )

      if (book.progress > 0F) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth(),
          progress = book.progress,
        )
      }
    }
  }
}

@Composable
internal fun gridColumnCount(): Int {
  val displayMetrics = LocalContext.current.resources.displayMetrics
  val widthPx = displayMetrics.widthPixels.toFloat()
  val desiredPx = with(LocalDensity.current) {
    180.dp.toPx()
  }
  val columns = (widthPx / desiredPx).roundToInt()
  return columns.coerceAtLeast(2)
}
