package voice.app.features

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import dev.zacsweers.metro.Inject
import voice.app.StartDestinationProvider
import voice.app.features.bookOverview.EditCoverDialog
import voice.app.injection.appGraph
import voice.bookOverview.views.BookOverviewScreen
import voice.bookmark.BookmarkScreen
import voice.common.compose.VoiceTheme
import voice.common.navigation.Destination
import voice.common.navigation.NavigationCommand
import voice.common.navigation.Navigator
import voice.cover.SelectCoverFromInternet
import voice.folderPicker.addcontent.AddContent
import voice.folderPicker.folderPicker.FolderOverview
import voice.folderPicker.selectType.SelectFolderType
import voice.logging.core.Logger
import voice.migration.views.Migration
import voice.onboarding.OnboardingExplanation
import voice.onboarding.OnboardingWelcome
import voice.onboarding.completion.OnboardingCompletion
import voice.playbackScreen.BookPlayScreen
import voice.review.ReviewFeature
import voice.settings.views.Settings

class MainActivity : AppCompatActivity() {

  @Inject
  lateinit var navigator: Navigator

  @Inject
  lateinit var startDestinationProvider: StartDestinationProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    appGraph.inject(this)
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    setContent {
      val backStack = rememberNavBackStack(*startDestinationProvider(intent).toTypedArray())
      VoiceTheme {

        val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }


        NavDisplay(
          backStack = backStack,
          sceneStrategy = dialogStrategy,
          onBack = {
            backStack.removeLastOrNull()
          },
          entryProvider = { key ->
            check(key is Destination.Compose)
            when (key) {
              is Destination.AddContent -> {
                NavEntry(key) {
                  AddContent(mode = key.mode)
                }
              }
              Destination.BookOverview -> {
                NavEntry(key) {
                  BookOverviewScreen()
                }
              }
              is Destination.CoverFromInternet -> {
                NavEntry(key) {
                  SelectCoverFromInternet(
                    bookId = key.bookId,
                    onCloseClick = { backStack.removeLastOrNull() },
                  )
                }
              }
              Destination.FolderPicker -> {
                NavEntry(key) {
                  FolderOverview(
                    onCloseClick = {
                      backStack.removeLastOrNull()
                    },
                  )
                }
              }
              Destination.Migration -> {
                NavEntry(key) {
                  Migration()
                }
              }
              Destination.OnboardingCompletion -> {
                NavEntry(key) {
                  OnboardingCompletion()
                }
              }
              Destination.OnboardingExplanation -> {
                NavEntry(key) {
                  OnboardingExplanation(
                    onNext = {
                      backStack.add(Destination.AddContent(mode = Destination.AddContent.Mode.Onboarding))
                    },
                    onBack = {
                      if (backStack.isNotEmpty()) {
                        backStack.removeLastOrNull()
                      }
                    },
                  )
                }
              }
              Destination.OnboardingWelcome -> {
                NavEntry(key) {
                  OnboardingWelcome(
                    onNext = { backStack.add(Destination.OnboardingExplanation) },
                  )
                }
              }
              is Destination.SelectFolderType -> {
                NavEntry(key) {
                  SelectFolderType(
                    uri = key.uri,
                    mode = key.mode,
                  )
                }
              }
              Destination.Settings -> {
                NavEntry(key) {
                  Settings()
                }
              }
              is Destination.EditCover -> {
                NavEntry(key, metadata = DialogSceneStrategy.dialog()) {
                  EditCoverDialog(
                    bookId = key.bookId,
                    coverUri = key.cover,
                    onDismiss = { backStack.removeLastOrNull() },
                  )
                }
              }
              is Destination.Bookmarks -> {
                NavEntry(key) {
                  BookmarkScreen(key.bookId)
                }
              }
              is Destination.Playback -> {
                NavEntry(key) {
                  BookPlayScreen(key.bookId)
                }
              }
            }
          },
        )

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
                backStack.removeLastOrNull()
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
        action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
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
