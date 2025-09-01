package voice.app.di

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import coil.Coil
import coil.ImageLoader
import com.google.android.material.color.DynamicColors
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import voice.core.common.rootGraph
import voice.core.data.store.DarkThemeStore
import voice.core.initializer.AppInitializer
import voice.core.scanner.MediaScanTrigger
import voice.core.ui.DARK_THEME_SETTABLE

open class App : Application() {

  @Inject
  lateinit var mediaScanner: MediaScanTrigger

  @Inject
  lateinit var appInitializers: Set<AppInitializer>

  @field:[
  Inject
  DarkThemeStore
  ]
  lateinit var useDarkThemeStore: DataStore<Boolean>

  override fun onCreate() {
    super.onCreate()

    Coil.setImageLoader(
      ImageLoader.Builder(this)
        .addLastModifiedToFileCacheKey(false)
        .build(),
    )

    DynamicColors.applyToActivitiesIfAvailable(this)

    appGraph = createGraph()
    rootGraph = appGraph
    appGraph.inject(this)

    if (DARK_THEME_SETTABLE) {
      MainScope().launch {
        useDarkThemeStore.data
          .distinctUntilChanged()
          .collect { useDarkTheme ->
            val nightMode = if (useDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(nightMode)
          }
      }
    }

    appInitializers.forEach {
      it.onAppStart(this)
    }
    mediaScanner.scan()
  }

  open fun createGraph(): AppGraph {
    return createGraphFactory<ProductionAppGraph.Factory>().create(this)
  }
}

lateinit var appGraph: AppGraph
  @VisibleForTesting set
