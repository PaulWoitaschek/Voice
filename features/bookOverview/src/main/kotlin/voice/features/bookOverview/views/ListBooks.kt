@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.strings.R
import voice.core.ui.NowPlayingBars
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.features.bookOverview.overview.BookOverviewItemViewState

@Composable
internal fun ListBookRow(
  book: BookOverviewItemViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(24.dp),
  finished: Boolean = false,
  part: String? = null,
  current: Boolean = false,
  playing: Boolean = false,
) {
  val colors = MaterialTheme.colorScheme
  val secondaryColor = if (current) colors.onPrimaryContainer else colors.onSurfaceVariant
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .combinedClickable(
        onClick = { onBookClick(book.id) },
        onLongClick = { onBookLongClick(book.id) },
      )
      .semantics {
        if (current) selected = true
      },
    shape = shape,
    color = if (current) colors.primaryContainer else colors.surfaceContainer,
    contentColor = if (current) colors.onPrimaryContainer else colors.onSurface,
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // like the current chapter, the current book shows the now playing bars instead of its number
      if (current) {
        Box(
          modifier = Modifier
            .padding(end = 8.dp)
            .size(PartWidth)
            .background(colors.primary, MaterialShapes.Cookie9Sided.toShape()),
          contentAlignment = Alignment.Center,
        ) {
          NowPlayingBars(playing = playing, color = colors.onPrimary)
        }
      } else if (part != null) {
        Text(
          text = part,
          style = MaterialTheme.typography.titleLargeEmphasized,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .padding(end = 8.dp)
            .widthIn(min = PartWidth, max = 56.dp),
        )
      }
      BookCover(
        bookId = book.id,
        cover = book.cover,
        finished = finished,
        unavailable = book.unavailable,
        download = book.download,
        modifier = Modifier.size(64.dp),
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = book.name,
          style = MaterialTheme.typography.titleMediumEmphasized.bookText(),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (book.author != null) {
          Text(
            text = book.author,
            style = MaterialTheme.typography.bodyMedium.bookText(),
            color = secondaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (!finished) {
          Spacer(Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (book.progress > 0F) {
              LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                  .weight(1F)
                  .padding(end = 12.dp),
                gapSize = 0.dp,
                drawStopIndicator = {},
              )
            }
            Text(
              text = stringResource(R.string.playback_book_progress_remaining, book.remainingTime),
              style = MaterialTheme.typography.labelMedium,
              color = secondaryColor,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

private val PartWidth = 40.dp

@Composable
@Preview
private fun ListBookRowPreviewWithProgress() {
  VoiceTheme {
    Surface {
      ListBookRow(BookOverviewPreviewParameterProvider().book().copy(progress = 0.6f), {}, {})
    }
  }
}

@Composable
@Preview
private fun ListBookRowPreviewFinished() {
  VoiceTheme {
    Surface {
      ListBookRow(BookOverviewPreviewParameterProvider().book().copy(progress = 1f), {}, {}, finished = true)
    }
  }
}
