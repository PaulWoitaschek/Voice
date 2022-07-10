package voice.bookOverview.views

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.bookOverview.R

@Composable
internal fun BookFolderIcon(modifier: Modifier = Modifier, withHint: Boolean, onClick: () -> Unit) {
  Box {
    IconButton(modifier = modifier, onClick = onClick) {
      Icon(
        imageVector = Icons.Outlined.Book,
        contentDescription = stringResource(R.string.audiobook_folders_title)
      )
    }
    if (withHint) {
      AddBookHint()
    }
  }
}
