package voice.playbackScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
internal fun SelectChapterDialog(
  dialogState: BookPlayDialogViewState.SelectChapterDialog,
  viewModel: BookPlayViewModel,
) {
  AlertDialog(
    onDismissRequest = { viewModel.dismissDialog() },
    confirmButton = {},
    text = {
      LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = dialogState.selectedIndex ?: 0),
        content = {
          itemsIndexed(dialogState.chapters) { index, chapter ->
            ListItem(
              modifier = Modifier.clickable {
                viewModel.onChapterClicked(index)
              },
              headlineText = {
                Text(text = chapter.name ?: "")
              },
              trailingContent = {
                if (dialogState.selectedIndex == index) {
                  Icon(
                    imageVector = Icons.Outlined.Equalizer,
                    contentDescription = stringResource(id = R.string.migration_detail_content_position_current_chapter_title),
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
