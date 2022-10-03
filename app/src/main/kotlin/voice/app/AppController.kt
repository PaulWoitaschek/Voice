package voice.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.datastore.core.DataStore
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import voice.app.injection.appComponent
import voice.bookOverview.search.BookSearchScreen
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
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(navController = navController, startDestination = Destination.BookOverview.route) {
      composable(Destination.BookOverview.route) {
        BookOverviewScreen()
      }
      composable(Destination.Settings.route) {
        Settings()
      }
      composable(Destination.Migration.route) {
        Migration()
      }
      composable(Destination.FolderPicker.route) {
        FolderPicker(
          onCloseClick = {
            navController.popBackStack()
          },
        )
      }
      composable(Destination.BookSearch.route) {
        BookSearchScreen()
      }
      composable(Destination.SelectFolderType.route) { backStackEntry ->
        val destination = Destination.SelectFolderType.parse(backStackEntry.arguments!!)
        SelectFolderType(uri = destination.uri)
      }
    }

    LaunchedEffect(navigator) {
      navigator.navigationCommands.collect { command ->
        when (command) {
          is NavigationCommand.GoTo -> {
            when (val destination = command.destination) {
              is Destination.Compose -> {
                navController.navigate(destination.route)
              }
              else -> {
                // no-op
              }
            }
          }
          NavigationCommand.GoBack -> {
            navController.popBackStack()
          }
        }
      }
    }
  }
}
