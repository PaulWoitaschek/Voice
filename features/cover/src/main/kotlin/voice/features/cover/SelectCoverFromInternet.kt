package voice.features.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.VoiceTheme
import voice.features.cover.SelectCoverFromInternetViewModel.Events
import voice.features.cover.SelectCoverFromInternetViewModel.ViewState
import voice.features.cover.api.SearchResponse
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@BindingContainer
@ContributesTo(AppScope::class)
object SelectCoverFromInternetProvider {

  @Provides
  @IntoSet
  fun selectCoverFromInternetNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.CoverFromInternet> { key ->
    NavEntry(key) {
      SelectCoverFromInternet(
        bookId = key.bookId,
      )
    }
  }
}

@Composable
fun SelectCoverFromInternet(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<SelectCoverFromInternetViewModel.Factory.Provider>()
      .factory
      .create(bookId)
  }

  // a new flow would restart the view model's event collection and cancel a running download
  val sink = remember { MutableSharedFlow<Events>(extraBufferCapacity = 1) }
  SelectCoverFromInternet(
    viewState = viewModel.viewState(sink),
    query = viewModel.query,
    onCloseClick = viewModel::onCloseClick,
    onCoverClick = { sink.tryEmit(Events.CoverClick(it)) },
    onRetry = { sink.tryEmit(Events.Retry) },
    onSearch = { sink.tryEmit(Events.Search) },
    onDownloadErrorDismiss = { sink.tryEmit(Events.DownloadErrorShown) },
  )
}

@Composable
private fun SelectCoverFromInternet(
  viewState: ViewState,
  query: TextFieldState,
  onCloseClick: () -> Unit,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
  onRetry: () -> Unit,
  onSearch: () -> Unit,
  onDownloadErrorDismiss: () -> Unit,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  // scrolling the covers means looking at them, so the keyboard makes room
  val hideKeyboardOnScroll = remember(keyboard) {
    object : NestedScrollConnection {
      override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
      ): Offset {
        if (source == NestedScrollSource.UserInput && available.y != 0F) {
          keyboard?.hide()
        }
        return Offset.Zero
      }
    }
  }
  val snackbarHostState = remember { SnackbarHostState() }
  val downloadFailed = viewState is ViewState.Content && viewState.downloadFailed
  val downloadFailedMessage = stringResource(StringsR.string.cover_search_download_failed)
  val currentOnDownloadErrorDismiss by rememberUpdatedState(onDownloadErrorDismiss)
  LaunchedEffect(downloadFailed) {
    if (downloadFailed) {
      snackbarHostState.showSnackbar(downloadFailedMessage)
      currentOnDownloadErrorDismiss()
    }
  }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface),
  ) {
    Column(Modifier.statusBarsPadding()) {
      CoverSearchBar(
        query = query,
        onSearch = {
          keyboard?.hide()
          onSearch()
        },
        onCloseClick = onCloseClick,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
      )
      Surface(
        modifier = Modifier
          .fillMaxSize()
          .nestedScroll(hideKeyboardOnScroll),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      ) {
        CoverContents(
          viewState = viewState,
          contentPadding = coverContentPadding(),
          onCoverClick = { cover ->
            keyboard?.hide()
            onCoverClick(cover)
          },
          onRetry = onRetry,
        )
      }
    }
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    )
  }
}

@Composable
private fun coverContentPadding(): PaddingValues {
  val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues()
  return PaddingValues(
    start = 12.dp,
    end = 12.dp,
    top = 12.dp,
    bottom = 16.dp + bottomInsets.calculateBottomPadding(),
  )
}

@Preview
@Composable
private fun ErrorPreview() {
  VoiceTheme {
    SelectCoverFromInternet(
      viewState = ViewState.Error,
      query = TextFieldState("Dune by Frank Herbert audiobook cover"),
      onCloseClick = {},
      onCoverClick = {},
      onRetry = {},
      onSearch = {},
      onDownloadErrorDismiss = {},
    )
  }
}

@Preview
@Composable
private fun EmptyPreview() {
  VoiceTheme {
    SelectCoverFromInternet(
      viewState = ViewState.Empty,
      query = TextFieldState("Dune by Frank Herbert audiobook cover"),
      onCloseClick = {},
      onCoverClick = {},
      onRetry = {},
      onSearch = {},
      onDownloadErrorDismiss = {},
    )
  }
}

@Preview
@Composable
private fun ListPreview() {
  VoiceTheme {
    val items = remember {
      MutableStateFlow(
        PagingData.from(
          List(10) { index ->
            SearchResponse.ImageResult(
              width = 600,
              height = if (index % 3 == 0) 900 else 600,
              image = "image$index",
              thumbnail = "thumb$index",
            )
          },
        ),
      )
    }.collectAsLazyPagingItems()
    SelectCoverFromInternet(
      viewState = ViewState.Content(
        items = items,
        loadingMore = true,
        loadingMoreFailed = false,
        downloading = null,
        downloadFailed = false,
      ),
      query = TextFieldState("Dune by Frank Herbert audiobook cover"),
      onCloseClick = {},
      onCoverClick = {},
      onRetry = {},
      onSearch = {},
      onDownloadErrorDismiss = {},
    )
  }
}
