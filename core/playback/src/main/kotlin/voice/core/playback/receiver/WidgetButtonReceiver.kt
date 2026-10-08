package voice.core.playback.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.SleepTimerPreferenceStore
import voice.core.logging.api.Logger
import voice.core.playback.PlayerController
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import kotlin.time.Duration.Companion.seconds

class WidgetButtonReceiver : BroadcastReceiver() {

  private val scope = MainScope()

  @Inject
  lateinit var player: PlayerController

  @Inject
  lateinit var bookRepository: BookRepository

  @Inject
  lateinit var sleepTimer: SleepTimer

  @Inject
  @CurrentBookStore
  lateinit var currentBookStore: DataStore<BookId?>

  @Inject
  @SleepTimerPreferenceStore
  lateinit var sleepTimerPreferenceStore: DataStore<SleepTimerPreference>

  override fun onReceive(
    context: Context,
    intent: Intent?,
  ) {
    val action = Action.parse(intent)
    Logger.d("onReceive ${intent?.action}. Parsed to $action")
    action ?: return

    rootGraphAs<Graph>().inject(this)

    val result = goAsync()
    scope.launch {
      try {
        withTimeout(20.seconds) {
          player.awaitConnect()
          when (action) {
            Action.PlayPause -> player.playPause(ListeningEvent.Source.Widget)
            Action.FastForward -> {
              player.fastForward(ListeningEvent.Source.Widget)
              player.play(ListeningEvent.Source.Widget)
            }
            Action.Rewind -> {
              player.rewind(ListeningEvent.Source.Widget)
              player.play(ListeningEvent.Source.Widget)
            }
            Action.PlayBook -> {
              val bookId = intent?.getStringExtra(BOOK_ID_KEY)?.let(::BookId)
              if (bookId != null) {
                playBook(bookId)
              }
            }
            Action.StartOver -> startOver()
            Action.ToggleSleepTimer -> toggleSleepTimer()
          }
        }
      } finally {
        result.finish()
      }
    }
  }

  private suspend fun playBook(id: BookId) {
    when {
      currentBookStore.data.first() == id -> player.playPause(ListeningEvent.Source.Widget)
      bookRepository.get(id)?.content?.isActive == true -> {
        player.pauseIfCurrentBookDifferentFrom(id)
        currentBookStore.updateData { id }
        player.play(ListeningEvent.Source.Widget)
      }
      else -> Logger.w("Can't play $id, it's not in the library")
    }
  }

  private suspend fun startOver() {
    val bookId = currentBookStore.data.first() ?: return
    val book = bookRepository.get(bookId) ?: return
    player.setPosition(0, book.chapters.first().id)
    player.play(ListeningEvent.Source.Widget)
  }

  private suspend fun toggleSleepTimer() {
    if (sleepTimer.state.value.enabled) {
      sleepTimer.disable()
    } else {
      val duration = sleepTimerPreferenceStore.data.first().duration
      sleepTimer.enable(SleepTimerMode.TimedWithDuration(duration))
    }
  }

  @ContributesTo(AppScope::class)
  interface Graph {
    fun inject(target: WidgetButtonReceiver)
  }

  companion object {

    private const val ACTION_KEY = "action"
    private const val BOOK_ID_KEY = "bookId"
    private const val WIDGET_ACTION = "voice.WidgetAction"

    fun intent(
      context: Context,
      action: Action,
    ): Intent {
      return Intent(WIDGET_ACTION)
        .setComponent(ComponentName(context, WidgetButtonReceiver::class.java))
        .putExtra(ACTION_KEY, action.name)
    }

    fun playBookIntent(
      context: Context,
      bookId: BookId,
    ): Intent {
      return intent(context, Action.PlayBook)
        .putExtra(BOOK_ID_KEY, bookId.value)
    }
  }

  // the names are sent by automation apps like Tasker, so they must not change
  enum class Action {
    PlayPause,
    FastForward,
    Rewind,
    PlayBook,
    StartOver,
    ToggleSleepTimer,
    ;

    companion object {
      fun parse(intent: Intent?): Action? {
        return when (intent?.action) {
          WIDGET_ACTION -> {
            entries.find { it.name == intent.getStringExtra(ACTION_KEY) }
          }
          else -> null
        }
      }
    }
  }
}
