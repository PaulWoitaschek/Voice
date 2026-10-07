package voice.features.bookmark.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
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

@AssistedInject
class HistoryViewModel(
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val listeningHistoryRepo: ListeningHistoryRepo,
  @ListeningHistoryEnabledStore
  private val enabledStore: DataStore<Boolean>,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  private val navigator: Navigator,
  private val clock: Clock,
  dispatcherProvider: DispatcherProvider,
  @KioskModeFeatureFlagQualifier
  private val kioskModeFeatureFlag: FeatureFlag<Boolean>,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope(dispatcherProvider)

  internal val viewEffects: Flow<HistoryViewEffect>
    field = MutableSharedFlow<HistoryViewEffect>(extraBufferCapacity = 1)

  private var selectedFilter by mutableStateOf<HistoryFilter?>(null)
  private var selectedSource by mutableStateOf<ListeningEvent.Source?>(null)
  private var dismissedSuggestion by mutableStateOf<Long?>(null)

  @Composable
  internal fun viewState(): HistoryViewState? {
    val kioskMode = remember { kioskModeFeatureFlag.get() }
    if (kioskMode) return kioskModeHistoryViewState()

    val book = remember(bookId) { bookRepository.flow(bookId) }
      .collectAsState(initial = null).value ?: return null
    val events = remember(bookId) { listeningHistoryRepo.events(bookId) }
      .collectAsState(initial = null).value ?: return null
    val bookmarks = remember(book.id, book.content.chapters) {
      bookmarkRepo.bookmarksFlow(book.content)
    }.collectAsState(initial = null).value ?: return null
    val enabled = remember { enabledStore.data }.collectAsState(initial = null).value ?: return null
    val playState by remember { playStateManager.playStateFlow }.collectAsState()
    val index = remember(book) { BookIndex(book) }
    return historyViewState(
      events = events,
      index = index,
      bookmarks = bookmarks,
      enabled = enabled,
      playing = playState == PlayStateManager.PlayState.Playing,
      selectedFilter = selectedFilter,
      selectedSource = selectedSource,
      dismissedSuggestion = dismissedSuggestion,
      now = clock.instant(),
      zone = clock.zone,
    )
  }

  internal fun onFilterClick(filter: HistoryFilter?) {
    selectedFilter = if (selectedFilter == filter) null else filter
  }

  internal fun onSourceChange(source: ListeningEvent.Source?) {
    selectedSource = source
  }

  internal fun onActionClick(action: HistoryAction) {
    when (action) {
      is HistoryAction.JumpBack -> goTo(action.chapterId, action.time, ListeningEvent.Type.JumpBack)
      is HistoryAction.GoThere -> goTo(action.chapterId, action.time, ListeningEvent.Type.Seek)
      is HistoryAction.Pin -> scope.launch {
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
        viewEffects.tryEmit(HistoryViewEffect.Pinned)
      }
      is HistoryAction.Restore -> scope.launch {
        bookmarkRepo.addBookmark(action.bookmark)
        viewEffects.tryEmit(HistoryViewEffect.Restored)
      }
      is HistoryAction.ChangeBack -> changeBack(action)
    }
  }

  internal fun onSuggestionBack(suggestion: HistorySuggestion) {
    dismissedSuggestion = suggestion.key
    onActionClick(suggestion.back)
  }

  internal fun onSuggestionKeep(suggestion: HistorySuggestion) {
    dismissedSuggestion = suggestion.key
  }

  internal fun onTurnOnClick() {
    scope.launch {
      enabledStore.updateData { true }
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
