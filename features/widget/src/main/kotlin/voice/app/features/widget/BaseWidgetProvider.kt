package voice.app.features.widget

import androidx.glance.appwidget.GlanceAppWidget
import voice.features.widget.NowPlayingWidget
import voice.features.widget.WidgetReceiver

// widgets on home screens are bound to this class name, so it must not move
class BaseWidgetProvider : WidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}
