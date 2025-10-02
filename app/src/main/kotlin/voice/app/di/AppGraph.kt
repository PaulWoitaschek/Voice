package voice.app.di

import voice.app.features.widget.BaseWidgetProvider
import voice.features.widget.WidgetGraph

interface AppGraph : WidgetGraph {

  fun inject(target: App)
  override fun inject(target: BaseWidgetProvider)
}
