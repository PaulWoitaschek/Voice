@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.playbackScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import voice.core.ui.NowPlayingBars
import voice.core.ui.segmentedShape
import voice.core.strings.R as StringsR

@Composable
internal fun ChapterSheet(
  dialogState: BookPlayDialogViewState.SelectChapterDialog,
  playing: Boolean,
  onChapterClick: (number: Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val items = dialogState.items
  val selectedIndex = items.indexOfFirst { it.active }
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        modifier = Modifier.weight(1F),
        text = stringResource(StringsR.string.playback_chapters_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
      )
      if (selectedIndex >= 0) {
        Text(
          text = "${selectedIndex + 1} / ${items.size}",
          style = MaterialTheme.typography.labelLargeEmphasized,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
    // -1 because we want to show the previous chapter on the screen
    val initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)
    LazyColumn(
      state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      itemsIndexed(items, key = { _, item -> item.number }) { index, chapter ->
        ChapterItem(
          chapter = chapter,
          played = selectedIndex >= 0 && index < selectedIndex,
          playing = playing,
          shape = segmentedShape(index = index, count = items.size),
          onClick = { onChapterClick(chapter.number) },
        )
      }
    }
  }
}

@Composable
private fun ChapterItem(
  chapter: BookPlayDialogViewState.SelectChapterDialog.ItemViewState,
  played: Boolean,
  playing: Boolean,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val currentDescription = stringResource(StringsR.string.playback_chapter_current_content_description)
  Surface(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .semantics {
        selected = chapter.active
        if (chapter.active) contentDescription = currentDescription
      },
    shape = shape,
    color = if (chapter.active) colors.primaryContainer else colors.surfaceContainerHigh,
    contentColor = if (chapter.active) colors.onPrimaryContainer else colors.onSurface,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(
            color = if (chapter.active) colors.primary else colors.secondaryContainer,
            shape = if (chapter.active) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
          ),
        contentAlignment = Alignment.Center,
      ) {
        if (chapter.active) {
          NowPlayingBars(playing = playing, color = colors.onPrimary)
        } else {
          Text(
            text = chapter.number.toString(),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = colors.onSecondaryContainer,
          )
        }
      }
      Spacer(Modifier.width(16.dp))
      Text(
        modifier = Modifier.weight(1F),
        text = chapter.name,
        style = if (chapter.active) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.bodyLarge,
        color = if (played) colors.onSurfaceVariant else Color.Unspecified,
        maxLines = 2,
      )
      Spacer(Modifier.width(12.dp))
      Text(
        text = chapter.time,
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
      )
    }
  }
}
