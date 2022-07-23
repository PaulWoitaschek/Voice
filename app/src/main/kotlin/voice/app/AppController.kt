package voice.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import voice.app.injection.appComponent
import voice.bookOverview.views.BookOverviewScreen
import voice.common.BookId
import voice.common.compose.ComposeController
import voice.common.navigation.Navigator
import voice.common.navigation.Screen
import voice.common.pref.CurrentBook
import voice.folderPicker.FolderPicker
import voice.logging.core.Logger
import voice.migration.views.Migration
import voice.settings.views.Settings
import javax.inject.Inject

class AppController : ComposeController() {

  init {
    appComponent.inject(this)
  }

  @field:[Inject CurrentBook]
  lateinit var currentBookIdPref: DataStore<BookId?>

  @Inject
  lateinit var navigator: Navigator

  @Composable
  override fun Content() {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(navController = navController, startDestination = Screen.BookOverview.route) {
      composable(Screen.BookOverview.route) {
        BookOverviewScreen()
      }
      composable(Screen.Settings.route) {
        Settings(
          onCloseScreenClicked = {
            navController.popBackStack()
          },
          toSupport = {
            visitUri("https://github.com/PaulWoitaschek/Voice".toUri())
          },
          toTranslations = {
            visitUri("https://www.transifex.com/projects/p/voice".toUri())
          }
        )
      }
      composable(Screen.Migration.route) {
        Migration(
          onCloseClicked = {
            navController.popBackStack()
          }
        )
      }
      composable(Screen.FolderPicker.route) {
        FolderPicker(
          onCloseClick = {
            navController.popBackStack()
          }
        )
      }
    }

    LaunchedEffect(navigator) {
      navigator.composeCommands.collect { screen ->
        navController.navigate(screen.route)
      }
    }
  }

  private fun visitUri(uri: Uri) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (exception: ActivityNotFoundException) {
      Logger.w(exception)
    }
  }
}
