package voice.cover

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.MutableSharedFlow
import voice.common.BookId
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.cover.api.SearchResponse
import voice.strings.R as StringsR

@Composable
fun SelectCoverFromInternet(
  bookId: BookId,
  onCloseClick: () -> Unit,
) {
  val viewModel = rememberScoped(bookId.value) {
    rootComponentAs<SelectCoverFromInternetViewModel.Factory.Provider>()
      .factory
      .create(bookId)
  }

  val sink = MutableSharedFlow<SelectCoverFromInternetViewModel.Events>(extraBufferCapacity = 1)
  SelectCoverFromInternet(
    viewState = viewModel.viewState(sink),
    onCloseClick = onCloseClick,
    onCoverClick = {
      sink.tryEmit(SelectCoverFromInternetViewModel.Events.CoverClick(it))
    },
    onRetry = {
      sink.tryEmit(SelectCoverFromInternetViewModel.Events.Retry)
    },
  )
}

@Composable
private fun SelectCoverFromInternet(
  viewState: SelectCoverFromInternetViewModel.ViewState,
  onCloseClick: () -> Unit,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
  onRetry: () -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
          Text(stringResource(StringsR.string.cover))
        },
        navigationIcon = {
          IconButton(onClick = onCloseClick) {
            Icon(
              imageVector = Icons.Outlined.ArrowBack,
              contentDescription = stringResource(StringsR.string.close),
            )
          }
        },
      )
    },
    content = { contentPadding ->
      when (viewState) {
        is SelectCoverFromInternetViewModel.ViewState.Content -> {
          val items = viewState.items
          LazyVerticalStaggeredGrid(
            contentPadding = contentPadding,
            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
            content = {
              items(
                count = items.itemCount,
                key = null,
              ) { index ->
                val item = items[index]
                if (item != null) {
                  Image(
                    modifier = Modifier
                      .clickable {
                        onCoverClick(item)
                        onCloseClick()
                      }
                      .fillMaxWidth()
                      .aspectRatio(item.width.toFloat() / item.height),
                    painter = rememberAsyncImagePainter(model = item.thumbnail), contentDescription = null,
                  )
                }
              }
            },
          )
        }
        SelectCoverFromInternetViewModel.ViewState.Loading -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(contentPadding),
          ) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
          }
        }
        SelectCoverFromInternetViewModel.ViewState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = stringResource(id = StringsR.string.generic_error_message),
              style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = onRetry) {
              Text(text = stringResource(id = StringsR.string.generic_error_retry))
            }
          }
        }
      }
    },
  )
}

@Preview
@Composable
private fun ErrorPreview() {
  VoiceTheme {
    SelectCoverFromInternet(
      viewState = SelectCoverFromInternetViewModel.ViewState.Error,
      onCloseClick = {},
      onCoverClick = {},
      onRetry = {},
    )
  }
}
