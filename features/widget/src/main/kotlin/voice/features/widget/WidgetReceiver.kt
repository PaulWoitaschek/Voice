package voice.features.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver

abstract class WidgetReceiver : GlanceAppWidgetReceiver() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    widgetGraph.placedWidgets.refresh()
  }

  override fun onDeleted(
    context: Context,
    appWidgetIds: IntArray,
  ) {
    super.onDeleted(context, appWidgetIds)
    widgetGraph.placedWidgets.refresh()
  }
}

class SmallNowPlayingWidgetReceiver : WidgetReceiver() {
  override val glanceAppWidget = NowPlayingWidget()
}

class ShelfWidgetReceiver : WidgetReceiver() {
  override val glanceAppWidget = ShelfWidget()
}

class SmallShelfWidgetReceiver : WidgetReceiver() {
  override val glanceAppWidget = ShelfWidget()
}

class SleepTimerWidgetReceiver : WidgetReceiver() {
  override val glanceAppWidget = SleepTimerWidget()
}
