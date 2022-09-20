package voice.folderPicker.selectType

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import voice.folderPicker.R

@Composable
internal fun FolderModeBook(
  book: SelectFolderTypeViewState.Book,
  modifier: Modifier = Modifier,
) {
  Card(modifier) {
    Column {
      Image(
        modifier = Modifier
          .aspectRatio(16F / 9F)
          .padding(start = 8.dp, end = 8.dp, top = 8.dp)
          .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop,
        painter = painterResource(id = R.drawable.album_art),
        contentDescription = null,
      )
      Text(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
        text = book.name,
        maxLines = 3,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        text = pluralStringResource(
          id = R.plurals.folder_type_file_count,
          count = book.fileCount,
          book.fileCount,
        ),
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}
