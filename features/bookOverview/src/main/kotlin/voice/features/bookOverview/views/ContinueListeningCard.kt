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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.CoverTheme
import voice.core.ui.PlayButton
import voice.core.ui.bookText
import voice.core.ui.playButtonSharedElementModifier
import voice.features.bookOverview.overview.BookOverviewItemViewState
import java.text.NumberFormat

/** Uses the same cover theme as the player, so cover and play button hand over seamlessly. */
@Composable
internal fun ContinueListeningCard(
  book: BookOverviewItemViewState,
  playing: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CoverTheme(cover = book.cover) {
    val colors = MaterialTheme.colorScheme
    val percentFormat = remember { NumberFormat.getPercentInstance() }
    // Dark containers can be bright for warm covers, so the tint is kept subtle there. Text and
    // accents use surface / primary roles, which stay legible on either.
    val tint = if (colors.surface.luminance() < 0.5F) 0.35F else 0.85F
    Box(
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(32.dp))
        .background(colors.surfaceContainerHigh)
        .background(
          Brush.linearGradient(
            listOf(colors.primaryContainer.copy(alpha = tint), colors.tertiaryContainer.copy(alpha = tint)),
          ),
        )
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(20.dp),
    ) {
      CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
        Column {
          Row {
            BookCover(
              bookId = book.id,
              cover = book.cover,
              unavailable = book.unavailable,
              download = book.download,
              modifier = Modifier.size(112.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1F)) {
              Text(
                text = stringResource(R.string.library_continue_listening),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = colors.primary,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = book.name,
                style = MaterialTheme.typography.headlineSmallEmphasized.bookText(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              if (book.author != null) {
                Text(
                  text = book.author,
                  style = MaterialTheme.typography.bodyLarge.bookText(),
                  color = colors.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
          Spacer(Modifier.height(16.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1F)) {
              val density = LocalDensity.current
              val stroke = remember(density) { Stroke(width = with(density) { 5.dp.toPx() }, cap = StrokeCap.Round) }
              // the player's seek bar wave: 1.75dp of the 2.5dp the stroke leaves in the 10dp container, and a fifth of a
              // wave per second
              LinearWavyProgressIndicator(
                progress = { book.progress },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.2F),
                stroke = stroke,
                trackStroke = stroke,
                amplitude = { if (playing) 0.7F else 0F },
                wavelength = 44.dp,
                waveSpeed = 8.8.dp,
              )
              Spacer(Modifier.height(8.dp))
              // the time left gets its full width first, the percentage gives way on narrow cards
              Row {
                Text(
                  modifier = Modifier.weight(1F),
                  text = stringResource(R.string.playback_book_progress_read, percentFormat.format(book.progress.toDouble())),
                  style = MaterialTheme.typography.labelLargeEmphasized,
                  color = colors.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.playback_book_progress_remaining, book.remainingTime),
                  style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            Spacer(Modifier.width(16.dp))
            PlayButton(
              playing = playing,
              onClick = onPlayClick,
              modifier = Modifier.playButtonSharedElementModifier(book.id),
              size = 72.dp,
            )
          }
        }
      }
    }
  }
}
