package voice.features.bookmark.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.features.bookmark.BookIndex
import voice.features.bookmark.kioskModeHistoryViewState
import voice.navigation.Navigator
import java.time.Clock

@AssistedInject
class HistoryViewModel(
  private val currentBookStore: CurrentBookStore,
  private val bookRepository: BookRepository,
  private val listeningHistoryRepo: ListeningHistoryRepo,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  private val navigator: Navigator,
  private val clock: Clock,
  private val dispatcherProvider: DispatcherProvider,
  private val kioskModeFeatureFlag: KioskModeFeatureFlag,
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
    return remember { viewStates() }.collectAsState(initial = null).value
  }

  /**
   * The position changes many times a second while playing, but the history only depends on the
   * chapters, so it's built off the main thread only when something it shows changes.
   */
  private fun viewStates(): Flow<HistoryViewState> {
    return bookRepository.flow(bookId)
      .filterNotNull()
      .distinctUntilChanged { old, new -> old.chapters == new.chapters }
      .mapLatest { book -> withContext(dispatcherProvider.io) { BookIndex(book) } }
      .flatMapLatest { index ->
        listeningHistoryRepo.events(bookId).map { events ->
          withContext(dispatcherProvider.io) {
            historyViewState(events = events, index = index, now = clock.instant(), zone = clock.zone)
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

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): HistoryViewModel
  }
}
