package voice.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import voice.app.navigation.BottomSheetSceneStrategy
import voice.app.navigation.NavEntryResolver
import voice.app.navigation.StartDestinationProvider
import voice.core.analytics.api.Analytics
import voice.core.common.rootGraphAs
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.data.store.ThemeColorSchemeStore
import voice.core.data.store.ThemeModeStore
import voice.core.logging.api.Logger
import voice.core.ui.LocalSharedTransitionScope
import voice.core.ui.VoiceTheme
import voice.features.review.ReviewFeature
import voice.navigation.Destination
import voice.navigation.NavigationCommand
import voice.navigation.Navigator

@ContributesTo(AppScope::class)
interface MainActivityGraph {
  fun inject(activity: MainActivity)
}

class MainActivity : AppCompatActivity() {

  @Inject
  private lateinit var navigator: Navigator

  @Inject
  lateinit var navEntryResolver: NavEntryResolver

  @Inject
  private lateinit var startDestinationProvider: StartDestinationProvider

  @Inject
  private lateinit var analytics: Analytics

  @Inject
  @ThemeModeStore
  private lateinit var themeModeStore: DataStore<ThemeMode>

  @Inject
  @ThemeColorSchemeStore
  private lateinit var themeColorSchemeStore: DataStore<ThemeColorScheme>

  @OptIn(ExperimentalSharedTransitionApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    rootGraphAs<MainActivityGraph>().inject(this)
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    setContent {
      @Suppress("UNCHECKED_CAST")
      val backStack = rememberNavBackStack(*startDestinationProvider(intent).toTypedArray()) as MutableList<Destination.Compose>
      LaunchedEffect(backStack.last()) {
        analytics.screenView(backStack.last().trackingName)
      }
      val themeMode = themeModeStore.data.collectAsState(initial = null).value
        ?: return@setContent
      val themeColorScheme = themeColorSchemeStore.data.collectAsState(initial = null).value
        ?: return@setContent
      VoiceTheme(
        themeMode = themeMode,
        themeColorScheme = themeColorScheme,
      ) {
        val bottomSheetStrategy = remember { BottomSheetSceneStrategy<Destination.Compose>() }
        val dialogStrategy = remember { DialogSceneStrategy<Destination.Compose>() }
        val density = LocalDensity.current

        SharedTransitionLayout {
          CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
              backStack = backStack,
              sceneStrategies = listOf(bottomSheetStrategy, dialogStrategy),
              sharedTransitionScope = this,
              transitionSpec = {
                if (isBookOverviewPlaybackTransition(initialState.destination(), targetState.destination())) {
                  SharedZAxisEnterTransition togetherWith SharedZAxisExitTransition
                } else {
                  SharedXAxisEnterTransition(density) togetherWith SharedXAxisExitTransition(density)
                }
              },
              popTransitionSpec = {
                SharedZAxisEnterTransition togetherWith SharedZAxisExitTransition
              },
              predictivePopTransitionSpec = {
                SharedZAxisEnterTransition togetherWith SharedZAxisExitTransition
              },
              onBack = {
                if (backStack.size > 1) {
                  backStack.removeLastOrNull()
                }
              },
              entryProvider = { key ->
                navEntryResolver.create(key)
              },
            )
          }
        }

        LaunchedEffect(navigator) {
          navigator.navigationCommands.collect { command ->
            when (command) {
              is NavigationCommand.GoTo -> {
                when (val destination = command.destination) {
                  is Destination.Compose -> {
                    backStack += destination
                  }
                  is Destination.Activity -> {
                    startActivity(destination.intent)
                  }
                  Destination.BatteryOptimization -> {
                    toBatteryOptimizations()
                  }
                  is Destination.Website -> {
                    try {
                      startActivity(Intent(Intent.ACTION_VIEW, destination.url.toUri()))
                    } catch (exception: ActivityNotFoundException) {
                      Logger.w(exception)
                    }
                  }
                }
              }
              NavigationCommand.GoBack -> {
                if (backStack.size > 1) {
                  backStack.removeLastOrNull()
                }
              }
              is NavigationCommand.SetRoot -> {
                backStack.clear()
                backStack.add(command.root)
              }
            }
          }
        }

        ReviewFeature()
      }
    }
  }

  private fun toBatteryOptimizations() {
    val intent = Intent()
      .apply {
        @Suppress("BatteryLife")
        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = "package:$packageName".toUri()
      }
    try {
      startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      Logger.w(e, "Can't request ignoring battery optimizations")
    }
  }

  companion object {

    const val NI_GO_TO_BOOK = "niGotoBook"

    fun goToBookIntent(context: Context) = Intent(context, MainActivity::class.java).apply {
      putExtra(NI_GO_TO_BOOK, true)
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
  }
}
