package voice.app.injection

import android.app.Application
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import voice.common.AppScope
import javax.inject.Singleton

@Singleton
@MergeComponent(
  scope = AppScope::class,
)
interface ProductionAppComponent : AppComponent {

  @Component.Factory
  interface Factory {
    fun create(@BindsInstance application: Application): ProductionAppComponent
  }

  companion object {
    fun factory(): Factory = DaggerProductionAppComponent.factory()
  }
}
