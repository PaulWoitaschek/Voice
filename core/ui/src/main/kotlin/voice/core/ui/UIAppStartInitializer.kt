package voice.core.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil.Coil
import coil.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
class UIAppStartInitializer(
  private val themeModeStore: ThemeModeStore,
  private val scope: CoroutineScope,
  private val okHttpClient: Lazy<OkHttpClient>,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    Coil.setImageLoader(
      ImageLoader.Builder(application)
        .okHttpClient { okHttpClient.value }
        .addLastModifiedToFileCacheKey(false)
        .build(),
    )
    themeModeStore.data
      .distinctUntilChanged()
      .onEach { themeMode ->
        val nightMode =
          when (themeMode) {
            ThemeMode.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
          }
        AppCompatDelegate.setDefaultNightMode(nightMode)
      }
      .launchIn(scope)
  }
}
