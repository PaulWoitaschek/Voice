package voice.playbackScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import voice.strings.R as StringsR

@Composable
internal fun SelectChapterDialog(
  dialogState: BookPlayDialogViewState.SelectChapterDialog,
  viewModel: BookPlayViewModel,
) {
  AlertDialog(
    onDismissRequest = { viewModel.dismissDialog() },
    confirmButton = {},
    text = {
      val selectedIndex = dialogState.items.indexOfFirst { it.active }
      // -1 because we want to show the previous chapter as well
      val initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)
      LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex),
        content = {
          items(dialogState.items) { chapter ->
            ListItem(
              colors = ListItemDefaults.colors(containerColor = Color.Transparent),
              modifier = Modifier.clickable {
                viewModel.onChapterClick(chapter.number)
              },
              headlineContent = {
                Text(text = chapter.name)
              },
              leadingContent = {
                Text(text = chapter.number.toString())
              },
              trailingContent = {
                if (chapter.active) {
                  Icon(
                    imageVector = Icons.Outlined.Equalizer,
                    contentDescription = stringResource(id = StringsR.string.playback_current_chapter),
                  )
                }
              },
            )
          }
        },
      )
    },
  )
}
