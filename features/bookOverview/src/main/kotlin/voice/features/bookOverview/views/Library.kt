@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.strings.R
import voice.core.ui.CenteredContent
import voice.core.ui.HoldSplashScreenWhile
import voice.core.ui.MaxContentWidth
import voice.core.ui.entrance
import voice.core.ui.rememberEntranceState
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewLayoutMode
import voice.features.bookOverview.overview.BookOverviewViewState
import java.util.Calendar

/**
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
  onSurveyClick: () -> Unit,
  onSurveyDismiss: () -> Unit,
  onShowFoldersClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var entered by rememberSaveable { mutableStateOf(false) }
  // by identity, as a loaded empty library can equal the loading state
  val loading = viewState === BookOverviewViewState.Loading
  HoldSplashScreenWhile(loading = loading)
  if (loading) return
  val entrance = rememberEntranceState(animate = !entered)
  LaunchedEffect(Unit) { entered = true }
  val grid = viewState.layoutMode == BookOverviewLayoutMode.Grid
  val heroId = viewState.currentBookId
  val hero = heroId?.let { id -> viewState.books.values.firstNotNullOfOrNull { it[id] } }
  val inProgressCount = viewState.books[BookOverviewCategory.CURRENT].orEmpty().size

  CenteredContent(modifier = modifier) { centeredPadding ->
    val horizontalPadding = if (grid) 16.dp else centeredPadding
    // sections further down join the stagger at the same time, so they don't lag behind when scrolled to
    var nextEntranceIndex = 0
    fun entranceIndex() = nextEntranceIndex++.coerceAtMost(MAX_ENTRANCE_INDEX)
    // in the grid, a card across a wide grid would leave a lot of empty space next to its content
    val cardWidth = Modifier
      .wrapContentWidth(Alignment.Start)
      .widthIn(max = MaxContentWidth)
    LazyVerticalGrid(
      state = gridState,
      columns = if (grid) BookGridCells else GridCells.Fixed(1),
      contentPadding = PaddingValues(
        start = horizontalPadding,
        end = horizontalPadding,
        top = 8.dp,
        // room to scroll the last books above the floating play button
        bottom = if (viewState.playButtonState != null) 104.dp else 24.dp,
      ),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (viewState.showStoragePermissionBugCard) {
        val index = entranceIndex()
        item(span = { GridItemSpan(maxLineSpan) }) {
          PermissionBugCard(onPermissionBugCardClick, cardWidth.entrance(entrance, index))
        }
      }
      val greetingIndex = entranceIndex()
      item(key = "greeting", span = { GridItemSpan(maxLineSpan) }, contentType = "greeting") {
        LibraryGreeting(
          inProgressCount = inProgressCount,
          modifier = Modifier
            .entrance(entrance, greetingIndex)
            .padding(top = 16.dp, bottom = 20.dp, start = 4.dp),
        )
      }
      if (viewState.showEmptyLibrary) {
        val index = entranceIndex()
        item(span = { GridItemSpan(maxLineSpan) }) {
          EmptyLibrary(onShowFoldersClick, Modifier.entrance(entrance, index))
        }
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
            modifier = cardWidth.entrance(entrance, index),
          )
        }
      }
      if (viewState.showSurveyCard) {
        val index = entranceIndex()
        item(key = "survey", span = { GridItemSpan(maxLineSpan) }, contentType = "survey") {
          SurveyCard(
            onClick = onSurveyClick,
            onDismiss = onSurveyDismiss,
            modifier = cardWidth
              .entrance(entrance, index)
              .padding(top = if (hero != null) 16.dp else 0.dp),
          )
        }
      }
      BookOverviewCategory.entries.forEach { category ->
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
