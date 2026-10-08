@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookmark

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import voice.core.data.Bookmark
import voice.core.strings.R
import voice.core.ui.BookBar
import voice.core.ui.BookmarkBadge
import voice.core.ui.NowPlayingBars
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.segmentedShape
import java.text.NumberFormat

@Composable
internal fun BookmarkList(
  viewState: BookmarkViewState,
  listState: LazyListState,
  contentPadding: PaddingValues,
  onCategoryClick: (BookmarkCategory?) -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  onLongClick: (Bookmark.Id) -> Unit,
  onDelete: (Bookmark.Id) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  LazyColumn(
    state = listState,
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      end = 16.dp,
      top = contentPadding.calculateTopPadding(),
      bottom = contentPadding.calculateBottomPadding() + 96.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    item(key = "book-bar") {
      val bookBar = viewState.bookBar
      if (bookBar != null) {
        BookBarHeader(
          bookBar = bookBar,
          onPinClick = { pin ->
            val key = bookBar.pinKeys[pin]
            val index = viewState.items.indexOfFirst { it.key == key }
            if (index != -1) {
              scope.launch { listState.animateScrollToItem(index + HEADER_ITEMS) }
            }
          },
        )
      }
    }
    item(key = "categories") {
      if (viewState.categories.size >= 2) {
        CategoryChips(
          categories = viewState.categories,
          total = viewState.totalCount,
          selected = viewState.selectedCategory,
          onClick = onCategoryClick,
        )
      }
    }
    if (viewState.totalCount == 0) {
      item(key = "empty") {
        EmptyHero(
          Modifier
            .animateItem()
            .padding(top = 32.dp, bottom = 24.dp),
        )
      }
    }
    items(viewState.items, key = { it.key }, contentType = { it::class }) { item ->
      when (item) {
        is BookmarkListItem.ChapterHeader -> ChapterHeader(item, Modifier.animateItem())
        is BookmarkListItem.Row -> BookmarkRow(
          row = item.bookmark,
          group = item.group,
          onClick = onClick,
          onLongClick = onLongClick,
          onDelete = onDelete,
          modifier = Modifier.animateItem(),
        )
        is BookmarkListItem.YouAreHere -> YouAreHereRow(
          item = item,
          playing = viewState.playing,
          modifier = Modifier.animateItem(),
        )
      }
    }
  }
}

/** The book bar and the chips come before the bookmarks. */
internal const val HEADER_ITEMS = 2

