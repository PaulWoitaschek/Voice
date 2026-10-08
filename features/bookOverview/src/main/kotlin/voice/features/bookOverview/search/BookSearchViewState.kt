package voice.features.bookOverview.search

import androidx.compose.runtime.Immutable
import voice.core.data.BookId
import voice.core.search.BookSearchField
import voice.features.bookOverview.overview.BookOverviewCategory

@Immutable
sealed interface BookSearchViewState {

  /** Nothing typed yet. */
  data class Idle(
    val recentSearches: List<String>,
    val browse: Browse?,
  ) : BookSearchViewState

  data class Results(
    val filters: List<Filter>,
    val topResult: SearchResultViewState,
    val topResultPlaying: Boolean,
    val otherResults: List<SearchResultViewState>,
  ) : BookSearchViewState

  data class NoResults(
    val query: String,
    val filtered: Boolean,
  ) : BookSearchViewState

  data class Browse(
    val categories: List<BrowseCategory>,
    val selectedCategory: BrowseCategory,
    val entries: List<BrowseEntry>,
  )

  data class BrowseEntry(
    val name: String,
    val bookCount: Int,
    val inProgressCount: Int,
    val finishedCount: Int,
  )

  /**
   * A filter chip. [field] is null for the one that shows all results.
   */
  data class Filter(
    val field: BookSearchField?,
    val count: Int,
    val selected: Boolean,
  )
}

enum class BrowseCategory(val field: BookSearchField) {
  Authors(BookSearchField.Author),
  Series(BookSearchField.Series),
  Narrators(BookSearchField.Narrator),
  Genres(BookSearchField.Genre),
}

@Immutable
data class SearchResultViewState(
  val id: BookId,
  val title: HighlightedText,
  val author: HighlightedText?,
  val cover: String?,
  val category: BookOverviewCategory,
  val progress: Float,
  val remainingTime: String,
  /** Why a book matched when it wasn't its title or author, e.g. its series. */
  val matchTag: MatchTag?,
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
