@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

/**
 * Says what was searched and where search looks. A missing book is usually in a folder Voice doesn't know about yet,
 * so the audiobook folders are one tap away.
 */
@Composable
internal fun NoResults(
  viewState: BookSearchViewState.NoResults,
  contentPadding: PaddingValues,
  onFoldersClick: () -> Unit,
  onSearchEverythingClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(contentPadding)
      .padding(horizontal = 16.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      modifier = Modifier
        .size(128.dp)
        .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialShapes.Flower.toShape()),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = VoiceIcons.SearchOff,
        contentDescription = null,
        modifier = Modifier.size(52.dp),
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
    Text(
      text = stringResource(StringsR.string.library_search_no_results_title, viewState.query),
      modifier = Modifier.fillMaxWidth(),
      style = MaterialTheme.typography.headlineSmallEmphasized,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(StringsR.string.library_search_no_results_message),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (viewState.filtered) {
      Button(onClick = onSearchEverythingClick) {
        Icon(
          imageVector = VoiceIcons.Search,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(StringsR.string.library_search_no_results_search_everything))
      }
    }
    FilledTonalButton(onClick = onFoldersClick) {
      Icon(
        imageVector = VoiceIcons.Folder,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(stringResource(StringsR.string.library_folders_title))
    }
    Text(
      text = stringResource(StringsR.string.library_search_no_results_folders_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Preview
@Composable
private fun NoResultsPreview() {
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
      NoResults(
        viewState = BookSearchViewState.NoResults(query = "dragonlance", filtered = true),
        contentPadding = PaddingValues(),
        onFoldersClick = {},
        onSearchEverythingClick = {},
      )
    }
  }
}
