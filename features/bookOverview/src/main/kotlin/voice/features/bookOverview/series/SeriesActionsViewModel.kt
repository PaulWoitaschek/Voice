package voice.features.bookOverview.series

import androidx.compose.runtime.Composable
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
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.bottomSheet.BottomSheetItem
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
  private val navigator: Navigator,
  dispatcherProvider: DispatcherProvider,
  @Assisted private val name: String,
) {

  private val scope = MainScope(dispatcherProvider)

  @Composable
  internal fun state(): SeriesStackViewState? {
    val books = remember { repo.flow() }.collectAsState(initial = null).value ?: return null
    val series = remember(books) { books.inSeries(name).series().singleOrNull() }
    // with no series there is no menu to show, and an invisible one would swallow the next back
    // press
    LaunchedEffect(series == null) {
      if (series == null) onClose()
    }
    return series?.toStackViewState()
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
          else -> {}
        }
      }
      .join()
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
