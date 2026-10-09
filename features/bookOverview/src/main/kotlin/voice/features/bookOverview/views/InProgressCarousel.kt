@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.strings.R
import voice.features.bookOverview.overview.BookOverviewItemViewState

/**
 * The other books you are in the middle of, as an M3 multi-browse carousel. Labels fade out as an
 * item shrinks towards the edge.
 */
@Composable
internal fun InProgressCarousel(
  books: List<BookOverviewItemViewState>,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = rememberCarouselState { books.size }
  HorizontalMultiBrowseCarousel(
    state = state,
    preferredItemWidth = 180.dp,
    itemSpacing = 8.dp,
    modifier = modifier
      .fillMaxWidth()
      .height(240.dp),
  ) { index ->
    val book = books[index]
    val shape = MaterialTheme.shapes.extraLarge
    Box(
      modifier = Modifier
        .fillMaxSize()
        .maskClip(shape)
        .combinedClickable(
          onClick = { onBookClick(book.id) },
          onLongClick = { onBookLongClick(book.id) },
        ),
    ) {
      BookCover(
        bookId = book.id,
        cover = book.cover,
        shape = shape,
        unavailable = book.unavailable,
        downloadProgress = book.downloadProgress,
        modifier = Modifier.fillMaxSize(),
      )
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .graphicsLayer {
            val info = carouselItemDrawInfo
            val range = info.maxSize - info.minSize
            alpha = if (range <= 0F) 1F else ((info.size - info.minSize) / range).coerceIn(0F, 1F)
          }
          .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8F))))
          .padding(start = 14.dp, end = 14.dp, top = 40.dp, bottom = 14.dp),
      ) {
        Text(
          text = book.name,
          style = MaterialTheme.typography.titleMediumEmphasized,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = stringResource(R.string.playback_book_progress_remaining, book.remainingTime),
          style = MaterialTheme.typography.labelMedium,
          color = Color.White.copy(alpha = 0.8F),
          maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { book.progress },
          modifier = Modifier.fillMaxWidth(),
          color = Color.White,
          trackColor = Color.White.copy(alpha = 0.3F),
        )
      }
    }
  }
}
