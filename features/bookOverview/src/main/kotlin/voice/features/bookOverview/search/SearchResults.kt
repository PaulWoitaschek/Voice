@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.search

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.search.BookSearchField
import voice.core.ui.PlayButton
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.playButtonSharedElementModifier
import voice.core.ui.segmentedShape
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.views.BookCover
import java.text.NumberFormat
import voice.core.strings.R as StringsR

@Composable
internal fun SearchResults(
  viewState: BookSearchViewState.Results,
  contentPadding: PaddingValues,
  listener: BookSearchListener,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    if (viewState.filters.isNotEmpty()) {
      item(key = "filters", contentType = "filters") {
        Filters(
          filters = viewState.filters,
          onClick = listener::onFilterClick,
          modifier = Modifier.padding(bottom = 10.dp),
        )
      }
    }
    item(key = viewState.topResult.id.value, contentType = "topResult") {
      TopResult(
        result = viewState.topResult,
        playing = viewState.topResultPlaying,
        onClick = { listener.onBookClick(viewState.topResult.id) },
        onLongClick = { listener.onBookLongClick(viewState.topResult.id) },
        onPlayClick = { listener.onPlayClick(viewState.topResult.id) },
        modifier = Modifier
          .animateItem()
          .padding(bottom = 10.dp),
      )
    }
    itemsIndexed(
      items = viewState.otherResults,
      key = { _, result -> result.id.value },
      contentType = { _, _ -> "result" },
    ) { index, result ->
      SearchResultRow(
        result = result,
        shape = segmentedShape(index, viewState.otherResults.size),
        onClick = { listener.onBookClick(result.id) },
        onLongClick = { listener.onBookLongClick(result.id) },
        modifier = Modifier.animateItem(),
      )
    }
  }
}

@Composable
private fun Filters(
  filters: List<BookSearchViewState.Filter>,
  onClick: (BookSearchField?) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    filters.forEach { filter ->
      FilterChip(
        selected = filter.selected,
        onClick = { onClick(filter.field) },
        label = {
          Text(filter.field?.label() ?: stringResource(StringsR.string.library_search_field_all))
          Spacer(Modifier.width(6.dp))
          Text(
            text = filter.count.toString(),
            fontWeight = FontWeight.SemiBold,
          )
        },
        leadingIcon = if (filter.selected) {
          {
            Icon(
              imageVector = VoiceIcons.Check,
              contentDescription = null,
              modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
          }
        } else {
          null
        },
      )
    }
  }
}

@Composable
private fun TopResult(
  result: SearchResultViewState,
  playing: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(28.dp)
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = shape,
    color = colors.primaryContainer,
    contentColor = colors.onPrimaryContainer,
  ) {
    Row(Modifier.padding(12.dp)) {
      BookCover(
        bookId = result.id,
        cover = result.cover,
        finished = result.category == BookOverviewCategory.FINISHED,
        modifier = Modifier.size(96.dp),
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = result.title.annotated(SpanStyle(background = colors.primary, color = colors.onPrimary)),
          style = MaterialTheme.typography.titleLargeEmphasized,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (result.author != null) {
          Text(
            text = result.author.annotated(SpanStyle(background = colors.primary, color = colors.onPrimary)),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (result.matchTag != null) {
          MatchTagText(
            tag = result.matchTag,
            modifier = Modifier.padding(top = 2.dp),
          )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1F)) {
            if (result.category == BookOverviewCategory.CURRENT) {
              LinearProgressIndicator(
                progress = { result.progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 6.dp),
              )
            }
            Text(
              text = stringResource(StringsR.string.playback_book_progress_remaining, result.remainingTime),
              style = MaterialTheme.typography.labelMedium,
              maxLines = 1,
            )
          }
          Spacer(Modifier.width(12.dp))
          PlayButton(
            playing = playing,
            onClick = onPlayClick,
            size = 56.dp,
            modifier = Modifier.playButtonSharedElementModifier(result.id),
          )
        }
      }
    }
  }
}

