package voice.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kiwi.navigationcompose.typed.composable
import com.kiwi.navigationcompose.typed.createRoutePattern
import voice.app.injection.appComponent
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
import javax.inject.Inject
import com.kiwi.navigationcompose.typed.navigate as typedNavigate

class AppController : ComposeController() {

  init {
    appComponent.inject(this)
  }

  @Inject
  lateinit var startDestinationProvider: StartDestinationProvider

  @Inject
  lateinit var navigator: Navigator

  @Composable
  override fun Content() {
    val navController = rememberNavController()
    NavHost(
      navController = navController,
      startDestination = when (startDestinationProvider()) {
        StartDestinationProvider.StartDestination.OnboardingWelcome -> {
          createRoutePattern<Destination.OnboardingWelcome>()
        }
        StartDestinationProvider.StartDestination.BookOverview -> {
          createRoutePattern<Destination.BookOverview>()
        }
      },
    ) {
      composable<Destination.Migration> {
        Migration()
      }
      composable<Destination.SelectFolderType> {
        SelectFolderType(
          uri = uri,
          mode = mode,
        )
      }
      composable<Destination.CoverFromInternet> {
        SelectCoverFromInternet(
          bookId = bookId,
          onCloseClick = { navController.popBackStack() },
        )
      }
      composable<Destination.AddContent> {
        AddContent(mode)
      }
      composable<Destination.OnboardingCompletion> {
        OnboardingCompletion()
      }
      composable<Destination.OnboardingExplanation> {
        OnboardingExplanation(
          onNext = {
            navController.typedNavigate(
              Destination.AddContent(
                mode = Destination.AddContent.Mode.Onboarding,
              ),
            )
          },
          onBack = {
            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
              navController.popBackStack()
            }
          },
        )
      }
      composable<Destination.OnboardingWelcome> {
        OnboardingWelcome(
          onNext = { navController.typedNavigate(Destination.OnboardingExplanation) },
        )
      }
      composable<Destination.BookOverview> {
        BookOverviewScreen()
      }
      composable<Destination.Settings> {
        Settings()
      }
      composable<Destination.FolderPicker> {
        FolderOverview(
          onCloseClick = {
            navController.popBackStack()
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
                navController.typedNavigate(destination)
              }
              else -> {
                // no-op
              }
            }
          }
          NavigationCommand.GoBack -> {
            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
              navController.popBackStack()
            }
          }
          is NavigationCommand.Execute -> {
            command.action(navController)
          }
        }
      }
    }

    ReviewFeature()
  }
}
