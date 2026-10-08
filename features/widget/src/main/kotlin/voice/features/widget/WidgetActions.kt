package voice.features.widget

import android.content.Context
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import dev.zacsweers.metro.Inject
import voice.core.data.BookId
import voice.core.playback.notification.MainActivityIntentProvider
import voice.core.playback.receiver.WidgetButtonReceiver

@Inject
class WidgetActions(
  private val context: Context,
  private val mainActivityIntentProvider: MainActivityIntentProvider,
) {

  internal fun playPause(): Action = broadcast(WidgetButtonReceiver.Action.PlayPause)

  internal fun rewind(): Action = broadcast(WidgetButtonReceiver.Action.Rewind)

  internal fun fastForward(): Action = broadcast(WidgetButtonReceiver.Action.FastForward)

  internal fun startOver(): Action = broadcast(WidgetButtonReceiver.Action.StartOver)

  internal fun toggleSleepTimer(): Action = broadcast(WidgetButtonReceiver.Action.ToggleSleepTimer)

  internal fun playBook(id: BookId): Action = actionSendBroadcast(WidgetButtonReceiver.playBookIntent(context, id))

  internal fun openPlayer(): Action = actionStartActivity(mainActivityIntentProvider.currentBookIntent())

  internal fun openLibrary(): Action = actionStartActivity(mainActivityIntentProvider.libraryIntent())

  private fun broadcast(action: WidgetButtonReceiver.Action): Action {
    return actionSendBroadcast(WidgetButtonReceiver.intent(context, action))
  }
}
