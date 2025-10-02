package voice.core.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import coil.Coil
import coil.ImageLoader
import com.google.android.material.color.DynamicColors
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import voice.core.data.store.DarkThemeStore
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
@Inject
class UIAppStartInitializer(
  @DarkThemeStore
  private val useDarkThemeStore: DataStore<Boolean>,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    DynamicColors.applyToActivitiesIfAvailable(application)
    Coil.setImageLoader(
      ImageLoader.Builder(application)
        .addLastModifiedToFileCacheKey(false)
        .build(),
    )
    if (DARK_THEME_SETTABLE) {
      useDarkThemeStore.data
        .distinctUntilChanged()
        .onEach { useDarkTheme ->
          val nightMode = if (useDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
          AppCompatDelegate.setDefaultNightMode(nightMode)
        }
        .launchIn(scope)
    }
  }
}
