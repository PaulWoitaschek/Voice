package voice.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.datastore.core.DataStore
import dev.olshevski.navigation.reimagined.AnimatedNavHost
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import dev.olshevski.navigation.reimagined.rememberNavController
import voice.app.injection.appComponent
import voice.bookOverview.views.BookOverviewScreen
import voice.common.BookId
import voice.common.compose.ComposeController
import voice.common.navigation.Destination
import voice.common.navigation.NavigationCommand
import voice.common.navigation.Navigator
import voice.common.pref.CurrentBook
import voice.folderPicker.folderPicker.FolderPicker
import voice.folderPicker.selectType.SelectFolderType
import voice.migration.views.Migration
import voice.settings.views.Settings
import javax.inject.Inject

class AppController : ComposeController() {

  init {
    appComponent.inject(this)
  }

  @field:[
  Inject
  CurrentBook
  ]
  lateinit var currentBookIdPref: DataStore<BookId?>

  @Inject
  lateinit var navigator: Navigator

  @Composable
  override fun Content() {
    val navController = rememberNavController<Destination.Compose>(
      startDestination = Destination.BookOverview,
    )
    NavBackHandler(navController)
    AnimatedNavHost(
      navController,
      transitionSpec = { action, _, _ ->
        navTransition(action)
      },
    ) { screen ->
      when (screen) {
        Destination.BookOverview -> {
          BookOverviewScreen()
        }
        Destination.FolderPicker -> {
          FolderPicker(
            onCloseClick = {
              navController.pop()
            },
          )
        }
        Destination.Migration -> {
          Migration()
        }
        is Destination.SelectFolderType -> {
          SelectFolderType(uri = screen.uri)
        }
        Destination.Settings -> {
          Settings()
        }
      }
    }

    LaunchedEffect(navigator) {
      navigator.navigationCommands.collect { command ->
        when (command) {
          is NavigationCommand.GoTo -> {
            when (val destination = command.destination) {
              is Destination.Compose -> {
                navController.navigate(destination)
              }
              else -> {
                // no-op
              }
            }
          }
          NavigationCommand.GoBack -> {
            navController.pop()
          }
        }
      }
    }
  }
}
