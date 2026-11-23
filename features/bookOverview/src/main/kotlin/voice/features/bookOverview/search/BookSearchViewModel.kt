package voice.features.bookOverview.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshotFlow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.core.audiobookshelf.ServerDownloads
import voice.core.audiobookshelf.ServerLibrary
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.category
import voice.core.data.nameKey
import voice.core.data.repo.BookRepository
import voice.core.data.repo.RecentBookSearchRepo
import voice.core.data.seriesName
import voice.core.data.store.CurrentBookStore
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.search.BookSearch
import voice.core.search.BookSearchField
import voice.core.search.BookSearchResult
import voice.features.bookOverview.di.BookOverviewScope
import voice.features.bookOverview.overview.AuthorGroup
import voice.features.bookOverview.overview.ServerBookStates
import voice.features.bookOverview.overview.authorKey
import voice.features.bookOverview.overview.authorName
import voice.features.bookOverview.overview.byAuthorKey
import voice.features.bookOverview.overview.libraryEntries
import voice.features.bookOverview.overview.rememberServerBookStates
import voice.features.bookOverview.overview.series
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator

@SingleIn(BookOverviewScope::class)
@Inject
class BookSearchViewModel(
  private val repo: BookRepository,
  private val search: BookSearch,
  private val recentBookSearchRepo: RecentBookSearchRepo,
  private val navigator: Navigator,
  private val playerController: PlayerController,
  private val playStateManager: PlayStateManager,
  private val currentBookStore: CurrentBookStore,
  private val experimentalPlaybackPersistenceFeatureFlag:
    ExperimentalPlaybackPersistenceFeatureFlag,
  private val lastResults: LastBookSearchResults,
  private val serverLibrary: ServerLibrary,
  private val serverDownloads: ServerDownloads,
  private val dispatcherProvider: DispatcherProvider,
) {

  private val scope = MainScope(dispatcherProvider)

  val query = TextFieldState()

  /**
   * Navigation disposes the search while another screen is open, and this view model with it. The
   * saved state brings back what was searched.
   */
  internal val saver: Saver<BookSearchViewModel, String> =
    Saver(
      save = { query.text.toString() },
      restore = { text ->
        query.setTextAndPlaceCursorAtEnd(text)
        this
      },
    )

  @Composable
  internal fun state(): BookSearchViewState {
    val recentSearches = remember {
      recentBookSearchRepo.flow()
    }
      .collectAsState(initial = null)
      .value
    val currentBookId = remember { currentBookStore.data }.collectAsState(initial = null).value
    val playState = remember {
      playStateManager.playStateFlow
    }
      .collectAsState(initial = PlayStateManager.PlayState.Paused)
      .value
    val serverStates = rememberServerBookStates(serverLibrary, serverDownloads)

    val queryText = query.text.toString().trim()
    val results = remember {
      queryResults()
    }
    // coming back shows the results right away, so the cover can fly back into its row
    .collectAsState(initial = lastResults.value?.takeIf { it.query == queryText })
      .value
      ?.takeIf { queryText.isNotEmpty() }

    if (results == null) return BookSearchViewState.Idle(recentSearches)
    if (results.results.isEmpty()) return BookSearchViewState.NoResults(results.query)

    val livePlaybackState = livePlaybackState(currentBookId)
    val topResult = results.results.first().toViewState(livePlaybackState, serverStates)
    return BookSearchViewState.Results(
      topResult = topResult,
      topResultPlaying = playState == PlayStateManager.PlayState.Playing(topResult.id),
      groups = results.groups,
      otherResults =
        results.results.drop(1).map { it.toViewState(livePlaybackState, serverStates) },
    )
  }

  /** With the experimental persistence, the stored position of the playing book lags behind. */
  @Composable
  private fun livePlaybackState(currentBookId: BookId?): LivePlaybackState? {
    val experimentalPlaybackPersistence = remember {
      experimentalPlaybackPersistenceFeatureFlag.get()
    }
    if (!experimentalPlaybackPersistence || currentBookId == null) return null
    return remember(currentBookId) { playerController.livePlaybackStateFlow(currentBookId) }
      .collectAsState(initial = null)
      .value
  }

  @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
  private fun queryResults() = snapshotFlow {
    query.text.toString().trim()
  }
    .distinctUntilChanged()
  // clearing the field shows the recent searches right away
  .debounce { if (it.isEmpty()) Duration.ZERO else 150.milliseconds }
  // searching again when the library changes keeps the progress and the results up to date
  .combine(repo.flow(), ::Pair)
    .mapLatest { (query, books) ->
      if (query.isEmpty()) {
        null
      } else {
        val results = search.search(query)
        QueryResults(
          query = query,
          results = results,
          groups = withContext(dispatcherProvider.default) { groups(results, books) },
        )
      }
    }
    .onEach { lastResults.value = it }

  fun onSearch() {
    saveRecentSearch(query.text.toString())
  }

  fun onRecentSearchClick(recentSearch: String) {
    query.setTextAndPlaceCursorAtEnd(recentSearch)
    saveRecentSearch(recentSearch)
  }

  fun onRemoveRecentSearch(recentSearch: String) {
    scope.launch {
      recentBookSearchRepo.delete(recentSearch)
    }
  }

  fun onClearRecentSearches() {
    scope.launch {
      recentBookSearchRepo.clear()
    }
  }

  fun onBookClick(id: BookId) {
    saveRecentSearch(query.text.toString())
    navigator.goTo(Destination.Playback(id))
  }

  fun onBookLongClick(id: BookId) {
    navigator.goTo(Destination.BookActions(id))
  }

  internal fun onGroupClick(group: SearchGroupViewState) {
    saveRecentSearch(query.text.toString())
    navigator.goTo(
      when (group.kind) {
        SearchGroupViewState.Kind.Author -> Destination.Author(group.name)
        SearchGroupViewState.Kind.Series -> Destination.Series(group.name)
      },
    )
  }

  fun onPlayClick(id: BookId) {
    saveRecentSearch(query.text.toString())
    scope.launch {
      if (currentBookStore.data.first() == id) {
        playerController.playPause()
      } else {
        playerController.pauseIfCurrentBookDifferentFrom(id)
        currentBookStore.updateData { id }
        playerController.play()
      }
    }
  }

  fun onFoldersClick() {
    navigator.goTo(Destination.FolderPicker)
  }

  fun onBackClick() {
    navigator.goBack()
  }

  private fun saveRecentSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isNotEmpty()) {
      scope.launch {
        recentBookSearchRepo.add(trimmed)
      }
    }
  }
}

