package voice.features.sleepTimer.rewind

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.sleeptimer.SleepTimerRewind
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.SleepTimerRewindStore
import voice.core.playback.PlayerController

internal data class SleepTimerRewindViewState(val options: List<RewindOption>)

@Inject
class SleepTimerRewindViewModel(
  @SleepTimerRewindStore
  private val sleepTimerRewindStore: DataStore<SleepTimerRewind?>,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val player: PlayerController,
  dispatcherProvider: DispatcherProvider,
) {

  private val scope = MainScope(dispatcherProvider)

  @Composable
  internal fun viewState(): SleepTimerRewindViewState? {
    val rewind = remember { sleepTimerRewindStore.data }.collectAsState(initial = null).value
      ?: return null
    val currentBookId = remember { currentBookStore.data }.collectAsState(initial = null).value
    // the player can only seek within the current book; once another book plays the rewind is cleared anyway
    if (rewind.bookId != currentBookId) return null
    return SleepTimerRewindViewState(options = rewind.rewindOptions())
  }

  internal fun onRewind(option: RewindOption) {
    scope.launch {
      val rewind = sleepTimerRewindStore.data.first() ?: return@launch
      val book = bookRepository.get(rewind.bookId)
      if (book != null) {
        val position = book.chapterPosition(option.positionInBookMs)
        player.setPosition(time = position.positionInChapterMs, id = position.chapterId)
      }
      sleepTimerRewindStore.updateData { null }
    }
  }

  internal fun onDismiss() {
    scope.launch {
      sleepTimerRewindStore.updateData { null }
    }
  }
}
