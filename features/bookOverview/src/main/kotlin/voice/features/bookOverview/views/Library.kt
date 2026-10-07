@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.strings.R
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewLayoutMode
import voice.features.bookOverview.overview.BookOverviewViewState
import java.util.Calendar

/**
 * The library: a greeting, a hero card for the book you are listening to, a carousel of the other
 * books in progress and the rest of the library below, as a list or grid.
 */
@Composable
internal fun Library(
  viewState: BookOverviewViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onPlayClick: () -> Unit,
  onPermissionBugCardClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val grid = viewState.layoutMode == BookOverviewLayoutMode.Grid
  val columns = if (grid) gridColumnCount() else 1
  val heroId = viewState.currentBookId
  val hero = heroId?.let { id -> viewState.books.values.firstNotNullOfOrNull { it[id] } }
  val inProgress = viewState.books[BookOverviewCategory.CURRENT].orEmpty()
  val inProgressOthers = inProgress.filterKeys { it != heroId }.values.map { it.value }

  LazyVerticalGrid(
    modifier = modifier,
    columns = GridCells.Fixed(columns),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (viewState.showStoragePermissionBugCard) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        PermissionBugCard(onPermissionBugCardClick)
      }
    }
    item(key = "greeting", span = { GridItemSpan(maxLineSpan) }, contentType = "greeting") {
      LibraryGreeting(
        inProgressCount = inProgress.size,
        modifier = Modifier.padding(top = 16.dp, bottom = 20.dp, start = 4.dp),
      )
    }
    if (hero != null) {
      item(key = "hero", span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
        ContinueListeningCard(
          book = hero.value,
          playing = viewState.playButtonState == BookOverviewViewState.PlayButtonState.Playing,
          onClick = { onBookClick(hero.value.id) },
          onLongClick = { onBookLongClick(hero.value.id) },
          onPlayClick = onPlayClick,
        )
      }
    }
    if (inProgressOthers.isNotEmpty()) {
      item(key = BookOverviewCategory.CURRENT, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        Header(
          category = BookOverviewCategory.CURRENT,
          count = inProgressOthers.size,
          modifier = Modifier.padding(top = 32.dp, bottom = 12.dp, start = 4.dp),
        )
      }
      item(key = "carousel", span = { GridItemSpan(maxLineSpan) }, contentType = "carousel") {
        InProgressCarousel(
          books = inProgressOthers,
          onBookClick = onBookClick,
          onBookLongClick = onBookLongClick,
        )
      }
    }
    listOf(BookOverviewCategory.NOT_STARTED, BookOverviewCategory.FINISHED).forEach { category ->
      val books = viewState.books[category].orEmpty().filterKeys { it != heroId }.toList()
      if (books.isEmpty()) return@forEach
      val finished = category == BookOverviewCategory.FINISHED
      item(key = category, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        Header(
          category = category,
          count = books.size,
          modifier = Modifier.padding(top = 32.dp, bottom = 12.dp, start = 4.dp),
        )
      }
      if (grid) {
        items(
          items = books,
          key = { (bookId, _) -> bookId.value },
          contentType = { "gridBook" },
        ) { (_, book) ->
          GridBook(
            book = book.value,
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
            finished = finished,
            modifier = Modifier.padding(bottom = 8.dp),
          )
        }
      } else {
        itemsIndexed(
          items = books,
          key = { _, (bookId, _) -> bookId.value },
          contentType = { _, _ -> "listBook" },
        ) { index, (_, book) ->
          ListBookRow(
            book = book.value,
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
            finished = finished,
            shape = segmentedShape(index, books.size),
            modifier = Modifier.padding(bottom = 2.dp),
          )
        }
      }
    }
    item(span = { GridItemSpan(maxLineSpan) }, contentType = "navigationBarSpacer") {
      Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
  }
}

@Composable
private fun LibraryGreeting(
  inProgressCount: Int,
  modifier: Modifier = Modifier,
) {
  val greeting = remember {
    when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
      in 5..11 -> R.string.library_greeting_morning
      in 12..16 -> R.string.library_greeting_afternoon
      in 17..21 -> R.string.library_greeting_evening
      else -> R.string.library_greeting_night
    }
  }
  Column(modifier = modifier) {
    Text(
      text = stringResource(greeting),
      style = MaterialTheme.typography.displaySmallEmphasized,
    )
    if (inProgressCount > 0) {
      Text(
        text = pluralStringResource(R.plurals.library_header_in_progress, inProgressCount, inProgressCount),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
