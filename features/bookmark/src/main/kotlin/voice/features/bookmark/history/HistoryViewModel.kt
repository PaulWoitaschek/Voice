package voice.features.bookmark.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.DismissedHistorySuggestionsStore
import voice.core.data.store.ListeningHistoryEnabledStore
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.KioskModeFeatureFlagQualifier
import voice.core.playback.PlayerController
import voice.core.playback.misc.Decibel
import voice.core.playback.playstate.PlayStateManager
import voice.features.bookmark.BookIndex
import voice.features.bookmark.kioskModeHistoryViewState
import voice.navigation.Navigator
import java.time.Clock

/** A suggestion shows for a day at most, so only the latest dismissals matter. */
private const val MAX_DISMISSED_SUGGESTIONS = 20

@AssistedInject
class HistoryViewModel(
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val listeningHistoryRepo: ListeningHistoryRepo,
  @ListeningHistoryEnabledStore
  private val enabledStore: DataStore<Boolean>,
  @DismissedHistorySuggestionsStore
  private val dismissedSuggestionsStore: DataStore<List<Long>>,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  private val navigator: Navigator,
  private val clock: Clock,
  private val dispatcherProvider: DispatcherProvider,
  @KioskModeFeatureFlagQualifier
  private val kioskModeFeatureFlag: FeatureFlag<Boolean>,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope(dispatcherProvider)
  private val pinMutex = Mutex()

  internal val viewEffects: Flow<HistoryViewEffect>
    field = MutableSharedFlow<HistoryViewEffect>(extraBufferCapacity = 1)

  private val selection = MutableStateFlow(Selection(filter = null, source = null))

  @Composable
  internal fun viewState(): HistoryViewState? {
    val kioskMode = remember { kioskModeFeatureFlag.get() }
    if (kioskMode) return kioskModeHistoryViewState()

    val timeline = remember { timeline() }.collectAsState(initial = null).value ?: return null
    val dismissedSuggestions = remember { dismissedSuggestionsStore.data }
      .collectAsState(initial = null).value ?: return null
    // the position only matters while a session is ongoing, so it's not even collected otherwise
    val currentProgress = if (timeline.isOngoing) currentProgress(timeline.index) else null
    return timeline.viewState(
      now = clock.instant(),
      currentProgress = currentProgress,
      dismissedSuggestions = dismissedSuggestions,
    )
  }

  @Composable
  private fun currentProgress(index: BookIndex): Float? {
    val book = remember { bookRepository.flow(bookId) }.collectAsState(initial = null).value ?: return null
    return index.locate(book.content.currentChapter, book.content.positionInChapter)?.progress
  }

  /**
   * The position changes many times a second while playing, but the history only depends on the
   * chapters, so it's built off the main thread only when something it shows changes.
   */
  private fun timeline(): Flow<HistoryTimeline> {
    val playing = playStateManager.playStateFlow
      .map { it == PlayStateManager.PlayState.Playing }
      .distinctUntilChanged()
    return bookRepository.flow(bookId)
      .filterNotNull()
      .distinctUntilChanged { old, new -> old.chapters == new.chapters }
      .mapLatest { book -> withContext(dispatcherProvider.io) { book.content to BookIndex(book) } }
      .flatMapLatest { (content, index) ->
        combine(
          listeningHistoryRepo.events(bookId),
          bookmarkRepo.bookmarksFlow(content),
          enabledStore.data,
          playing,
          selection,
        ) { events, bookmarks, enabled, isPlaying, selected ->
          withContext(dispatcherProvider.io) {
            historyTimeline(
              events = events,
              index = index,
              bookmarks = bookmarks,
              enabled = enabled,
              playing = isPlaying,
              selectedFilter = selected.filter,
              selectedSource = selected.source,
              now = clock.instant(),
              zone = clock.zone,
            )
          }
        }
      }
  }

  internal fun onFilterClick(filter: HistoryFilter?) {
    selection.update { it.copy(filter = if (it.filter == filter) null else filter) }
  }

  internal fun onSourceChange(source: ListeningEvent.Source?) {
    selection.update { it.copy(source = source) }
  }

  internal fun onActionClick(action: HistoryAction) {
    when (action) {
      is HistoryAction.JumpBack -> goTo(action.chapterId, action.time, ListeningEvent.Type.JumpBack)
      is HistoryAction.GoThere -> goTo(action.chapterId, action.time, ListeningEvent.Type.Seek)
      is HistoryAction.Pin -> pin(action)
      is HistoryAction.Restore -> scope.launch {
        bookmarkRepo.addBookmark(action.bookmark)
        viewEffects.tryEmit(HistoryViewEffect.Restored)
      }
      is HistoryAction.ChangeBack -> changeBack(action)
    }
  }

  internal fun onSuggestionBack(suggestion: HistorySuggestion) {
    dismiss(suggestion)
    onActionClick(suggestion.back)
  }

  internal fun onSuggestionKeep(suggestion: HistorySuggestion) {
    dismiss(suggestion)
  }

  internal fun onTurnOnClick() {
    scope.launch {
      enabledStore.updateData { true }
    }
  }

  private fun dismiss(suggestion: HistorySuggestion) {
    scope.launch {
      dismissedSuggestionsStore.updateData { keys ->
        (keys - suggestion.key + suggestion.key).takeLast(MAX_DISMISSED_SUGGESTIONS)
      }
    }
  }

  private fun pin(action: HistoryAction.Pin) {
    scope.launch {
      // the pin stays until the bookmarks are reloaded, so tapping it twice must not save it twice
      val pinned = pinMutex.withLock {
        val book = bookRepository.get(bookId) ?: return@withLock false
        val exists = bookmarkRepo.bookmarks(book.content).any { it.chapterId == action.chapterId && it.time == action.time }
        if (!exists) {
          bookmarkRepo.addBookmark(
            Bookmark(
              bookId = bookId,
              chapterId = action.chapterId,
              title = null,
              time = action.time,
              addedAt = clock.instant(),
              setBySleepTimer = false,
              id = Bookmark.Id.random(),
            ),
          )
        }
        !exists
      }
      if (pinned) {
        viewEffects.tryEmit(HistoryViewEffect.Pinned)
      }
    }
  }

  private fun changeBack(action: HistoryAction.ChangeBack) {
    scope.launch {
      currentBookStore.updateData { bookId }
      when (action.type) {
        ListeningEvent.Type.SpeedChanged -> action.value.toFloatOrNull()?.let { playerController.setSpeed(it) }
        ListeningEvent.Type.VolumeBoostChanged -> action.value.toFloatOrNull()?.let { playerController.setGain(Decibel(it)) }
        ListeningEvent.Type.SkipSilenceChanged -> action.value.toBooleanStrictOrNull()?.let { playerController.skipSilence(it) }
        else -> {}
      }
      viewEffects.tryEmit(HistoryViewEffect.ChangedBack)
    }
  }

  private fun goTo(
    chapterId: ChapterId,
    time: Long,
    type: ListeningEvent.Type,
  ) {
    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.Playing
    scope.launch {
      currentBookStore.updateData { bookId }
      playerController.setPosition(time, chapterId, type)
      if (wasPlaying) {
        playerController.play()
      }
      navigator.goBack()
    }
  }

  private data class Selection(
    val filter: HistoryFilter?,
    val source: ListeningEvent.Source?,
  )

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): HistoryViewModel
  }
}

internal sealed interface HistoryViewEffect {
  data object Pinned : HistoryViewEffect
  data object Restored : HistoryViewEffect
  data object ChangedBack : HistoryViewEffect
}
