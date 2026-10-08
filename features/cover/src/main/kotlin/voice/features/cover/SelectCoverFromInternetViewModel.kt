package voice.features.cover

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.cover.api.CoverApi
import voice.features.cover.api.ImageSearchPagingSource
import voice.features.cover.api.SearchResponse
import voice.navigation.Destination
import voice.navigation.Navigator
import kotlin.time.Duration.Companion.milliseconds
import voice.core.strings.R as StringsR

@AssistedInject
class SelectCoverFromInternetViewModel(
  private val api: CoverApi,
  private val bookRepository: BookRepository,
  private val navigator: Navigator,
  private val context: Context,
  private val coverDownloader: CoverDownloader,
  @Assisted private val bookId: BookId,
) {

  val query = TextFieldState()
  private var queryInitialized = false

  @Composable
  internal fun viewState(events: Flow<Events>): ViewState {
    var searchQuery: String? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
      if (!queryInitialized) {
        query.setTextAndPlaceCursorAtEnd(initialQuery())
        queryInitialized = true
      }
      snapshotFlow { query.text.toString() }.collectLatest { text ->
        if (searchQuery != null) {
          delay(SEARCH_DEBOUNCE)
        }
        searchQuery = text
      }
    }

    val items = remember(searchQuery) {
      val searchFor = searchQuery.orEmpty()
      Pager(
        config = PagingConfig(10),
        pagingSourceFactory = {
          ImageSearchPagingSource(api, searchFor)
        },
      ).flow
    }.collectAsLazyPagingItems()
    // the events are collected for the whole screen, while each query gets new items
    val currentItems by rememberUpdatedState(items)

    var downloading: SearchResponse.ImageResult? by remember { mutableStateOf(null) }
    var downloadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(events) {
      events.collect { event ->
        when (event) {
          is Events.Retry -> currentItems.retry()
          is Events.CoverClick -> {
            if (downloading == null) {
              downloading = event.cover
              // apart from the collection, so retrying or searching isn't held up by a slow download
              launch {
                val downloaded = coverDownloader.download(event.cover.image)
                  ?: coverDownloader.download(event.cover.thumbnail)
                downloading = null
                if (downloaded != null) {
                  navigator.goBack()
                  navigator.goTo(Destination.EditCover(bookId, downloaded.toUri()))
                } else {
                  downloadFailed = true
                }
              }
            }
          }
          is Events.Search -> {
            val text = query.text.toString()
            if (text == searchQuery) {
              currentItems.refresh()
            } else {
              searchQuery = text
            }
          }
          is Events.DownloadErrorShown -> {
            downloadFailed = false
          }
        }
      }
    }

    val currentSearchQuery = searchQuery
    val loadState = items.loadState
    val results = when {
      currentSearchQuery == null -> Results.Loading
      currentSearchQuery.isBlank() -> Results.Idle
      loadState.refresh is LoadState.Error -> Results.Error
      items.itemCount == 0 && loadState.append.endOfPaginationReached -> Results.Empty
      items.itemCount == 0 -> Results.Loading
      else -> Results.Content(
        items = items,
        loadingMore = loadState.append is LoadState.Loading,
        loadingMoreFailed = loadState.append is LoadState.Error,
      )
    }
    return ViewState(
      results = results,
      downloading = downloading,
      downloadFailed = downloadFailed,
    )
  }

  private suspend fun initialQuery(): String {
    val content = bookRepository.get(bookId)?.content ?: return ""
    val author = content.author
    return if (author == null) {
      context.getString(StringsR.string.cover_search_query_without_author, content.name)
    } else {
      context.getString(StringsR.string.cover_search_query_with_author, content.name, author)
    }
  }

  fun onCloseClick() {
    navigator.goBack()
  }

  internal data class ViewState(
    val results: Results,
    val downloading: SearchResponse.ImageResult?,
    val downloadFailed: Boolean,
  )

  internal sealed interface Results {
    data object Idle : Results
    data object Loading : Results
    data object Error : Results
    data object Empty : Results
    data class Content(
      val items: LazyPagingItems<SearchResponse.ImageResult>,
      val loadingMore: Boolean,
      val loadingMoreFailed: Boolean,
    ) : Results
  }

  internal sealed interface Events {
    data object Retry : Events
    data class CoverClick(val cover: SearchResponse.ImageResult) : Events
    data object Search : Events
    data object DownloadErrorShown : Events
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

private val SEARCH_DEBOUNCE = 600.milliseconds
