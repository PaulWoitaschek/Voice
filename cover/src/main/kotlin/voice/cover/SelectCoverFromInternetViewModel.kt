package voice.cover

import android.content.Context
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
import voice.strings.R as StringsR

class SelectCoverFromInternetViewModel
@AssistedInject constructor(
  private val api: CoverApi,
  private val bookRepository: BookRepository,
  private val navigator: Navigator,
  private val context: Context,
  private val coverDownloader: CoverDownloader,
  @Assisted private val bookId: BookId,
) {

  @Composable
  internal fun viewState(events: Flow<Events>): ViewState {
    var bookNameWithAuthor: BookNameWithAuthor? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
      val content = bookRepository.get(bookId)?.content
      bookNameWithAuthor = BookNameWithAuthor(
        bookName = content?.name ?: "",
        author = content?.author ?: "",
      )
    }
    bookNameWithAuthor ?: return ViewState.Loading("")

    var query: String by remember(bookNameWithAuthor) {
      val query = bookNameWithAuthor?.let { bookNameWithAuthor ->
        if (bookNameWithAuthor.author == null) {
          context.getString(StringsR.string.cover_search_template_no_author, bookNameWithAuthor.bookName)
        } else {
          context.getString(
            StringsR.string.cover_search_template_with_author,
            bookNameWithAuthor.bookName,
            bookNameWithAuthor.author,
          )
        }
      }
      mutableStateOf(query ?: "")
    }

    val items = remember(query) {
      Pager(
        config = PagingConfig(10),
        pagingSourceFactory = {
          ImageSearchPagingSource(api, query)
        },
      ).flow
    }.collectAsLazyPagingItems()

    LaunchedEffect(events) {
      events.collect { event ->
        when (event) {
          is Events.Retry -> items.retry()
          is Events.CoverClick -> {
            val downloaded = coverDownloader.download(event.cover.image)
              ?: coverDownloader.download(event.cover.thumbnail)
            if (downloaded != null) {
              navigator.goBack()
              navigator.goTo(Destination.EditCover(bookId, downloaded.toUri()))
            }
          }
          is Events.QueryChange -> {
            query = event.query
          }
        }
      }
    }

    items.loadState.source.forEach { _, loadState ->
      if (loadState is LoadState.Error) {
        return ViewState.Error(query)
      }
    }

    return ViewState.Content(items, query)
  }

  internal sealed interface ViewState {

    val query: String

    data class Loading(
      override val query: String,
    ) : ViewState
    data class Error(
      override val query: String,
    ) : ViewState
    data class Content(
      val items: LazyPagingItems<SearchResponse.ImageResult>,
      override val query: String,
    ) : ViewState
  }

  internal sealed interface Events {
    data object Retry : Events
    data class CoverClick(
      val cover: SearchResponse.ImageResult,
    ) : Events
    data class QueryChange(
      val query: String,
    ) : Events
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): SelectCoverFromInternetViewModel

    @ContributesTo(AppScope::class)
    interface Provider {
      val factory: Factory
    }
  }

  private data class BookNameWithAuthor(
    val bookName: String,
    val author: String?,
  )
}
