@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.features.bookOverview.search.searchBarSharedBounds
import kotlin.time.Duration.Companion.seconds
import voice.core.strings.R as StringsR

@Composable
internal fun LibraryTopBar(
  showSearch: Boolean,
  showFolderPickerIcon: Boolean,
  showAddBookHint: Boolean,
  isLoading: Boolean,
  scrollBehavior: SearchBarScrollBehavior?,
  onSearchClick: () -> Unit,
  onBookFolderClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showLoading by remember { mutableStateOf(false) }
  LaunchedEffect(isLoading) {
    if (isLoading) {
      delay(3.seconds)
    }
    showLoading = isLoading
  }
  Box(modifier) {
    AppBarWithSearch(
      state = rememberSearchBarState(),
      inputField = {
        AnimatedVisibility(
          visible = showSearch,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          LibrarySearchPill(loading = showLoading, onClick = onSearchClick)
        }
      },
      actions = {
        if (showFolderPickerIcon) {
          BookFolderIcon(withHint = showAddBookHint, onClick = onBookFolderClick)
        }
        SettingsIcon(onSettingsClick)
      },
      // the pill draws its own container, so it can grow into the search field
      colors = SearchBarDefaults.appBarWithSearchColors(
        searchBarColors = SearchBarDefaults.colors(containerColor = Color.Transparent),
        scrolledSearchBarContainerColor = Color.Transparent,
      ),
      scrollBehavior = scrollBehavior,
    )
    // without books there's no pill to show the scan in. Drawn over the bar's bottom edge so it
    // doesn't push the library down.
    AnimatedVisibility(
      visible = showLoading && !showSearch,
      modifier = Modifier
        .matchParentSize()
        .wrapContentHeight(Alignment.Bottom),
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
    }
  }
}

@Composable
private fun LibrarySearchPill(
  loading: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = SearchBarDefaults.inputFieldShape
  Surface(
    onClick = onClick,
    modifier = modifier
      .searchBarSharedBounds(shape)
      .fillMaxWidth()
      .height(SearchBarDefaults.InputFieldHeight),
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Crossfade(targetState = loading, modifier = Modifier.size(24.dp)) { showSpinner ->
        if (showSpinner) {
          LoadingIndicator()
        } else {
          Icon(imageVector = VoiceIcons.Search, contentDescription = null)
        }
      }
      Spacer(Modifier.width(16.dp))
      Text(
        text = stringResource(StringsR.string.library_search_hint),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Preview
@Composable
private fun LibraryTopBarPreview() {
  VoiceTheme {
    LibraryTopBar(
      showSearch = true,
      showFolderPickerIcon = true,
      showAddBookHint = false,
      isLoading = false,
      scrollBehavior = null,
      onSearchClick = {},
      onBookFolderClick = {},
      onSettingsClick = {},
    )
  }
}

@Preview
@Composable
private fun LibrarySearchPillLoadingPreview() {
  VoiceTheme {
    LibrarySearchPill(loading = true, onClick = {})
  }
}
