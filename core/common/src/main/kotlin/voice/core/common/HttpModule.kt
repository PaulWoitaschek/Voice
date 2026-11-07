package voice.core.common

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okhttp3.OkHttpClient

@BindingContainer
@ContributesTo(AppScope::class)
object HttpModule {

  // Clients that need their own settings derive from this one with newBuilder(), so all of them
  // share one connection pool and its threads
  @Provides @SingleIn(AppScope::class) fun okHttpClient(): OkHttpClient = OkHttpClient()
}
