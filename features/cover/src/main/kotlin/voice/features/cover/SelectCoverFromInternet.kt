package voice.features.cover

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.VoiceTheme
import voice.core.ui.rememberScoped
import voice.features.cover.api.SearchResponse
import voice.navigation.Destination
import voice.navigation.NavEntryProvider

@ContributesTo(AppScope::class)
interface SelectCoverFromInternetProvider {

  @Provides
  @IntoSet
  fun navEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.CoverFromInternet> { key ->
    NavEntry(key) {
      SelectCoverFromInternet(
        bookId = key.bookId,
      )
    }
  }
}

@Composable
fun SelectCoverFromInternet(bookId: BookId) {
  val viewModel = rememberScoped(bookId.value) {
    rootGraphAs<SelectCoverFromInternetViewModel.Factory.Provider>()
      .factory
      .create(bookId)
  }

  val sink = MutableSharedFlow<SelectCoverFromInternetViewModel.Events>(extraBufferCapacity = 1)
  SelectCoverFromInternet(
    viewState = viewModel.viewState(sink),
    onCloseClick = viewModel::onCloseClick,
    onCoverClick = {
      sink.tryEmit(SelectCoverFromInternetViewModel.Events.CoverClick(it))
    },
    onRetry = {
      sink.tryEmit(SelectCoverFromInternetViewModel.Events.Retry)
    },
    onQueryChange = {
      sink.tryEmit(SelectCoverFromInternetViewModel.Events.QueryChange(it))
    },
  )
}

@Composable
private fun SelectCoverFromInternet(
  viewState: SelectCoverFromInternetViewModel.ViewState,
  onCloseClick: () -> Unit,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
  onRetry: () -> Unit,
  onQueryChange: (String) -> Unit,
) {
  Box {
    CoverSearchBar(
      onCloseClick = onCloseClick,
      onQueryChange = onQueryChange,
      viewState = viewState,
    )
    CoverContents(
      viewState = viewState,
      onCoverClick = onCoverClick,
      onRetry = onRetry,
    )
  }
}

@Preview
@Composable
private fun ErrorPreview() {
  VoiceTheme {
    Surface {
      SelectCoverFromInternet(
        viewState = SelectCoverFromInternetViewModel.ViewState.Error("Searching..."),
        onCloseClick = {},
        onCoverClick = {},
        onRetry = {},
        onQueryChange = {},
      )
    }
  }
}

@Preview
@Composable
private fun ListPreview() {
  VoiceTheme {
    Surface {
      val element = SearchResponse.ImageResult(
        width = 100,
        height = 100,
        image = "image",
        thumbnail = "thumb",
      )
      val items = MutableStateFlow(
        PagingData.from(
          buildList<SearchResponse.ImageResult> {
            repeat(10) {
              add(element)
            }
          },
        ),
      ).collectAsLazyPagingItems()
      SelectCoverFromInternet(
        viewState = SelectCoverFromInternetViewModel.ViewState.Content(
          items = items,
          query = "Search Term",
        ),
        onCloseClick = {},
        onCoverClick = {},
        onRetry = {},
        onQueryChange = {},
      )
    }
  }
}
