package voice.features.listeningStats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningSessionRepo
import voice.navigation.Navigator
import java.time.Clock

// The ViewModel holds the per-book context and current period selection, keeping stats queries composable and testable.
@AssistedInject
class ListeningStatsViewModel(
  private val bookRepository: BookRepository,
  private val listeningSessionRepo: ListeningSessionRepo,
  private val navigator: Navigator,
  private val clock: Clock,
  @Assisted
  private val bookId: BookId,
) {

  private val selectedPeriod = mutableStateOf(ListeningStatsPeriod.Day)

  // ViewState subscribes to Room flows during composition, following the project's lightweight Compose ViewModel pattern.
  @Composable
  internal fun viewState(): ListeningStatsViewState {
    val period = selectedPeriod.value
    val range = remember(period) {
      period.range(clock.instant(), clock.zone)
    }
    val book by remember(bookId) { bookRepository.flow(bookId) }.collectAsState(initial = null)
    val durationMs by remember(bookId, range) {
      listeningSessionRepo.durationFlow(bookId, range.startedAt, range.endedAt)
    }.collectAsState(initial = 0L)
    return ListeningStatsViewState(
      title = book?.content?.name ?: bookId.value,
      selectedPeriod = period,
      durationMs = durationMs,
      empty = durationMs == 0L,
    )
  }

  // Switching periods only changes the query range, never the underlying stats.
  internal fun selectPeriod(period: ListeningStatsPeriod) {
    selectedPeriod.value = period
  }

  // Close is delegated to the navigator so the screen never owns navigation-stack details.
  internal fun close() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    // The stats page is opened from the per-book menu, so the factory only needs the book id.
    fun create(bookId: BookId): ListeningStatsViewModel
  }
}
