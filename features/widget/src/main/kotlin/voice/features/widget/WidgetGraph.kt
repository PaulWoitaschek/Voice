package voice.features.widget

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import voice.core.common.rootGraphAs

@ContributesTo(AppScope::class)
interface WidgetGraph {
  val widgetData: WidgetData
  val widgetImages: WidgetImages
  val widgetActions: WidgetActions
  val placedWidgets: PlacedWidgets
}

internal val widgetGraph: WidgetGraph
  get() = rootGraphAs()