@Composable
private fun BookBarHeader(
  bookBar: BookBarViewState,
  onPinClick: (Int) -> Unit,
) {
  val percentFormat = remember { NumberFormat.getPercentInstance() }
  Column(Modifier.padding(top = 8.dp, bottom = 8.dp)) {
    BookBar(
      segments = bookBar.segments,
      currentSegment = bookBar.currentSegment,
      progress = bookBar.progress,
      pins = bookBar.pins,
      showPositionMarker = true,
      pinSize = 14.dp,
      onPinClick = onPinClick,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(R.string.playback_book_progress_read, percentFormat.format(bookBar.percent / 100.0)),
        style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.weight(1F))
      Text(
        text = stringResource(R.string.playback_book_progress_remaining, bookBar.remaining),
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CategoryChips(
  categories: List<CategoryCount>,
  total: Int,
  selected: BookmarkCategory?,
  onClick: (BookmarkCategory?) -> Unit,
) {
  val locale = LocalConfiguration.current.locales[0]
  val countFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(bottom = 8.dp),
  ) {
    item {
      FilterChip(
        selected = selected == null,
        onClick = { onClick(null) },
        label = { ChipLabel(stringResource(R.string.bookmark_category_all), countFormat.format(total.toLong())) },
      )
    }
    items(categories, key = { it.category }) { (category, count) ->
      FilterChip(
        selected = selected == category,
        onClick = { onClick(category) },
        label = { ChipLabel(categoryLabel(category), countFormat.format(count.toLong())) },
        leadingIcon = {
          val (kind, sleep) = category.badge()
          BookmarkBadge(kind = kind, setBySleepTimer = sleep, size = 18.dp)
        },
      )
    }
  }
}

@Composable
private fun ChipLabel(
  label: String,
  count: String,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(label)
    Text(count)
  }
}

internal fun BookmarkCategory.badge(): Pair<Bookmark.Kind, Boolean> = when (this) {
  BookmarkCategory.Note -> Bookmark.Kind.Note to false
  BookmarkCategory.Favorite -> Bookmark.Kind.Favorite to false
  BookmarkCategory.Quote -> Bookmark.Kind.Quote to false
  BookmarkCategory.Revisit -> Bookmark.Kind.Revisit to false
  BookmarkCategory.Sleep -> Bookmark.Kind.Note to true
}

@Composable
private fun categoryLabel(category: BookmarkCategory): String = stringResource(
  when (category) {
    BookmarkCategory.Note -> R.string.bookmark_category_notes
    BookmarkCategory.Favorite -> R.string.bookmark_category_favorites
    BookmarkCategory.Quote -> R.string.bookmark_category_quotes
    BookmarkCategory.Revisit -> R.string.bookmark_category_revisit
    BookmarkCategory.Sleep -> R.string.bookmark_dozed_off
  },
)

@Composable
internal fun kindLabel(
  kind: Bookmark.Kind,
  setBySleepTimer: Boolean,
): String = stringResource(
  if (setBySleepTimer) {
    R.string.bookmark_dozed_off
  } else {
    when (kind) {
      Bookmark.Kind.Note -> R.string.bookmark_kind_note
      Bookmark.Kind.Favorite -> R.string.bookmark_kind_favorite
      Bookmark.Kind.Quote -> R.string.bookmark_kind_quote
      Bookmark.Kind.Revisit -> R.string.bookmark_kind_revisit
    }
  },
)

@Composable
internal fun chapterLabel(
  number: Int,
  name: String?,
): String = if (name != null) {
  stringResource(R.string.bookmark_chapter_with_name, number, name)
} else {
  stringResource(R.string.bookmark_chapter, number)
}

@Composable
private fun ChapterHeader(
  header: BookmarkListItem.ChapterHeader,
  modifier: Modifier = Modifier,
) {
  Text(
    text = chapterLabel(header.number, header.name),
    style = MaterialTheme.typography.titleSmallEmphasized,
    color = MaterialTheme.colorScheme.primary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 6.dp)
      .semantics { heading() },
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
  row: BookmarkRowViewState,
  group: GroupPosition,
  onClick: (Bookmark.Id) -> Unit,
  onLongClick: (Bookmark.Id) -> Unit,
  onDelete: (Bookmark.Id) -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = segmentedShape(group.index, group.count)
  val percentFormat = remember { NumberFormat.getPercentInstance() }
  val percent = percentFormat.format(row.percent / 100.0)
  val chapter = chapterLabel(row.chapterNumber, row.chapterName)
  val chapterPrefix = if (row.showChapter) "$chapter · " else ""
  val headline: String
  val meta: String
  val addNoteHint = row.note == null && !row.setBySleepTimer
  when {
    row.note != null -> {
      headline = row.note
      meta = "$chapterPrefix${row.time} · $percent"
    }
    row.setBySleepTimer -> {
      headline = stringResource(R.string.bookmark_dozed_off)
      meta = "$chapterPrefix${row.time} · $percent"
    }
    else -> {
      headline = "$chapterPrefix${row.time}"
      meta = percent
    }
  }
  val savedAt = dayLabelText(row.savedAt)
  val kind = kindLabel(row.kind, row.setBySleepTimer)
  val hint = stringResource(R.string.bookmark_add_note_hint)
  val editLabel = stringResource(R.string.common_action_edit)
  val deleteLabel = stringResource(R.string.common_action_delete)
  val description = listOf(kind, headline, meta, savedAt).joinToString(", ")

  // Not saveable: the list keeps saved state per key, so a row brought back by undo would come
  // back swiped away and be deleted again.
  val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
  val dismissState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }
  // a new callback would restart the box's effect and delete the swiped away row once more
  val currentOnDelete by rememberUpdatedState(onDelete)
  val onDismiss = remember(row.id) { { _: SwipeToDismissBoxValue -> currentOnDelete(row.id) } }
  SwipeToDismissBox(
    state = dismissState,
    modifier = modifier,
    onDismiss = onDismiss,
    backgroundContent = { DeleteBackground(progress = dismissState.progress, direction = dismissState.dismissDirection) },
  ) {
    Surface(
      shape = shape,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .combinedClickable(
          onClick = { onClick(row.id) },
          onLongClick = { onLongClick(row.id) },
          onLongClickLabel = editLabel,
        )
        .clearAndSetSemantics {
          contentDescription = description
          onClick {
            onClick(row.id)
            true
          }
          customActions = listOf(
            CustomAccessibilityAction(editLabel) {
              onLongClick(row.id)
              true
            },
            CustomAccessibilityAction(deleteLabel) {
              onDelete(row.id)
              true
            },
          )
        },
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BookmarkBadge(kind = row.kind, setBySleepTimer = row.setBySleepTimer, size = 44.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1F)) {
          Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = if (row.note == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Row {
            Text(
              text = meta,
              style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1F, fill = false),
            )
            if (addNoteHint) {
              Text(
                text = " · $hint",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
              )
            }
          }
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = savedAt,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.End,
        )
      }
    }
  }
}

