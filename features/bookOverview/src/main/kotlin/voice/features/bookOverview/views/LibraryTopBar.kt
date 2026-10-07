@file:OptIn(ExperimentalMaterial3Api::class)

package voice.features.bookOverview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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

/**
 * The library's app bar: a search pill that opens the search, with the settings next to it. It hides while
 * scrolling down the library and comes back when scrolling up.
 */
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
  Column(modifier) {
    AppBarWithSearch(
      state = rememberSearchBarState(),
      inputField = {
        AnimatedVisibility(
          visible = showSearch,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          LibrarySearchPill(onClick = onSearchClick)
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
    var showLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
      if (isLoading) {
        delay(3.seconds)
      }
      showLoading = isLoading
    }
    if (showLoading) {
      LinearProgressIndicator(
        Modifier
          .padding(top = 4.dp)
          .fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun LibrarySearchPill(
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
      Icon(imageVector = VoiceIcons.Search, contentDescription = null)
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
