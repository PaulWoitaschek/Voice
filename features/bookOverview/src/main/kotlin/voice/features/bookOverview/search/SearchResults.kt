@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.search

import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.search.BookSearchField
import voice.core.strings.R as StringsR
import voice.core.ui.PlayButton
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme
import voice.core.ui.authorSharedBoundsModifier
import voice.core.ui.bookText
import voice.core.ui.playButtonSharedElementModifier
import voice.core.ui.segmentedShape
import voice.features.bookOverview.overview.CoverViewState
import voice.features.bookOverview.views.AuthorCovers
import voice.features.bookOverview.views.BookCover
import voice.features.bookOverview.views.HiddenLabel
import voice.features.bookOverview.views.SeriesCover

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
    item(key = viewState.topResult.id.value, contentType = "topResult") {
      TopResult(
        result = viewState.topResult,
        playing = viewState.topResultPlaying,
        onClick = { listener.onBookClick(viewState.topResult.id) },
        onLongClick = { listener.onBookLongClick(viewState.topResult.id) },
        onPlayClick = { listener.onPlayClick(viewState.topResult.id) },
        modifier = Modifier.animateItem().padding(bottom = 10.dp),
      )
    }
    itemsIndexed(
      items = viewState.groups,
      key = { _, group -> "${group.kind}-${group.key}" },
      contentType = { _, _ -> "group" },
    ) { index, group ->
      val last = index == viewState.groups.lastIndex
      GroupRow(
        group = group,
        shape = segmentedShape(index, viewState.groups.size),
        onClick = { listener.onGroupClick(group) },
        modifier = Modifier.animateItem().padding(bottom = if (last) 10.dp else 0.dp),
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
private fun GroupRow(
  group: SearchGroupViewState,
  shape: Shape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = shape,
    color = colors.surfaceBright,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      when (group.kind) {
        SearchGroupViewState.Kind.Author ->
          AuthorCovers(
            covers = group.covers,
            modifier = Modifier.size(56.dp).authorSharedBoundsModifier(group.key),
          )
        SearchGroupViewState.Kind.Series ->
          SeriesCover(
            seriesKey = group.key,
            covers = group.covers,
            modifier = Modifier.size(56.dp),
            gapColor = colors.surfaceBright,
          )
      }
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = group.name,
          style = MaterialTheme.typography.titleMediumEmphasized.bookText(),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = groupSummary(group),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.width(12.dp))
      Icon(
        painter = painterResource(UiR.drawable.ic_chevron_right),
        contentDescription = null,
        tint = colors.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun groupSummary(group: SearchGroupViewState): String {
  val kind =
    stringResource(
      when (group.kind) {
        SearchGroupViewState.Kind.Author -> StringsR.string.book_edit_author_label
        SearchGroupViewState.Kind.Series -> StringsR.string.book_edit_series_label
      },
    )
  val books =
    pluralStringResource(
      StringsR.plurals.library_search_browse_book_count,
      group.bookCount,
      group.bookCount,
    )
  return "$kind · $books"
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
  val shape = MaterialTheme.shapes.extraLarge
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier =
      modifier
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
        title = result.title.text,
        finished = result.category == BookCategory.FINISHED,
        unavailable = result.unavailable,
        download = result.download,
        modifier = Modifier.size(96.dp),
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text =
            result.title.annotated(
              SpanStyle(background = colors.primary, color = colors.onPrimary),
            ),
          style = MaterialTheme.typography.titleLargeEmphasized.bookText(),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (result.author != null) {
          Text(
            text =
              result.author.annotated(
                SpanStyle(background = colors.primary, color = colors.onPrimary),
              ),
            style = MaterialTheme.typography.bodyMedium.bookText(),
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
            if (result.category == BookCategory.CURRENT) {
              LinearProgressIndicator(
                progress = { result.progress },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
              )
            }
            Text(
              text =
                stringResource(
                  StringsR.string.playback_book_progress_remaining,
                  result.remainingTime,
                ),
              style = MaterialTheme.typography.labelMedium,
              maxLines = 1,
            )
          }
          if (result.hidden) {
            Spacer(Modifier.width(12.dp))
            HiddenLabel()
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
    modifier =
      modifier
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
        title = result.title.text,
        finished = result.category == BookCategory.FINISHED,
        unavailable = result.unavailable,
        download = result.download,
        modifier = Modifier.size(56.dp),
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        val highlight =
          SpanStyle(background = colors.primaryContainer, color = colors.onPrimaryContainer)
        Text(
          text = result.title.annotated(highlight),
          style = MaterialTheme.typography.titleMediumEmphasized.bookText(),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (result.author != null) {
          Text(
            text = result.author.annotated(highlight),
            style = MaterialTheme.typography.bodyMedium.bookText(),
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (result.matchTag != null) {
          Surface(
            modifier = Modifier.padding(top = 4.dp),
            shape = MaterialTheme.shapes.small,
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
      if (result.hidden) {
        Spacer(Modifier.width(12.dp))
        HiddenLabel()
      }
      Spacer(Modifier.width(12.dp))
      ResultTrailing(result)
    }
  }
}

@Composable
private fun ResultTrailing(result: SearchResultViewState) {
  when (result.category) {
    BookCategory.CURRENT -> {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
          progress = { result.progress },
          modifier = Modifier.size(24.dp),
          strokeWidth = 3.dp,
          trackColor = ProgressIndicatorDefaults.circularDeterminateTrackColor,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text =
            remember(result.progress) { NumberFormat.getPercentInstance().format(result.progress) },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    BookCategory.NOT_STARTED -> {
      Text(
        text = result.remainingTime,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    // the cover shows a check
    BookCategory.FINISHED -> Unit
  }
}

@Composable
private fun MatchTagText(
  tag: MatchTag,
  modifier: Modifier = Modifier,
) {
  val template =
    stringResource(
      when (tag.field) {
        BookSearchField.Series -> StringsR.string.library_search_match_series
        BookSearchField.Narrator -> StringsR.string.library_search_match_narrator
        BookSearchField.Chapters -> StringsR.string.library_search_match_chapter
        else -> StringsR.string.library_search_match_genre
      },
      PLACEHOLDER,
    )
  val text = remember(template, tag) { tag.text.inLabel(template) }
  Text(
    text =
      text.annotated(
        SpanStyle(fontWeight = FontWeight.ExtraBold, textDecoration = TextDecoration.Underline),
      ),
    modifier = modifier,
    style = MaterialTheme.typography.labelMedium,
    maxLines = if (tag.field == BookSearchField.Chapters) 2 else 1,
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
    category: BookCategory,
    titleHighlights: List<IntRange> = emptyList(),
    authorHighlights: List<IntRange> = emptyList(),
    matchTag: MatchTag? = null,
  ) =
    SearchResultViewState(
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
        viewState =
          BookSearchViewState.Results(
            topResult =
              result(
                title = "The Way of Kings",
                author = "Brandon Sanderson",
                category = BookCategory.CURRENT,
                titleHighlights = listOf(11..14),
              ),
            topResultPlaying = false,
            groups =
              listOf(
                SearchGroupViewState(
                  kind = SearchGroupViewState.Kind.Author,
                  key = "stephenking",
                  name = "Stephen King",
                  bookCount = 2,
                  covers =
                    listOf(CoverViewState(null, "Dune"), CoverViewState(null, "Dune Messiah")),
                ),
                SearchGroupViewState(
                  kind = SearchGroupViewState.Kind.Series,
                  key = "kingkillerchronicle",
                  name = "Kingkiller Chronicle",
                  bookCount = 2,
                  covers =
                    listOf(CoverViewState(null, "Dune"), CoverViewState(null, "Dune Messiah")),
                ),
              ),
            otherResults =
              listOf(
                result(
                  title = "The King of Elfland's Daughter",
                  author = "Lord Dunsany",
                  category = BookCategory.NOT_STARTED,
                  titleHighlights = listOf(4..7),
                ),
                result(
                  title = "Fairy Tale",
                  author = "Stephen King",
                  category = BookCategory.CURRENT,
                  authorHighlights = listOf(8..11),
                ),
                result(
                  title = "The Stand",
                  author = "Stephen King",
                  category = BookCategory.FINISHED,
                  authorHighlights = listOf(8..11),
                ),
                result(
                  title = "The Name of the Wind",
                  author = "Patrick Rothfuss",
                  category = BookCategory.NOT_STARTED,
                  matchTag =
                    MatchTag(
                      BookSearchField.Series,
                      HighlightedText("Kingkiller Chronicle 1", listOf(0..3)),
                    ),
                ),
                result(
                  title = "The Adventures of Sherlock Holmes",
                  author = "Arthur Conan Doyle",
                  category = BookCategory.NOT_STARTED,
                  matchTag =
                    MatchTag(
                      BookSearchField.Chapters,
                      HighlightedText("The King of Bohemia", listOf(4..7)),
                    ),
                ),
              ),
          ),
        contentPadding = PaddingValues(12.dp),
        listener = BookSearchListener.Noop,
      )
    }
  }
}
