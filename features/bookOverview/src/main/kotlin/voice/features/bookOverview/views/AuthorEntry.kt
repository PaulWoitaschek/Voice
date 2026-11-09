@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.core.data.BookCategory
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme
import voice.core.ui.authorSharedBoundsModifier
import voice.core.ui.bookText
import voice.core.ui.rememberCoverThumbnailRequest
import voice.features.bookOverview.overview.AuthorEntryViewState

@Composable
internal fun ListAuthorRow(
  author: AuthorEntryViewState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.largeIncreased,
  flies: Boolean = false,
) {
  Surface(
    modifier = modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AuthorCovers(covers = author.covers, modifier = Modifier.size(64.dp).flying(author, flies))
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = authorName(author),
          style = MaterialTheme.typography.titleMediumEmphasized.nameText(author.name),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = authorCounts(author.seriesCount, author.bookCount),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
      if (author.hidden) {
        Spacer(Modifier.width(12.dp))
        HiddenLabel()
      }
    }
  }
}

@Composable
internal fun GridAuthor(
  author: AuthorEntryViewState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  flies: Boolean = false,
) {
  Column(
    modifier =
      modifier.clip(MaterialTheme.shapes.largeIncreased).clickable(onClick = onClick).padding(6.dp),
  ) {
    // the badge flies along, or the flying covers would hide it
    Box(Modifier.fillMaxWidth().aspectRatio(1F).flying(author, flies)) {
      AuthorCovers(covers = author.covers, modifier = Modifier.fillMaxSize())
      CountBadge(
        count = author.bookCount,
        finished = author.category == BookCategory.FINISHED,
        hidden = author.hidden,
        description = authorCounts(author.seriesCount, author.bookCount),
        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = authorName(author),
      style = MaterialTheme.typography.titleSmallEmphasized.nameText(author.name),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

// "No author" has no photo to fly into
@Composable
private fun Modifier.flying(
  author: AuthorEntryViewState,
  flies: Boolean,
): Modifier = if (flies && author.key != null) authorSharedBoundsModifier(author.key) else this

// a grid and not a stack, so an author doesn't look like a series
@Composable
private fun AuthorCovers(
  covers: List<String?>,
  modifier: Modifier = Modifier,
) {
  if (covers.size == 1) {
    AuthorCover(covers.single(), modifier)
    return
  }
  // two covers in opposite corners, so the grid doesn't look half empty
  val cells =
    if (covers.size == 2) listOf(0, null, null, 1) else List(4) { it.takeIf { it < covers.size } }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
    repeat(2) { row ->
      Row(Modifier.weight(1F), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) { column ->
          val index = cells[row * 2 + column]
          val cell = Modifier.weight(1F).fillMaxHeight()
          if (index != null) {
            AuthorCover(covers[index], cell)
          } else {
            Box(cell.clip(CoverShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
          }
        }
      }
    }
  }
}

@Composable
private fun AuthorCover(
  cover: String?,
  modifier: Modifier = Modifier,
) {
  AsyncImage(
    modifier = modifier.clip(CoverShape),
    model = rememberCoverThumbnailRequest(cover),
    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
    error = painterResource(id = UiR.drawable.album_art),
    contentScale = ContentScale.Crop,
    contentDescription = null,
  )
}

@Composable
private fun authorName(author: AuthorEntryViewState): String =
  author.name ?: stringResource(StringsR.string.library_no_author)

// "No author" is Voice's own words, not the book's
internal fun TextStyle.nameText(name: String?): TextStyle = if (name != null) bookText() else this

@Composable
private fun authorCounts(
  seriesCount: Int,
  bookCount: Int,
): String {
  val books =
    pluralStringResource(StringsR.plurals.library_search_browse_book_count, bookCount, bookCount)
  if (seriesCount == 0) return books
  val series =
    pluralStringResource(StringsR.plurals.library_author_series_count, seriesCount, seriesCount)
  return "$series · $books"
}

private fun previewAuthor(
  name: String?,
  category: BookCategory = BookCategory.NOT_STARTED,
  covers: Int = 4,
) =
  AuthorEntryViewState(
    key = name?.lowercase(),
    name = name,
    covers = List(covers) { null },
    seriesCount = if (covers > 1) 1 else 0,
    bookCount = covers,
    category = category,
  )

@Composable
@Preview
private fun ListAuthorRowPreview() {
  VoiceTheme {
    Surface {
      Column {
        ListAuthorRow(previewAuthor("Lena Morgan"), {})
        ListAuthorRow(previewAuthor("Nora Whitfield", covers = 1), {})
        ListAuthorRow(previewAuthor(null, covers = 2), {})
      }
    }
  }
}

@Composable
@Preview(widthDp = 200)
private fun GridAuthorPreview() {
  VoiceTheme {
    Surface {
      GridAuthor(previewAuthor("Lena Morgan"), {})
    }
  }
}

@Composable
@Preview(widthDp = 200)
private fun GridAuthorPreviewFinished() {
  VoiceTheme {
    Surface {
      GridAuthor(previewAuthor("Daniel Hartwell", BookCategory.FINISHED, covers = 2), {})
    }
  }
}
