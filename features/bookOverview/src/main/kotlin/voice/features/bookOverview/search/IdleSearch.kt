@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.segmentedShape
import voice.core.strings.R as StringsR

/**
 * Search with nothing typed: the recent searches, and the library to browse by authors, series, narrators or genres.
 */
@Composable
internal fun IdleSearch(
  viewState: BookSearchViewState.Idle,
  contentPadding: PaddingValues,
  listener: BookSearchListener,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    if (viewState.recentSearches.isNotEmpty()) {
      item(key = "recentHeader", contentType = "header") {
        SectionHeader(
          title = stringResource(StringsR.string.library_search_recent_title),
          action = {
            TextButton(onClick = listener::onClearRecentSearches) {
              Text(stringResource(StringsR.string.library_search_recent_clear_all))
            }
          },
        )
      }
      item(key = "recent", contentType = "recent") {
        RecentSearches(
          recentSearches = viewState.recentSearches,
          onClick = listener::onRecentSearchClick,
          onRemove = listener::onRemoveRecentSearch,
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
    }
    val browse = viewState.browse
    if (browse != null) {
      item(key = "browseHeader", contentType = "header") {
        SectionHeader(title = stringResource(StringsR.string.library_search_browse_title))
      }
      item(key = "browseTabs", contentType = "browseTabs") {
        BrowseTabs(
          categories = browse.categories,
          selected = browse.selectedCategory,
          onClick = listener::onBrowseCategoryClick,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }
      itemsIndexed(
        items = browse.entries,
        key = { _, entry -> "browse-${browse.selectedCategory}-${entry.name}" },
        contentType = { _, _ -> "browseEntry" },
      ) { index, entry ->
        BrowseRow(
          entry = entry,
          index = index,
          shape = segmentedShape(index, browse.entries.size),
          onClick = { listener.onBrowseEntryClick(browse.selectedCategory, entry.name) },
        )
      }
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  action: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 40.dp)
      .padding(start = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      modifier = Modifier.weight(1F),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    action?.invoke()
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
            imageVector = VoiceIcons.History,
            contentDescription = null,
            modifier = Modifier.size(InputChipDefaults.AvatarSize),
          )
        },
        trailingIcon = {
          Icon(
            imageVector = VoiceIcons.Close,
            contentDescription = stringResource(StringsR.string.library_search_recent_remove, recentSearch),
            modifier = Modifier
              .size(InputChipDefaults.IconSize)
              .clip(CircleShape)
              .clickable(role = Role.Button) { onRemove(recentSearch) },
          )
        },
      )
    }
  }
}

@Composable
private fun BrowseTabs(
  categories: List<BrowseCategory>,
  selected: BrowseCategory,
  onClick: (BrowseCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    categories.forEachIndexed { index, category ->
      ToggleButton(
        checked = category == selected,
        onCheckedChange = { onClick(category) },
        modifier = Modifier
          .weight(1F)
          .semantics { role = Role.RadioButton },
        shapes = when (index) {
          0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
          categories.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
          else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        contentPadding = PaddingValues(horizontal = 8.dp),
      ) {
        Text(
          text = category.field.label(),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

private val BrowseShapes = listOf(
  MaterialShapes.Cookie9Sided,
  MaterialShapes.Sunny,
  MaterialShapes.Clover4Leaf,
  MaterialShapes.Flower,
)

@Composable
private fun BrowseRow(
  entry: BookSearchViewState.BrowseEntry,
  index: Int,
  shape: Shape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceBright,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val (containerColor, contentColor) = browseColors(index)
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(containerColor, BrowseShapes[index % BrowseShapes.size].toShape()),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = initials(entry.name),
          style = MaterialTheme.typography.labelLargeEmphasized,
          color = contentColor,
        )
      }
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = entry.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = browseSummary(entry),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = VoiceIcons.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun browseColors(index: Int): Pair<Color, Color> {
  val colors = MaterialTheme.colorScheme
  return when (index % 3) {
    0 -> colors.primaryContainer to colors.onPrimaryContainer
    1 -> colors.tertiaryContainer to colors.onTertiaryContainer
    else -> colors.secondaryContainer to colors.onSecondaryContainer
  }
}

@Composable
private fun browseSummary(entry: BookSearchViewState.BrowseEntry): String {
  return buildList {
    add(pluralStringResource(StringsR.plurals.library_search_browse_book_count, entry.bookCount, entry.bookCount))
    if (entry.inProgressCount > 0) {
      add(
        pluralStringResource(
          StringsR.plurals.library_search_browse_in_progress,
          entry.inProgressCount,
          entry.inProgressCount,
        ),
      )
    }
    if (entry.finishedCount > 0) {
      add(
        pluralStringResource(
          StringsR.plurals.library_search_browse_finished,
          entry.finishedCount,
          entry.finishedCount,
        ),
      )
    }
  }.joinToString(separator = " · ")
}

private fun initials(name: String): String {
  val words = name.split(' ').filter { word -> word.firstOrNull()?.isLetterOrDigit() == true }
  return words.take(2)
    .joinToString(separator = "") { it.first().uppercase() }
    // a whole code point, so an emoji isn't cut in half
    .ifEmpty { name.take(name.offsetByCodePoints(0, minOf(1, name.length))).uppercase() }
}

@Preview
@Composable
private fun IdleSearchPreview() {
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
      IdleSearch(
        viewState = BookSearchViewState.Idle(
          recentSearches = listOf("king", "hail mary", "kate reading"),
          browse = BookSearchViewState.Browse(
            categories = BrowseCategory.entries,
            selectedCategory = BrowseCategory.Authors,
            entries = listOf(
              BookSearchViewState.BrowseEntry("Brandon Sanderson", 4, 1, 0),
              BookSearchViewState.BrowseEntry("Stephen King", 2, 0, 1),
              BookSearchViewState.BrowseEntry("Patrick Rothfuss", 2, 0, 0),
              BookSearchViewState.BrowseEntry("Susanna Clarke", 1, 1, 0),
            ),
          ),
        ),
        contentPadding = PaddingValues(12.dp),
        listener = BookSearchListener.Noop,
      )
    }
  }
}