/** A friendly delete: a shape that grows and pops under the row instead of a red slab. */
@Composable
private fun DeleteBackground(
  progress: Float,
  direction: SwipeToDismissBoxValue,
) {
  val colors = MaterialTheme.colorScheme
  val scale by animateFloatAsState(
    targetValue = if (progress > 0.35F) 1.15F else (progress / 0.35F).coerceIn(0.3F, 1F),
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "deleteScale",
  )
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .background(colors.errorContainer, MaterialShapes.Cookie6Sided.toShape()),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = VoiceIcons.Delete,
        contentDescription = null,
        tint = colors.onErrorContainer,
      )
    }
  }
}

@Composable
private fun YouAreHereRow(
  item: BookmarkListItem.YouAreHere,
  playing: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val percentFormat = remember { NumberFormat.getPercentInstance() }
  Surface(
    shape = segmentedShape(item.group.index, item.group.count),
    color = colors.primaryContainer,
    contentColor = colors.onPrimaryContainer,
    modifier = modifier
      .fillMaxWidth()
      .semantics(mergeDescendants = true) {},
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(colors.primary, MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center,
      ) {
        NowPlayingBars(playing = playing, color = colors.onPrimary)
      }
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = stringResource(R.string.bookmark_you_are_here),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )
        Text(
          text = "${item.time} · ${percentFormat.format(item.percent / 100.0)}",
          style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
        )
      }
    }
  }
}

@Composable
private fun EmptyHero(modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  val scope = rememberCoroutineScope()
  val wiggle = remember { Animatable(0F) }
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(160.dp)
        .graphicsLayer { rotationZ = wiggle.value }
        .clip(MaterialShapes.Cookie12Sided.toShape())
        .background(colors.tertiaryContainer)
        .combinedClickable(onClick = {
          scope.launch {
            wiggle.snapTo(18F)
            wiggle.animateTo(0F, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
          }
        })
        .clearAndSetSemantics {},
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = VoiceIcons.Bookmark,
        contentDescription = null,
        tint = colors.onTertiaryContainer,
        modifier = Modifier.size(72.dp),
      )
    }
    Spacer(Modifier.height(24.dp))
    Text(
      text = stringResource(R.string.bookmark_empty_title),
      style = MaterialTheme.typography.headlineSmallEmphasized,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.bookmark_empty_message),
      style = MaterialTheme.typography.bodyLarge,
      color = colors.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
