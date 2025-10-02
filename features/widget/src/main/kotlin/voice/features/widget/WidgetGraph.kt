package voice.features.widget

import voice.app.features.widget.BaseWidgetProvider

interface WidgetGraph {
  fun inject(target: BaseWidgetProvider)
}
