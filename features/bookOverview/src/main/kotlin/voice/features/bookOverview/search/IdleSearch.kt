package voice.features.bookOverview.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme

@Composable
internal fun IdleSearch(
  viewState: BookSearchViewState.Idle,
  contentPadding: PaddingValues,
  listener: BookSearchListener,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding),
  ) {
    val recentSearches = viewState.recentSearches ?: return@Column
    if (recentSearches.isEmpty()) {
      Text(
        text = stringResource(StringsR.string.library_search_no_results_message),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    } else {
      SectionHeader(
        title = stringResource(StringsR.string.library_search_recent_title),
        action = {
          TextButton(onClick = listener::onClearRecentSearches) {
            Text(stringResource(StringsR.string.library_search_recent_clear_all))
          }
        },
      )
      RecentSearches(
        recentSearches = recentSearches,
        onClick = listener::onRecentSearchClick,
        onRemove = listener::onRemoveRecentSearch,
      )
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  action: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      modifier = Modifier.weight(1F),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    action()
  }
}

@Composable
private fun RecentSearches(
  recentSearches: List<String>,
  onClick: (String) -> Unit,
  onRemove: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier.padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    recentSearches.forEach { recentSearch ->
      InputChip(
        selected = false,
        onClick = { onClick(recentSearch) },
        label = {
          Text(
            text = recentSearch,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        avatar = {
          Icon(
            painter = painterResource(UiR.drawable.ic_history),
            contentDescription = null,
            modifier = Modifier.size(InputChipDefaults.AvatarSize),
          )
        },
        trailingIcon = {
          Icon(
            painter = painterResource(UiR.drawable.ic_close),
            contentDescription =
              stringResource(StringsR.string.library_search_recent_remove, recentSearch),
            modifier =
              Modifier.size(InputChipDefaults.IconSize).clip(CircleShape).clickable(
                role = Role.Button,
              ) {
                onRemove(recentSearch)
              },
          )
        },
      )
    }
  }
}

@Preview
@Composable
private fun IdleSearchPreview() {
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
      IdleSearch(
        viewState =
          BookSearchViewState.Idle(recentSearches = listOf("king", "hail mary", "kate reading")),
        contentPadding = PaddingValues(12.dp),
        listener = BookSearchListener.Noop,
      )
    }
  }
}

@Preview
@Composable
private fun IdleSearchPreviewNoRecentSearches() {
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
      IdleSearch(
        viewState = BookSearchViewState.Idle(recentSearches = emptyList()),
        contentPadding = PaddingValues(12.dp),
        listener = BookSearchListener.Noop,
      )
    }
  }
}
