package voice.app.injection

import android.app.Application
import android.content.Context
import android.os.PowerManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import voice.app.misc.AppInfoProviderImpl
import voice.app.misc.MainActivityIntentProviderImpl
import voice.common.AppInfoProvider
import voice.common.DispatcherProvider
import voice.playback.notification.MainActivityIntentProvider
import java.time.Clock

/**
 * Module providing Android SDK Related instances.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object AndroidModule {

  @Provides
  fun provideContext(app: Application): Context = app

  @Provides
  @SingleIn(AppScope::class)
  fun providePowerManager(context: Context): PowerManager {
    return context.getSystemService(Context.POWER_SERVICE) as PowerManager
  }

  @Provides
  fun toToBookIntentProvider(impl: MainActivityIntentProviderImpl): MainActivityIntentProvider = impl

  @Provides
  fun applicationIdProvider(impl: AppInfoProviderImpl): AppInfoProvider = impl

  @Provides
  @SingleIn(AppScope::class)
  fun json(): Json {
    return Json.Default
  }

  @Provides
  @SingleIn(AppScope::class)
  fun dispatcherProvider(): DispatcherProvider {
    return DispatcherProvider(
      main = Dispatchers.Main,
      io = Dispatchers.IO,
    )
  }

  @Provides
  fun clock(): Clock = Clock.systemDefaultZone()
}
