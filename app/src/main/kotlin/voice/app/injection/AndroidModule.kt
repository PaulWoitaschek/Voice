package voice.app.injection

import android.app.Application
import android.content.Context
import android.os.PowerManager
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import voice.app.misc.AppInfoProviderImpl
import voice.app.misc.MainActivityIntentProviderImpl
import voice.common.AppInfoProvider
import voice.common.AppScope
import voice.common.DispatcherProvider
import voice.playback.notification.MainActivityIntentProvider
import java.time.Clock
import javax.inject.Singleton

/**
 * Module providing Android SDK Related instances.
 */
@Module
@ContributesTo(AppScope::class)
object AndroidModule {

  @Provides
  fun provideContext(app: Application): Context = app

  @Provides
  @Singleton
  fun providePowerManager(context: Context): PowerManager {
    return context.getSystemService(Context.POWER_SERVICE) as PowerManager
  }

  @Provides
  fun toToBookIntentProvider(impl: MainActivityIntentProviderImpl): MainActivityIntentProvider = impl

  @Provides
  fun applicationIdProvider(impl: AppInfoProviderImpl): AppInfoProvider = impl

  @Provides
  @Singleton
  fun json(): Json {
    return Json.Default
  }

  @Provides
  @Singleton
  fun dispatcherProvider(): DispatcherProvider {
    return DispatcherProvider(
      main = Dispatchers.Main,
      io = Dispatchers.IO,
    )
  }

  @Provides
  fun clock(): Clock = Clock.systemDefaultZone()
}
