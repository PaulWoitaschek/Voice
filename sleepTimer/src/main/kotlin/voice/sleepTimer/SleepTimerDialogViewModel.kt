package voice.sleepTimer

import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.DispatcherProvider
import voice.common.pref.PrefKeys
import voice.data.repo.BookRepository
import voice.data.repo.BookmarkRepo
import javax.inject.Inject
import javax.inject.Named

class SleepTimerDialogViewModel
@Inject constructor(
  private val bookmarkRepo: BookmarkRepo,
  private val sleepTimer: SleepTimer,
  private val bookRepo: BookRepository,
  @Named(PrefKeys.SLEEP_TIME)
  private val sleepTimePref: Pref<Int>,
  dispatcherProvider: DispatcherProvider,
) {

  private val scope = CoroutineScope(dispatcherProvider.main + SupervisorJob())

  private val selectedMinutes = MutableStateFlow(sleepTimePref.value)

  fun viewState(): Flow<SleepTimerDialogViewState> {
    return selectedMinutes
      .map { selectedMinutes ->
        SleepTimerDialogViewState(
          selectedMinutes = selectedMinutes,
          showFab = selectedMinutes > 0
        )
      }
  }

  fun onNumberClicked(number: Int) {
    require(number in 0..9)
    selectedMinutes.update { oldValue ->
      val newValue = (oldValue * 10 + number)
      if (newValue > 999) {
        oldValue
      } else {
        newValue
      }
    }
  }

  fun onNumberDeleteClicked() {
    selectedMinutes.update { it / 10 }
  }

  fun onNumberDeleteLongClicked() {
    selectedMinutes.update { 0 }
  }

  fun onConfirmButtonClicked(bookId: BookId) {
    check(selectedMinutes.value > 0)
    sleepTimePref.value = selectedMinutes.value
    scope.launch {
      val book = bookRepo.get(bookId) ?: return@launch
      bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        setBySleepTimer = true,
        title = null
      )
    }
    sleepTimer.setActive(true)
  }
}

data class SleepTimerDialogViewState(
  val selectedMinutes: Int,
  val showFab: Boolean,
)