internal data class QueryResults(
  val query: String,
  val results: List<BookSearchResult>,
  val groups: List<SearchGroupViewState>,
)

/**
 * The results of the last search. They outlive the search screen, which is disposed while the
 * player is open.
 */
@SingleIn(AppScope::class)
@Inject
class LastBookSearchResults {
  internal var value: QueryResults? = null
}

private const val MIN_GROUP_BOOKS = 2
private const val MAX_GROUPS = 3

private fun groups(
  results: List<BookSearchResult>,
  books: List<Book>,
): List<SearchGroupViewState> {
  fun scores(
    field: BookSearchField,
    key: (Book) -> String?,
  ): Map<String, Int> = buildMap {
    results.forEach { result ->
      val score = result.nameScores[field] ?: return@forEach
      val groupKey = key(result.book) ?: return@forEach
      put(groupKey, maxOf(score, get(groupKey) ?: 0))
    }
  }
  val authorScores = scores(BookSearchField.Author) { it.authorKey }
  val seriesScores = scores(BookSearchField.Series) { it.seriesName?.let(::nameKey) }
  if (authorScores.isEmpty() && seriesScores.isEmpty()) return emptyList()

  val authors = books.byAuthorKey()
  val authorGroups = authorScores.mapNotNull { (key, score) ->
    val authorBooks = authors[key] ?: return@mapNotNull null
    val name = authorBooks.authorName(key) ?: return@mapNotNull null
    val group =
      SearchGroupViewState(
        kind = SearchGroupViewState.Kind.Author,
        key = key,
        name = name,
        bookCount = authorBooks.size,
        covers = AuthorGroup(key, name, libraryEntries(authorBooks).values.flatten()).covers,
      )
    group to score
  }
  val seriesGroups =
    books.series().mapNotNull { series ->
      val score = seriesScores[series.key] ?: return@mapNotNull null
      val group =
        SearchGroupViewState(
          kind = SearchGroupViewState.Kind.Series,
          key = series.key,
          name = series.name,
          bookCount = series.bookCount,
          covers = series.covers,
        )
      group to score
    }
  return (authorGroups + seriesGroups)
    .filter { (group, _) -> group.bookCount >= MIN_GROUP_BOOKS }
    .sortedWith(
      compareByDescending<Pair<SearchGroupViewState, Int>> { (_, score) -> score }
        .thenByDescending { (group, _) -> group.bookCount },
    )
    .take(MAX_GROUPS)
    .map { (group, _) -> group }
    .sortedBy { it.kind == SearchGroupViewState.Kind.Series }
}

private val TagFields =
  listOf(
    BookSearchField.Series,
    BookSearchField.Narrator,
    BookSearchField.Genre,
    BookSearchField.Chapters,
  )

private fun BookSearchResult.toViewState(
  livePlaybackState: LivePlaybackState?,
  serverStates: ServerBookStates,
): SearchResultViewState {
  val book = livePlaybackState?.let(book::overlay) ?: book
  val item = book.toItemViewState(serverStates)
  return SearchResultViewState(
    id = book.id,
    title = HighlightedText(item.name, matches[BookSearchField.Title].orEmpty()),
    author = item.author?.let { HighlightedText(it, matches[BookSearchField.Author].orEmpty()) },
    cover = item.cover,
    category = book.category,
    progress = item.progress,
    remainingTime = item.remainingTime,
    matchTag =
      TagFields.firstNotNullOfOrNull { field ->
        val highlights = matches[field] ?: return@firstNotNullOfOrNull null
        val text = text(field) ?: return@firstNotNullOfOrNull null
        val shown = HighlightedText(text, highlights)
        MatchTag(
          field,
          if (field == BookSearchField.Chapters) shown.nearFirstHighlight() else shown,
        )
      },
    hidden = item.hidden,
    unavailable = item.unavailable,
    download = item.download,
  )
}
