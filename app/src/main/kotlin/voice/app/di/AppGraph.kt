package voice.app.di

import voice.app.features.widget.BaseWidgetProvider

interface AppGraph {

  fun inject(target: App)
  fun inject(target: BaseWidgetProvider)
}
