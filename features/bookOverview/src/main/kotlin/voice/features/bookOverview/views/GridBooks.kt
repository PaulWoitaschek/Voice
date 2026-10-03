package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.core.data.BookId
import voice.core.ui.sharedCoverElementModifier
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewItemViewState
import kotlin.math.roundToInt
import voice.core.ui.R as UiR

import voice.features.bookOverview.overview.BookOverviewItem
import voice.core.ui.icons.VoiceIcons
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row

@Composable
internal fun GridBooks(
  books: Map<BookOverviewCategory, List<BookOverviewItem>>,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  showPermissionBugCard: Boolean,
  onPermissionBugCardClick: () -> Unit,
  onToggleAuthorExpanded: (BookOverviewCategory, String) -> Unit,
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
        PermissionBugCard(onPermissionBugCardClick)
      }
    }
    books.forEach { (category, booksInCategory) ->
      if (booksInCategory.isEmpty()) return@forEach
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
      booksInCategory.forEach { item ->
        when (item) {
          is BookOverviewItem.SingleBook -> {
            item(
              key = item.id,
              contentType = "item",
            ) {
              GridBook(
                book = item.state.value,
                onBookClick = onBookClick,
                onBookLongClick = onBookLongClick,
              )
            }
          }
          is BookOverviewItem.AuthorGroup -> {
            item(
              span = { GridItemSpan(maxLineSpan) },
              key = item.id,
              contentType = "author_group",
            ) {
              AuthorGroupGridHeader(
                author = item.author,
                isExpanded = item.isExpanded,
                bookCount = item.bookCount,
                onToggleExpanded = { onToggleAuthorExpanded(item.category, item.author) },
              )
            }
            if (item.isExpanded) {
              item.seriesGroups.forEach { seriesGroup ->
                if (seriesGroup.seriesName != null) {
                  item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "${item.id}_series_${seriesGroup.seriesName}",
                    contentType = "series_group",
                  ) {
                    SeriesGridHeader(seriesName = seriesGroup.seriesName)
                  }
                }
                items(
                  items = seriesGroup.books,
                  key = { bookState -> bookState.value.id.value },
                  contentType = { "item" },
                ) { bookState ->
                  GridBook(
                    book = bookState.value,
                    onBookClick = onBookClick,
                    onBookLongClick = onBookLongClick,
                  )
                }
              }
            }
          }
        }
      }
      item(
        span = { GridItemSpan(maxLineSpan) },
      ) {
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
      }
    }
  }
}

@Composable
internal fun AuthorGroupGridHeader(
  author: String,
  isExpanded: Boolean,
  bookCount: Int,
  onToggleExpanded: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable { onToggleExpanded() }
      .padding(horizontal = 8.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = VoiceIcons.Person,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 16.dp),
    ) {
      Text(
        text = author,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "$bookCount books",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(
      imageVector = if (isExpanded) VoiceIcons.ExpandMore else VoiceIcons.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
internal fun GridBook(
  book: BookOverviewItemViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
) {
  BookCard(
    bookId = book.id,
    onBookClick = onBookClick,
    onBookLongClick = onBookLongClick,
  ) {
    Column(
      modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(4f / 3f)
          .sharedCoverElementModifier(book.id)
          .clip(MaterialTheme.shapes.large)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        AsyncImage(
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          model = book.cover,
          placeholder = painterResource(id = UiR.drawable.album_art),
          error = painterResource(id = UiR.drawable.album_art),
          contentDescription = null,
        )
      }

      Spacer(Modifier.height(4.dp))

      Text(
        text = book.name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )

      if (book.series != null) {
        val partString = if (!book.seriesPart.isNullOrBlank()) ", Part ${book.seriesPart}" else ""
        Text(
          text = "${book.series}$partString",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      BookRemainingProgressRow(
        remainingTime = book.remainingTime,
        progress = book.progress,
      )

      Spacer(Modifier.height(8.dp))
      BookProgressIndicator(progress = book.progress)
    }
  }
}

@Composable
internal fun gridColumnCount(): Int {
  val displayMetrics = LocalResources.current.displayMetrics
  val widthPx = displayMetrics.widthPixels.toFloat()
  val desiredPx = with(LocalDensity.current) {
    180.dp.toPx()
  }
  val columns = (widthPx / desiredPx).roundToInt()
  return columns.coerceAtLeast(2)
}

@Composable
@Preview(widthDp = 200)
private fun GridBookPreviewWithProgress() {
  GridBook(BookOverviewPreviewParameterProvider().book().copy(progress = 0.66f), {}, {})
}

@Composable
@Preview(widthDp = 200)
private fun GridBookPreviewWithoutProgress() {
  GridBook(BookOverviewPreviewParameterProvider().book().copy(progress = 0f), {}, {})
}

@Composable
internal fun SeriesGridHeader(seriesName: String) {
  Text(
    text = seriesName,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
  )
}

