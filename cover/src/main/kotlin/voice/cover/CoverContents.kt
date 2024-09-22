package voice.cover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.cover.api.SearchResponse
import voice.strings.R

@Composable
internal fun CoverContents(
  viewState: SelectCoverFromInternetViewModel.ViewState,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
  onRetry: () -> Unit,
) {
  when (viewState) {
    is SelectCoverFromInternetViewModel.ViewState.Content -> {
      ItemsContent(viewState, onCoverClick)
    }
    is SelectCoverFromInternetViewModel.ViewState.Loading -> {
      LoadingContent()
    }
    is SelectCoverFromInternetViewModel.ViewState.Error -> {
      ErrorContent(onRetry)
    }
  }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = stringResource(id = R.string.generic_error_message),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.size(16.dp))
    Button(onClick = onRetry) {
      Text(text = stringResource(id = R.string.generic_error_retry))
    }
  }
}

@Composable
private fun LoadingContent() {
  Box(
    modifier = Modifier
      .fillMaxSize(),
  ) {
    CircularProgressIndicator(Modifier.align(Alignment.Center))
  }
}

@Composable
private fun ItemsContent(
  viewState: SelectCoverFromInternetViewModel.ViewState.Content,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
) {
  val items = viewState.items
  LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
    contentPadding = PaddingValues(top = 96.dp),
    content = {
      items(
        count = items.itemCount,
        key = null,
      ) { index ->
        val item = items[index]
        if (item != null) {
          CoverImage(onCoverClick = onCoverClick, item = item)
        }
      }
    },
  )
}

@Composable
private fun CoverImage(
  item: SearchResponse.ImageResult,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
) {
  AsyncImage(
    modifier = Modifier
      .clickable {
        onCoverClick(item)
      }
      .fillMaxWidth()
      .aspectRatio(item.width.toFloat() / item.height),
    model = item.thumbnail,
    contentDescription = null,
    placeholder = ColorPainter(MaterialTheme.colorScheme.onSurfaceVariant),
  )
}
