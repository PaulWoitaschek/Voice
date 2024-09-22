package voice.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableMap
import voice.bookOverview.overview.BookOverviewCategory
import voice.bookOverview.overview.BookOverviewItemViewState
import voice.common.BookId
import voice.common.compose.ImmutableFile
import voice.common.R as CommonR

@Composable
internal fun ListBooks(
  books: ImmutableMap<BookOverviewCategory, List<BookOverviewItemViewState>>,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  showPermissionBugCard: Boolean,
  onPermissionBugCardClick: () -> Unit,
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
  ) {
    if (showPermissionBugCard) {
      item {
        PermissionBugCard(onPermissionBugCardClick)
      }
    }
    books.forEach { (category, books) ->
      if (books.isEmpty()) return@forEach
      stickyHeader(
        key = category,
        contentType = "header",
      ) {
        Header(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 8.dp),
          category = category,
        )
      }
      items(
        items = books,
        key = { it.id.value },
        contentType = { "item" },
      ) { book ->
        ListBookRow(
          book = book,
          onBookClick = onBookClick,
          onBookLongClick = onBookLongClick,
        )
      }
      item {
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
      }
    }
  }
}

@Composable
internal fun ListBookRow(
  book: BookOverviewItemViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = {
          onBookClick(book.id)
        },
        onLongClick = {
          onBookLongClick(book.id)
        },
      ),
    content = {
      Column {
        Row {
          CoverImage(book.cover)
          Column(
            Modifier
              .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
              .align(Alignment.CenterVertically),
          ) {
            if (book.author != null) {
              Text(
                text = book.author.toUpperCase(LocaleList.current),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
              )
            }
            Text(
              text = book.name,
              style = MaterialTheme.typography.bodyMedium,
            )
            Text(
              text = book.remainingTime,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        if (book.progress > 0.05) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { book.progress },
          )
        }
      }
    },
  )
}

@Composable
private fun CoverImage(cover: ImmutableFile?) {
  AsyncImage(
    modifier = Modifier
      .padding(top = 8.dp, start = 8.dp, bottom = 8.dp)
      .size(76.dp)
      .clip(RoundedCornerShape(8.dp)),
    model = cover?.file,
    placeholder = painterResource(id = CommonR.drawable.album_art),
    error = painterResource(id = CommonR.drawable.album_art),
    contentDescription = null,
  )
}
