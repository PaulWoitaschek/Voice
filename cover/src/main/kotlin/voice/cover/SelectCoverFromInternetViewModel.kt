package voice.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.squareup.anvil.annotations.ContributesTo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import voice.common.AppScope
import voice.common.BookId
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.cover.api.CoverApi
import voice.cover.api.ImageSearchPagingSource
import voice.cover.api.SearchResponse
import voice.data.repo.BookRepository

class SelectCoverFromInternetViewModel
@AssistedInject constructor(
  private val api: CoverApi,
  private val bookRepository: BookRepository,
  private val navigator: Navigator,
  @Assisted private val bookId: BookId,
) {

  @Composable
  internal fun viewState(events: Flow<Events>): ViewState {
    var bookName: String? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
      bookName = bookRepository.get(bookId)?.content?.name
    }
    bookName ?: return ViewState.Loading

    val items = remember {
      Pager(
        config = PagingConfig(10),
        pagingSourceFactory = {
          ImageSearchPagingSource(api, "$bookName Audiobook Cover")
        },
      ).flow
    }.collectAsLazyPagingItems()

    LaunchedEffect(events) {
      events.collect {
        when (it) {
          is Events.Retry -> items.retry()
          is Events.CoverClick -> {
            navigator.goTo(Destination.EditCover(bookId, it.cover.image.toUri()))
          }
        }
      }
    }

    items.loadState.source.forEach { _, loadState ->
      if (loadState is LoadState.Error) {
        return ViewState.Error
      }
    }

    return ViewState.Content(items)
  }

  internal sealed interface ViewState {
    object Loading : ViewState
    object Error : ViewState
    data class Content(
      val items: LazyPagingItems<SearchResponse.ImageResult>,
    ) : ViewState
  }

  internal sealed interface Events {
    object Retry : Events
    data class CoverClick(val cover: SearchResponse.ImageResult) : Events
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): SelectCoverFromInternetViewModel

    @ContributesTo(AppScope::class)
    interface Provider {
      val factory: Factory
    }
  }
}
