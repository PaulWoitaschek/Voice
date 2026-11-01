package voice.core.playback.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.PlayerControllerImpl

class WidgetButtonReceiver : BroadcastReceiver() {

  private val scope = MainScope()

  @Inject lateinit var player: PlayerControllerImpl

  @Inject lateinit var bookRepository: BookRepository

  @Inject lateinit var currentBookStore: CurrentBookStore

  override fun onReceive(
    context: Context,
    intent: Intent?,
  ) {
    val action = Action.parse(intent) ?: return

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
        player.pauseIfCurrentBookDifferentFrom(id, ListeningEvent.Source.Widget)
        currentBookStore.updateData { id }
        player.play(ListeningEvent.Source.Widget)
      }
      else -> Logger.d("Can't play a book that's not in the library")
    }
  }

  private suspend fun startOver() {
    val bookId = currentBookStore.data.first() ?: return
    val book = bookRepository.get(bookId) ?: return
    player.setPosition(0, book.chapters.first().id, source = ListeningEvent.Source.Widget)
    player.play(ListeningEvent.Source.Widget)
  }

  @ContributesTo(AppScope::class)
  interface Graph {
    fun inject(target: WidgetButtonReceiver)
  }

  // the names are sent by automation apps like Tasker, so they must not change
  enum class Action {
    PlayPause,
    FastForward,
    Rewind,
    PlayBook,
    StartOver;

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

@ContributesBinding(AppScope::class)
@Inject
class WidgetButtonIntentsImpl(private val context: Context) : WidgetButtonIntents {

  override fun playPause(): Intent = intent(WidgetButtonReceiver.Action.PlayPause)

  override fun rewind(): Intent = intent(WidgetButtonReceiver.Action.Rewind)

  override fun fastForward(): Intent = intent(WidgetButtonReceiver.Action.FastForward)

  override fun startOver(): Intent = intent(WidgetButtonReceiver.Action.StartOver)

  override fun playBook(id: BookId): Intent {
    return intent(WidgetButtonReceiver.Action.PlayBook).putExtra(BOOK_ID_KEY, id.value)
  }

  private fun intent(action: WidgetButtonReceiver.Action): Intent {
    return Intent(WIDGET_ACTION).setComponent(ComponentName(context, WidgetButtonReceiver::class.java)).putExtra(ACTION_KEY, action.name)
  }
}

private const val ACTION_KEY = "action"
private const val BOOK_ID_KEY = "bookId"
private const val WIDGET_ACTION = "voice.WidgetAction"
