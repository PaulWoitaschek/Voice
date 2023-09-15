package voice.playback.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import voice.common.AppScope
import voice.common.rootComponentAs
import voice.logging.core.Logger
import voice.playback.PlayerController
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class WidgetButtonReceiver : BroadcastReceiver() {

  private val scope = MainScope()

  @Inject
  lateinit var player: PlayerController

  override fun onReceive(
    context: Context,
    intent: Intent?,
  ) {
    val action = Action.parse(intent)
    Logger.d("onReceive ${intent?.action}. Parsed to $action")
    action ?: return

    rootComponentAs<Component>().inject(this)

    val result = goAsync()
    scope.launch {
      try {
        withTimeout(20.seconds) {
          player.awaitConnect()
          when (action) {
            Action.PlayPause -> player.playPause()
            Action.FastForward -> {
              player.fastForward()
              player.play()
            }

            Action.Rewind -> {
              player.rewind()
              player.play()
            }
          }
        }
      } finally {
        result.finish()
      }
    }
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: WidgetButtonReceiver)
  }

  companion object {

    private const val ACTION_KEY = "action"
    private const val WIDGET_ACTION = "voice.WidgetAction"

    fun pendingIntent(
      context: Context,
      action: Action,
    ): PendingIntent? {
      val intent = Intent(WIDGET_ACTION)
        .setComponent(ComponentName(context, WidgetButtonReceiver::class.java))
        .putExtra(ACTION_KEY, action.name)
      return PendingIntent.getBroadcast(
        context,
        action.ordinal,
        intent,
        PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }

  enum class Action {
    PlayPause,
    FastForward,
    Rewind,
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