@Composable
private fun SearchResultRow(
  result: SearchResultViewState,
  shape: Shape,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = shape,
    color = colors.surfaceBright,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BookCover(
        bookId = result.id,
        cover = result.cover,
        finished = result.category == BookOverviewCategory.FINISHED,
        modifier = Modifier.size(56.dp),
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        val highlight = SpanStyle(background = colors.primaryContainer, color = colors.onPrimaryContainer)
        Text(
          text = result.title.annotated(highlight),
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (result.author != null) {
          Text(
            text = result.author.annotated(highlight),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (result.matchTag != null) {
          Surface(
            modifier = Modifier.padding(top = 4.dp),
            shape = RoundedCornerShape(6.dp),
            color = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
          ) {
            MatchTagText(
              tag = result.matchTag,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
        }
      }
      Spacer(Modifier.width(12.dp))
      ResultTrailing(result)
    }
  }
}

@Composable
private fun ResultTrailing(result: SearchResultViewState) {
  when (result.category) {
    BookOverviewCategory.CURRENT -> {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
          progress = { result.progress },
          modifier = Modifier.size(24.dp),
          strokeWidth = 3.dp,
          trackColor = ProgressIndicatorDefaults.circularDeterminateTrackColor,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = remember(result.progress) { NumberFormat.getPercentInstance().format(result.progress) },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    BookOverviewCategory.NOT_STARTED -> {
      Text(
        text = result.remainingTime,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    // the cover shows a check
    BookOverviewCategory.FINISHED -> Unit
  }
}

@Composable
private fun MatchTagText(
  tag: MatchTag,
  modifier: Modifier = Modifier,
) {
  val label = stringResource(
    when (tag.field) {
      BookSearchField.Series -> StringsR.string.library_search_match_series
      BookSearchField.Narrator -> StringsR.string.library_search_match_narrator
      else -> StringsR.string.library_search_match_genre
    },
    tag.text.text,
  )
  val text = remember(label, tag) {
    val offset = label.indexOf(tag.text.text)
    HighlightedText(
      text = label,
      highlights = if (offset < 0) emptyList() else tag.text.highlights.map { it.first + offset..it.last + offset },
    )
  }
  Text(
    text = text.annotated(SpanStyle(fontWeight = FontWeight.ExtraBold, textDecoration = TextDecoration.Underline)),
    modifier = modifier,
    style = MaterialTheme.typography.labelMedium,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

@Composable
private fun HighlightedText.annotated(style: SpanStyle): AnnotatedString {
  return remember(this, style) {
    buildAnnotatedString {
      append(text)
      highlights.forEach { range ->
        if (range.first >= 0 && range.last < text.length) {
          addStyle(style, range.first, range.last + 1)
        }
      }
    }
  }
}

@Preview
@Composable
private fun SearchResultsPreview() {
  fun result(
    title: String,
    author: String,
    category: BookOverviewCategory,
    titleHighlights: List<IntRange> = emptyList(),
    authorHighlights: List<IntRange> = emptyList(),
    matchTag: MatchTag? = null,
  ) = SearchResultViewState(
    id = BookId(title),
    title = HighlightedText(title, titleHighlights),
    author = HighlightedText(author, authorHighlights),
    cover = null,
    category = category,
    progress = 0.34F,
    remainingTime = "28:12:00",
    matchTag = matchTag,
  )
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
      SearchResults(
        viewState = BookSearchViewState.Results(
          filters = listOf(
            BookSearchViewState.Filter(field = null, count = 5, selected = true),
            BookSearchViewState.Filter(field = BookSearchField.Title, count = 2, selected = false),
            BookSearchViewState.Filter(field = BookSearchField.Author, count = 2, selected = false),
            BookSearchViewState.Filter(field = BookSearchField.Series, count = 1, selected = false),
          ),
          topResult = result(
            title = "The Way of Kings",
            author = "Brandon Sanderson",
            category = BookOverviewCategory.CURRENT,
            titleHighlights = listOf(11..14),
          ),
          topResultPlaying = false,
          otherResults = listOf(
            result(
              title = "The King of Elfland's Daughter",
              author = "Lord Dunsany",
              category = BookOverviewCategory.NOT_STARTED,
              titleHighlights = listOf(4..7),
            ),
            result(
              title = "Fairy Tale",
              author = "Stephen King",
              category = BookOverviewCategory.CURRENT,
              authorHighlights = listOf(8..11),
            ),
            result(
              title = "The Stand",
              author = "Stephen King",
              category = BookOverviewCategory.FINISHED,
              authorHighlights = listOf(8..11),
            ),
            result(
              title = "The Name of the Wind",
              author = "Patrick Rothfuss",
              category = BookOverviewCategory.NOT_STARTED,
              matchTag = MatchTag(BookSearchField.Series, HighlightedText("Kingkiller Chronicle 1", listOf(0..3))),
            ),
          ),
        ),
        contentPadding = PaddingValues(12.dp),
        listener = BookSearchListener.Noop,
      )
    }
  }
}
