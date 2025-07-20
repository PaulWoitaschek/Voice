package voice.app.injection

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import coil.Coil
import coil.ImageLoader
import com.google.android.material.color.DynamicColors
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import voice.app.features.widget.TriggerWidgetOnChange
import voice.app.scanner.MediaScanTrigger
import voice.common.DARK_THEME_SETTABLE
import voice.common.pref.DarkThemeStore
import voice.common.rootComponent
import voice.sleepTimer.AutoEnableSleepTimer
import javax.inject.Inject

open class App : Application() {

  @Inject
  lateinit var mediaScanner: MediaScanTrigger

  @Inject
  lateinit var triggerWidgetOnChange: TriggerWidgetOnChange

  @Inject
  lateinit var autoEnableSleepTimer: AutoEnableSleepTimer

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

    appComponent = createAppComponent()
    rootComponent = appComponent
    appComponent.inject(this)

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

    mediaScanner.scan()
    triggerWidgetOnChange.init()
    autoEnableSleepTimer.startMonitoring()
  }

  open fun createAppComponent(): AppComponent {
    return createGraphFactory<ProductionAppComponent.Factory>().create(this)
  }
}

lateinit var appComponent: AppComponent
  @VisibleForTesting set
