package voice.core.remoteconfig.api

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
class LoadRemoteConfigOnAppStart(
  private val remoteConfig: RemoteConfig,
  dispatcherProvider: DispatcherProvider,
) : AppInitializer {

  private val mainScope = MainScope(dispatcherProvider)

  override fun onAppStart(application: Application) {
    mainScope.launch {
      remoteConfig.refresh()
    }
  }
}
