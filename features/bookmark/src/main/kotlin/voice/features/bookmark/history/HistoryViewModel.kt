package voice.features.bookmark.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.store.CurrentBookStore
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.KioskModeFeatureFlagQualifier
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.features.bookmark.BookIndex
import voice.features.bookmark.kioskModeHistoryViewState
import voice.navigation.Navigator
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@AssistedInject
class HistoryViewModel(
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val listeningHistoryRepo: ListeningHistoryRepo,
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

  // going somewhere leaves the screen, so a second tap must not go back once more
  private var leaving = false

  @Composable
  internal fun viewState(): HistoryViewState? {
    val kioskMode = remember { kioskModeFeatureFlag.get() }
    if (kioskMode) return kioskModeHistoryViewState()

    val stretches = remember { stretches() }.collectAsState(initial = null).value ?: return null
    // the position only matters while a stretch is still playing, so it's not even collected otherwise
    val current = if (stretches.isOngoing) currentSpot() else null
    return stretches.viewState(now = clock.instant(), current = current)
  }

  @Composable
  private fun currentSpot(): HistorySpot? {
    val book = remember { bookRepository.flow(bookId) }.collectAsState(initial = null).value ?: return null
    return HistorySpot(book.content.currentChapter, book.content.positionInChapter)
  }

  /**
   * The position changes many times a second while playing, but the history only depends on the
   * chapters, so it's built off the main thread only when something it shows changes.
   */
  private fun stretches(): Flow<HistoryStretches> {
    val playing = combine(playStateManager.playStateFlow, currentBookStore.data) { state, current ->
      state == PlayStateManager.PlayState.Playing && current == bookId
    }
      .distinctUntilChanged()
      // the pause is written a moment after playback stops, until then the stretch would look cut off
      .debounce { if (it) Duration.ZERO else PAUSE_SETTLE }
    return bookRepository.flow(bookId)
      .filterNotNull()
      .distinctUntilChanged { old, new -> old.chapters == new.chapters }
      .mapLatest { book -> withContext(dispatcherProvider.io) { BookIndex(book) } }
      .flatMapLatest { index ->
        combine(listeningHistoryRepo.events(bookId), playing) { events, isPlaying ->
          withContext(dispatcherProvider.io) {
            historyStretches(
              events = events,
              index = index,
              playing = isPlaying,
              now = clock.instant(),
              zone = clock.zone,
            )
          }
        }
      }
  }

  internal fun onGoTo(spot: HistorySpot) {
    if (leaving) return
    leaving = true
    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.Playing
    scope.launch {
      currentBookStore.updateData { bookId }
      // a seek, so the player offers the way back
      playerController.setPosition(spot.time, spot.chapterId, ListeningEvent.Type.Seek)
      if (wasPlaying) {
        playerController.play()
      }
      navigator.goBack()
    }
  }

  private companion object {
    val PAUSE_SETTLE = 1.seconds
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): HistoryViewModel
  }
}
