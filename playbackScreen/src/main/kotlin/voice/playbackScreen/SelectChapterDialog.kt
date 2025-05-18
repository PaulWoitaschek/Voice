package voice.playbackScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import voice.common.formatTime
import voice.strings.R as StringsR

@Composable
internal fun SelectChapterDialog(
  dialogState: BookPlayDialogViewState.SelectChapterDialog,
  viewModel: BookPlayViewModel,
) {
  ModalBottomSheet(
    onDismissRequest = { viewModel.dismissDialog() },
    content = {
      LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = dialogState.selectedIndex?.minus(1)?.coerceAtLeast(0) ?: 0),
        content = {
          itemsIndexed(dialogState.chapters) { index, chapter ->
            val isCurrentChapter = dialogState.selectedIndex == index
            val description = stringResource(StringsR.string.migration_detail_content_position_current_chapter_title)

            ListItem(
              colors = if (isCurrentChapter) { ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) }
              else { ListItemDefaults.colors(containerColor = Color.Transparent) },
              modifier = Modifier
                .padding(3.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .semantics {
                  selected = isCurrentChapter
                  if (isCurrentChapter) contentDescription = description
                }
                .clickable {
                  viewModel.onChapterClick(index)
                },
              headlineContent = {
                Text(text = chapter.name ?: "")
              },
              trailingContent = {
                Text(text = formatTime(chapter.startMs))
              },
            )
          }
        },
      )
    },
  )
}
