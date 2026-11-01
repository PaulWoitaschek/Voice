package voice.features.widget

import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import dev.zacsweers.metro.Inject
import voice.core.data.BookId
import voice.core.playback.notification.MainActivityIntentProvider
import voice.core.playback.receiver.WidgetButtonIntents

@Inject
class WidgetActions(
  private val widgetButtonIntents: WidgetButtonIntents,
  private val mainActivityIntentProvider: MainActivityIntentProvider,
) {

  internal fun playPause(): Action = actionSendBroadcast(widgetButtonIntents.playPause())

  internal fun rewind(): Action = actionSendBroadcast(widgetButtonIntents.rewind())

  internal fun fastForward(): Action = actionSendBroadcast(widgetButtonIntents.fastForward())

  internal fun startOver(): Action = actionSendBroadcast(widgetButtonIntents.startOver())

  internal fun playBook(id: BookId): Action = actionSendBroadcast(widgetButtonIntents.playBook(id))

  internal fun openPlayer(): Action =
    actionStartActivity(mainActivityIntentProvider.currentBookIntent())

  internal fun openLibrary(): Action =
    actionStartActivity(mainActivityIntentProvider.libraryIntent())
}
