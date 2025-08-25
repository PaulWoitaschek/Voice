package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.strings.R as StringsR

@Composable
internal fun BookFolderIcon(
  withHint: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    IconButton(onClick = onClick) {
      Icon(
        imageVector = Icons.Outlined.Book,
        contentDescription = stringResource(StringsR.string.audiobook_folders_title),
      )
    }
    if (withHint) {
      AddBookHint()
    }
  }
}
