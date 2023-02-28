package voice.app.features.widget

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

class WidgetActionBroadcastReceiver : BroadcastReceiver() {

  private val scope = MainScope()

  @Inject
  lateinit var player: PlayerController

  override fun onReceive(context: Context, intent: Intent?) {
    val action = Action.values().find { it.name == intent?.getStringExtra(ACTION_KEY) }
      ?: return

    Logger.d("onReceive $action")

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
    fun inject(target: WidgetActionBroadcastReceiver)
  }

  internal companion object {

    private const val ACTION_KEY = "action"

    fun pendingIntent(
      context: Context,
      action: Action,
    ): PendingIntent? {
      val intent = Intent("voice.WidgetAction")
        .setComponent(ComponentName(context, WidgetActionBroadcastReceiver::class.java))
        .putExtra(ACTION_KEY, action.name)
      return PendingIntent.getBroadcast(context, action.ordinal, intent, PendingIntent.FLAG_IMMUTABLE)
    }
  }

  internal enum class Action {
    PlayPause, FastForward, Rewind
  }
}
