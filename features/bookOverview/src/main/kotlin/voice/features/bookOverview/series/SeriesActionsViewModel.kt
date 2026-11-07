package voice.features.bookOverview.series

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.BookCategory
import voice.core.data.category
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.category
import voice.features.bookOverview.bottomSheet.markAsItem
import voice.features.bookOverview.editBookCategory.BookCategoryChanger
import voice.features.bookOverview.hideBook.BookHider
import voice.features.bookOverview.overview.SeriesStackViewState
import voice.features.bookOverview.overview.inSeries
import voice.features.bookOverview.overview.series
import voice.features.bookOverview.overview.toStackViewState
import voice.navigation.Destination
import voice.navigation.Navigator

@AssistedInject
class SeriesActionsViewModel(
  private val repo: BookRepository,
  private val bookHider: BookHider,
  private val bookCategoryChanger: BookCategoryChanger,
  private val navigator: Navigator,
  dispatcherProvider: DispatcherProvider,
  @Assisted private val name: String,
) {

  private val scope = MainScope(dispatcherProvider)

  @Composable
  internal fun state(): SeriesActionsViewState? {
    val books = remember { repo.flow() }.collectAsState(initial = null).value ?: return null
    val series = remember(books) { books.inSeries(name).series().singleOrNull() }
    // with no series there is no menu to show, and an invisible one would swallow the next back
    // press
    LaunchedEffect(series == null) {
      if (series == null) onClose()
    }
    if (series == null) return null
    return SeriesActionsViewState(
      series = series.toStackViewState(),
      statuses =
        StatusOrder.mapNotNull { category ->
          val changes = series.books.filter { it.category != category }
          if (changes.isEmpty()) return@mapNotNull null
          SeriesActionsViewState.Status(
            item = category.markAsItem(),
            // a book not started yet has no place to lose, so it needs no asking
            resets = changes.count { it.category != BookCategory.NOT_STARTED },
          )
        },
    )
  }

  /**
   * Returns once the item is handled. The work itself runs in this scope, so it completes even if
   * the caller goes away.
   */
  internal suspend fun onItemClick(item: BottomSheetItem) {
    scope
      .launch {
        when (item) {
          BottomSheetItem.Hide -> bookHider.hideSeries(name)
          BottomSheetItem.Show -> bookHider.showSeries(name)
          else -> item.category?.let { markSeries(it) }
        }
      }
      .join()
  }

  private suspend fun markSeries(category: BookCategory) {
    repo
      .all()
      .inSeries(name)
      .filter { it.category != category }
      .forEach { bookCategoryChanger.change(it.id, category) }
  }

  internal fun onClose() {
    navigator.remove(Destination.SeriesActions(name))
  }

  @AssistedFactory
  interface Factory {
    fun create(name: String): SeriesActionsViewModel

    @ContributesTo(AppScope::class)
    interface Provider {
      val seriesActionsViewModelFactory: Factory
    }
  }
}

private val StatusOrder = listOf(BookCategory.NOT_STARTED, BookCategory.FINISHED)

@Immutable
internal data class SeriesActionsViewState(
  val series: SeriesStackViewState,
  val statuses: List<Status>,
) {

  data class Status(
    val item: BottomSheetItem,
    val resets: Int,
  )
}
