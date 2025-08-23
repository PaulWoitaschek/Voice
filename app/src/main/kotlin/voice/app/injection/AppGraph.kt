package voice.app.injection

import voice.app.AppController
import voice.app.features.MainActivity
import voice.app.features.widget.BaseWidgetProvider

interface AppGraph {

  fun inject(target: App)
  fun inject(target: BaseWidgetProvider)
  fun inject(target: AppController)
  fun inject(target: MainActivity)
}
