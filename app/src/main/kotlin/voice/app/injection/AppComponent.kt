package voice.app.injection

import android.app.Application
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import voice.app.AppController
import voice.app.features.MainActivity
import voice.app.features.bookOverview.EditCoverDialogController
import voice.app.features.widget.BaseWidgetProvider
import voice.common.AppScope
import javax.inject.Singleton

@Singleton
@MergeComponent(
  scope = AppScope::class,
)
interface AppComponent {

  fun inject(target: App)
  fun inject(target: BaseWidgetProvider)
  fun inject(target: AppController)
  fun inject(target: EditCoverDialogController)
  fun inject(target: MainActivity)

  @Component.Factory
  interface Factory {
    fun create(@BindsInstance application: Application): AppComponent
  }

  companion object {
    fun factory(): Factory = DaggerAppComponent.factory()
  }
}
