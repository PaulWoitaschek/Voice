package voice.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.zacsweers.metro.Inject
import voice.app.injection.appGraph
import voice.bookOverview.views.BookOverviewScreen
import voice.common.compose.ComposeController
import voice.common.navigation.Destination
import voice.common.navigation.NavigationCommand
import voice.common.navigation.Navigator
import voice.cover.SelectCoverFromInternet
import voice.folderPicker.addcontent.AddContent
import voice.folderPicker.folderPicker.FolderOverview
import voice.folderPicker.selectType.SelectFolderType
import voice.migration.views.Migration
import voice.onboarding.OnboardingExplanation
import voice.onboarding.OnboardingWelcome
import voice.onboarding.completion.OnboardingCompletion
import voice.review.ReviewFeature
import voice.settings.views.Settings

class AppController : ComposeController() {

  init {
    appGraph.inject(this)
  }

  @Inject
  lateinit var startDestinationProvider: StartDestinationProvider

  @Inject
  lateinit var navigator: Navigator

  @Composable
  override fun Content() {
    val backStack = rememberNavBackStack(
      when (startDestinationProvider()) {
        StartDestinationProvider.StartDestination.OnboardingWelcome -> Destination.OnboardingWelcome
        StartDestinationProvider.StartDestination.BookOverview -> Destination.BookOverview
      },
    )

    NavDisplay(
      backStack = backStack,
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
              else -> {
                // no-op
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
