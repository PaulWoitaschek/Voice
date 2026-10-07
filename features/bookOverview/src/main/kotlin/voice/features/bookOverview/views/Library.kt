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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.strings.R
import voice.core.ui.HoldSplashScreenWhile
import voice.core.ui.entrance
import voice.core.ui.rememberEntranceState
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewLayoutMode
import voice.features.bookOverview.overview.BookOverviewViewState
import java.util.Calendar

/**
 * The library: a greeting, a hero card for the book you are listening to, a carousel of the other
 * books in progress and the rest of the library below, as a list or grid.
 *
 * Opening the app keeps the splash screen up until the library is loaded, then the sections float in
 * one after another. That only happens once, not when coming back from another screen.
 */
@Composable
internal fun Library(
  viewState: BookOverviewViewState,
  gridState: LazyGridState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onPlayClick: () -> Unit,
  onPermissionBugCardClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var entered by rememberSaveable { mutableStateOf(false) }
  // by identity, as a loaded empty library can equal the loading state
  val loading = viewState === BookOverviewViewState.Loading
  HoldSplashScreenWhile(loading = loading)
  if (loading) return
  val entrance = rememberEntranceState(animate = !entered)
  LaunchedEffect(Unit) { entered = true }
  // sections further down join the stagger at the same time, so they don't lag behind when scrolled to
  var nextEntranceIndex = 0
  fun entranceIndex() = nextEntranceIndex++.coerceAtMost(MAX_ENTRANCE_INDEX)

  val grid = viewState.layoutMode == BookOverviewLayoutMode.Grid
  val columns = if (grid) gridColumnCount() else 1
  val heroId = viewState.currentBookId
  val hero = heroId?.let { id -> viewState.books.values.firstNotNullOfOrNull { it[id] } }
  val inProgress = viewState.books[BookOverviewCategory.CURRENT].orEmpty()
  val inProgressOthers = inProgress.filterKeys { it != heroId }.values.map { it.value }

  LazyVerticalGrid(
    modifier = modifier,
    state = gridState,
    columns = GridCells.Fixed(columns),
    contentPadding = PaddingValues(
      start = 16.dp,
      end = 16.dp,
      top = 8.dp,
      // room to scroll the last books above the floating play button
      bottom = if (viewState.playButtonState != null) 104.dp else 24.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (viewState.showStoragePermissionBugCard) {
      val index = entranceIndex()
      item(span = { GridItemSpan(maxLineSpan) }) {
        PermissionBugCard(onPermissionBugCardClick, Modifier.entrance(entrance, index))
      }
    }
    val greetingIndex = entranceIndex()
    item(key = "greeting", span = { GridItemSpan(maxLineSpan) }, contentType = "greeting") {
      LibraryGreeting(
        inProgressCount = inProgress.size,
        modifier = Modifier
          .entrance(entrance, greetingIndex)
          .padding(top = 16.dp, bottom = 20.dp, start = 4.dp),
      )
    }
    if (hero != null) {
      val index = entranceIndex()
      item(key = HERO_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
        ContinueListeningCard(
          book = hero.value,
          playing = viewState.playButtonState == BookOverviewViewState.PlayButtonState.Playing,
          onClick = { onBookClick(hero.value.id) },
          onLongClick = { onBookLongClick(hero.value.id) },
          onPlayClick = onPlayClick,
          modifier = Modifier.entrance(entrance, index),
        )
      }
    }
    if (inProgressOthers.isNotEmpty()) {
      val headerIndex = entranceIndex()
      item(key = BookOverviewCategory.CURRENT, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        Header(
          category = BookOverviewCategory.CURRENT,
          count = inProgressOthers.size,
          modifier = Modifier
            .entrance(entrance, headerIndex)
            .padding(top = 32.dp, bottom = 12.dp, start = 4.dp),
        )
      }
      val carouselIndex = entranceIndex()
      item(key = "carousel", span = { GridItemSpan(maxLineSpan) }, contentType = "carousel") {
        InProgressCarousel(
          books = inProgressOthers,
          onBookClick = onBookClick,
          onBookLongClick = onBookLongClick,
          modifier = Modifier.entrance(entrance, carouselIndex),
        )
      }
    }
    listOf(BookOverviewCategory.NOT_STARTED, BookOverviewCategory.FINISHED).forEach { category ->
      val books = viewState.books[category].orEmpty().filterKeys { it != heroId }.toList()
      if (books.isEmpty()) return@forEach
      val finished = category == BookOverviewCategory.FINISHED
      val headerIndex = entranceIndex()
      item(key = category, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        Header(
          category = category,
          count = books.size,
          modifier = Modifier
            .entrance(entrance, headerIndex)
            .padding(top = 32.dp, bottom = 12.dp, start = 4.dp),
        )
      }
      val bookIndexes = books.map { entranceIndex() }
      if (grid) {
        itemsIndexed(
          items = books,
          key = { _, (bookId, _) -> bookId.value },
          contentType = { _, _ -> "gridBook" },
        ) { index, (_, book) ->
          GridBook(
            book = book.value,
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
            finished = finished,
            modifier = Modifier
              .entrance(entrance, bookIndexes[index])
              .padding(bottom = 8.dp),
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
            modifier = Modifier
              .entrance(entrance, bookIndexes[index])
              .padding(bottom = 2.dp),
          )
        }
      }
    }
    item(span = { GridItemSpan(maxLineSpan) }, contentType = "navigationBarSpacer") {
      Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
  }
}

private const val MAX_ENTRANCE_INDEX = 6

internal const val HERO_KEY = "hero"

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
