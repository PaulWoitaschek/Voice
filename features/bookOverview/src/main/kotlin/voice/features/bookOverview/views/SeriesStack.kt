@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.NumberFormat
import voice.core.data.BookCategory
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberCoverThumbnailRequest
import voice.core.ui.seriesCoverSharedElementModifier
import voice.features.bookOverview.overview.SeriesStackViewState

@Composable
internal fun ListSeriesRow(
  series: SeriesStackViewState,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(24.dp),
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(shape)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SeriesCover(
        seriesKey = series.key,
        covers = series.covers,
        gapColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.size(64.dp),
      ) {
        SeriesBadge(
          series = series,
          // in a series in progress, the bar takes the place of the counts, and the row has its own
          // hidden label
          describe = series.category == BookCategory.CURRENT,
          hidden = false,
          modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
        )
      }
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = series.name,
          style = MaterialTheme.typography.titleMediumEmphasized.bookText(),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (series.author != null) {
          Text(
            text = series.author,
            style = MaterialTheme.typography.bodyMedium.bookText(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (series.category == BookCategory.CURRENT) {
          Spacer(Modifier.height(10.dp))
          SeriesProgress(
            progress = series.progress,
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          Spacer(Modifier.height(6.dp))
          Text(
            text = seriesCounts(series.bookCount, series.finishedCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }
      if (series.hidden) {
        Spacer(Modifier.width(12.dp))
        HiddenLabel()
      }
    }
  }
}

@Composable
internal fun GridSeries(
  series: SeriesStackViewState,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(24.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(6.dp),
  ) {
    SeriesCover(
      seriesKey = series.key,
      covers = series.covers,
      modifier = Modifier.fillMaxWidth().aspectRatio(1F),
    ) {
      SeriesBadge(
        series = series,
        describe = true,
        hidden = series.hidden,
        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
      )
    }
    // right under the cover, so the bars in a row line up however long the titles are
    if (series.category == BookCategory.CURRENT) {
      Spacer(Modifier.height(8.dp))
      SeriesProgress(
        progress = series.progress,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = series.name,
      style = MaterialTheme.typography.titleSmallEmphasized.bookText(),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (series.author != null) {
      Text(
        text = series.author,
        style = MaterialTheme.typography.labelMedium.bookText(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SeriesProgress(
  progress: List<Float>,
  modifier: Modifier = Modifier,
) {
  val color = ProgressIndicatorDefaults.linearColor
  val trackColor = ProgressIndicatorDefaults.linearTrackColor
  val average = progress.average().toFloat()
  Canvas(
    modifier.height(4.dp).progressSemantics(average),
  ) {
    val gap = SEGMENT_GAP.toPx()
    val fits = (size.width - gap * (progress.size - 1)) / progress.size >= MIN_SEGMENT_WIDTH.toPx()
    // a series with many books is too long for segments
    val segments = if (fits) progress else listOf(average)
    val width = (size.width - gap * (segments.size - 1)) / segments.size
    val radius = CornerRadius(size.height / 2)
    scale(scaleX = if (layoutDirection == LayoutDirection.Rtl) -1F else 1F, scaleY = 1F) {
      segments.forEachIndexed { index, segment ->
        val left = index * (width + gap)
        drawRoundRect(trackColor, Offset(left, 0F), Size(width, size.height), radius)
        clipRect(left = left, right = left + width * segment) {
          drawRoundRect(color, Offset(left, 0F), Size(width, size.height), radius)
        }
      }
    }
  }
}

private val SEGMENT_GAP = 4.dp
private val MIN_SEGMENT_WIDTH = 8.dp

// flies as the series and not as its front book, which can be on the Continue listening card at the
// same time
@Composable
internal fun SeriesCover(
  seriesKey: String,
  covers: List<String?>,
  modifier: Modifier = Modifier,
  gapColor: Color = MaterialTheme.colorScheme.surface,
  badge: @Composable BoxScope.() -> Unit = {},
) {
  // the badge flies along, or the flying covers would hide it
  CoverStack(
    covers = covers,
    modifier = modifier.seriesCoverSharedElementModifier(seriesKey),
    gapColor = gapColor,
    badge = badge,
  )
}

@Composable
internal fun CoverStack(
  covers: List<String?>,
  modifier: Modifier = Modifier,
  gapColor: Color = MaterialTheme.colorScheme.surface,
  badge: @Composable BoxScope.() -> Unit = {},
) {
  Box(modifier = modifier) {
    val behind = covers.size - 1
    covers.asReversed().forEachIndexed { index, cover ->
      val depth = if (behind == 0) 0F else (behind - index).toFloat() / behind
      // a cover keeps its image when another book comes to the front
      key(cover) {
        AsyncImage(
          modifier =
            Modifier.fillMaxSize(if (behind == 0) 1F else STACKED_COVER_SIZE)
              .align(
                BiasAlignment(horizontalBias = -1F + 2F * depth, verticalBias = 1F - 2F * depth),
              )
              .clip(CoverShape)
              .border(2.dp, gapColor, CoverShape),
          model = rememberCoverThumbnailRequest(cover),
          placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
          error = painterResource(id = UiR.drawable.album_art),
          contentScale = ContentScale.Crop,
          contentDescription = null,
        )
      }
    }
    badge()
  }
}

private const val STACKED_COVER_SIZE = 0.8F

@Composable
private fun SeriesBadge(
  series: SeriesStackViewState,
  describe: Boolean,
  hidden: Boolean,
  modifier: Modifier = Modifier,
) {
  val finished = series.category == BookCategory.FINISHED
  val counts = seriesCounts(series.bookCount, series.finishedCount)
  val description =
    if (hidden) "$counts, ${stringResource(StringsR.string.library_book_hidden)}" else counts
  val colors = MaterialTheme.colorScheme
  Box(
    modifier =
      modifier
        .sizeIn(minWidth = 24.dp, minHeight = 24.dp)
        .background(colors.surface, CircleShape)
        .padding(2.dp)
        .background(
          when {
            hidden -> colors.surfaceContainerHighest
            finished -> colors.primary
            else -> colors.secondaryContainer
          },
          CircleShape,
        )
        .clearAndSetSemantics {
          if (describe) contentDescription = description
        },
    contentAlignment = Alignment.Center,
  ) {
    when {
      hidden ->
        Icon(
          modifier = Modifier.size(14.dp),
          imageVector = VoiceIcons.VisibilityOff,
          contentDescription = null,
          tint = colors.onSurfaceVariant,
        )
      finished ->
        Icon(
          modifier = Modifier.size(14.dp),
          imageVector = VoiceIcons.Check,
          contentDescription = null,
          tint = colors.onPrimary,
        )
      else ->
        Text(
          text = NumberFormat.getIntegerInstance().format(series.bookCount),
          style = MaterialTheme.typography.labelMedium,
          color = colors.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
  }
}

@Composable
internal fun seriesCounts(
  bookCount: Int,
  finishedCount: Int,
): String {
  val books =
    pluralStringResource(StringsR.plurals.library_search_browse_book_count, bookCount, bookCount)
  if (finishedCount == 0) return books
  val finished =
    pluralStringResource(
      StringsR.plurals.library_search_browse_finished,
      finishedCount,
      finishedCount,
    )
  return "$books · $finished"
}

private fun previewSeries(
  category: BookCategory,
  hidden: Boolean = false,
) =
  SeriesStackViewState(
    key = "signalsaga",
    name = "The Signal Saga",
    author = "Daniel Hartwell",
    covers = listOf(null, null, null),
    bookCount = 4,
    finishedCount = if (category == BookCategory.FINISHED) 4 else 1,
    progress = listOf(1F, 0.4F, 0F, 0F),
    category = category,
    hidden = hidden,
  )

@Composable
@Preview
private fun ListSeriesRowPreviewStarted() {
  VoiceTheme {
    Surface {
      ListSeriesRow(previewSeries(BookCategory.CURRENT), {}, {})
    }
  }
}

@Composable
@Preview
private fun ListSeriesRowPreviewNotStarted() {
  VoiceTheme {
    Surface {
      ListSeriesRow(previewSeries(BookCategory.NOT_STARTED), {}, {})
    }
  }
}

@Composable
@Preview(widthDp = 200)
private fun GridSeriesPreviewStarted() {
  VoiceTheme {
    Surface {
      GridSeries(previewSeries(BookCategory.CURRENT), {}, {})
    }
  }
}

@Composable
@Preview(widthDp = 200)
private fun GridSeriesPreviewFinished() {
  VoiceTheme {
    Surface {
      GridSeries(previewSeries(BookCategory.FINISHED), {}, {})
    }
  }
}

@Composable
@Preview
private fun ListSeriesRowPreviewHidden() {
  VoiceTheme {
    Surface {
      ListSeriesRow(previewSeries(BookCategory.NOT_STARTED, hidden = true), {}, {})
    }
  }
}

@Composable
@Preview(widthDp = 200)
private fun GridSeriesPreviewHidden() {
  VoiceTheme {
    Surface {
      GridSeries(previewSeries(BookCategory.NOT_STARTED, hidden = true), {}, {})
    }
  }
}
