package voice.app

import android.app.Application
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import voice.app.injection.AppComponent
import voice.common.AppScope
import javax.inject.Singleton

@Singleton
@MergeComponent(
  scope = AppScope::class,
)
interface TestComponent : AppComponent {

  fun inject(target: SleepTimerIntegrationTest)

  @Component.Factory
  interface Factory {
    fun create(@BindsInstance application: Application): TestComponent
  }

  companion object {
    fun factory(): Factory = DaggerTestComponent.factory()
  }
}
