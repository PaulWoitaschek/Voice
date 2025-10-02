package voice.features.playbackScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import voice.core.strings.R as StringsR

@Composable
internal fun SelectChapterDialog(
  dialogState: BookPlayDialogViewState.SelectChapterDialog,
  viewModel: BookPlayViewModel,
) {
  ModalBottomSheet(
    onDismissRequest = { viewModel.dismissDialog() },
    content = {
      val selectedIndex = dialogState.items.indexOfFirst { it.active }
      // -1 because we want to show the previous chapter as well
      val initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)
      LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex),
        content = {
          items(dialogState.items) { chapter ->
            val isCurrentChapter = chapter.active
            val description = stringResource(StringsR.string.playback_current_chapter)
            val backgroundColor = if (chapter.active) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              Color.Transparent
            }
            ListItem(
              colors = ListItemDefaults.colors(containerColor = backgroundColor),
              modifier = Modifier
                .padding(3.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .semantics {
                  selected = chapter.active
                  if (isCurrentChapter) contentDescription = description
                }
                .clickable {
                  viewModel.onChapterClick(number = chapter.number)
                },
              headlineContent = {
                Text(text = chapter.name)
              },
              leadingContent = {
                Text(text = chapter.number.toString())
              },
              trailingContent = {
                Text(text = chapter.time)
              },
            )
          }
        },
      )
    },
  )
}
