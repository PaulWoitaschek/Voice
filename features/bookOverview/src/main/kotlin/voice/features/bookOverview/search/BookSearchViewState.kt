package voice.features.bookOverview.search

import androidx.compose.runtime.Immutable
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.search.BookSearchField
import voice.features.bookOverview.overview.DownloadBadge

@Immutable
sealed interface BookSearchViewState {

  /** Nothing typed yet. [recentSearches] is null until they are read. */
  data class Idle(val recentSearches: List<String>?) : BookSearchViewState

  data class Results(
    val topResult: SearchResultViewState,
    val topResultPlaying: Boolean,
    val groups: List<SearchGroupViewState>,
    val otherResults: List<SearchResultViewState>,
  ) : BookSearchViewState

  data class NoResults(val query: String) : BookSearchViewState
}

@Immutable
data class SearchGroupViewState(
  val kind: Kind,
  val key: String,
  val name: String,
  val bookCount: Int,
  val covers: List<String?>,
) {
  enum class Kind {
    Author,
    Series,
  }
}

@Immutable
data class SearchResultViewState(
  val id: BookId,
  val title: HighlightedText,
  val author: HighlightedText?,
  val cover: String?,
  val category: BookCategory,
  val progress: Float,
  val remainingTime: String,
  /** Why a book matched when it wasn't its title or author, e.g. its series. */
  val matchTag: MatchTag?,
  val hidden: Boolean = false,
  val unavailable: Boolean = false,
  val download: DownloadBadge? = null,
)

@Immutable
data class MatchTag(
  val field: BookSearchField,
  val text: HighlightedText,
)

@Immutable
data class HighlightedText(
  val text: String,
  val highlights: List<IntRange> = emptyList(),
)

// the label can hold the name itself, e.g. "Chapter · Chapter", so the name replaces this mark
internal const val PLACEHOLDER = "\uE000"

internal fun HighlightedText.inLabel(template: String): HighlightedText {
  val offset = template.indexOf(PLACEHOLDER)
  return HighlightedText(
    text = template.replace(PLACEHOLDER, text),
    highlights =
      if (offset < 0) emptyList() else highlights.map { it.first + offset..it.last + offset },
  )
}

// the view model doesn't know the screen width, so the cut counts characters
internal fun HighlightedText.nearFirstHighlight(): HighlightedText {
  val first = highlights.minOfOrNull { it.first } ?: return this
  if (first <= CHAPTER_CUT_AFTER) return this
  var start = text.indexOf(' ', first - CHAPTER_LEAD).let { if (it in 0..<first) it + 1 else first }
  // a line that starts "…- The Five" seems to cut a word
  while (start < first && !text[start].isLetterOrDigit()) start++
  return HighlightedText(
    text = "…" + text.substring(start),
    highlights = highlights.map { it.first - start + 1..it.last - start + 1 },
  )
}

private const val CHAPTER_CUT_AFTER = 30
private const val CHAPTER_LEAD = 12
