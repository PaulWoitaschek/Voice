package voice.bookOverview.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import voice.bookOverview.BookOverviewViewState
import voice.bookOverview.R
import voice.data.Book
import kotlin.math.roundToInt

@Composable
internal fun GridBooks(viewState: BookOverviewViewState.Content, onBookClick: (Book.Id) -> Unit) {
  val cellCount = gridColumnCount()
  LazyVerticalGrid(
    cells = GridCells.Fixed(cellCount),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 4.dp),
  ) {
    viewState.books.forEach { (category, books) ->
      if (books.isEmpty()) return@forEach
      item(
        span = { GridItemSpan(maxLineSpan) },
        key = { category },
        contentType = "header"
      ) {
        Header(
          modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
          category = category
        )
      }
      items(
        items = books,
        key = { it.id },
        contentType = { "item" }
      ) { book ->
        GridBook(book, onBookClick)
      }
    }
  }
}

@Composable
private fun GridBook(
  book: BookOverviewViewState.Content.BookViewState,
  onBookClick: (Book.Id) -> Unit,
) {
  Card(
    Modifier
      .fillMaxWidth()
      .clickable { onBookClick(book.id) }) {
    Column {
      Image(
        modifier = Modifier
          .aspectRatio(4F / 3F)
          .padding(start = 8.dp, end = 8.dp, top = 8.dp)
          .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop,
        painter = rememberImagePainter(data = book.cover) {
          fallback(R.drawable.album_art)
          error(R.drawable.album_art)
        },
        contentDescription = null
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
          progress = book.progress
        )
      }
    }
  }
}

@Composable
private fun gridColumnCount(): Int {
  val displayMetrics = LocalContext.current.resources.displayMetrics
  val widthPx = displayMetrics.widthPixels.toFloat()
  val desiredPx = with(LocalDensity.current) {
    180.dp.toPx()
  }
  val columns = (widthPx / desiredPx).roundToInt()
  return columns.coerceAtLeast(2)
}
